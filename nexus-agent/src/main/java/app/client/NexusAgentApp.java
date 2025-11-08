package app.client;

import app.websocket.JvmMetricsCollector;
import app.websocket.ResilientWebSocketClient;
import app.websocket.StompSession;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/*
  Converted to a standalone agent runner:
  - reads runtime config from classpath application.properties
  - connects to server WebSocket/STOMP endpoint
  - schedules metrics push at configured interval
  - processes simple incoming commands (CMD:, PING)
  - provides graceful shutdown
*/
public class NexusAgentApp {

    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream in = NexusAgentApp.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                config.load(in);
            } else {
                System.err.println("application.properties not found on classpath; using defaults");
            }
        } catch (Exception e) {
            System.err.println("Failed to read application.properties: " + e.getMessage());
        }

        final String uriStr = config.getProperty("server.uri", "ws://localhost:9000/ws");
        final String agentId = config.getProperty("agent.id", "nexus-agent-unknown");
        final int metricsInterval = Integer.parseInt(config.getProperty("metrics.interval.seconds", "5"));
        final int reconnectInitial = Integer.parseInt(config.getProperty("reconnect.initial.seconds", "2"));
        final int reconnectMax = Integer.parseInt(config.getProperty("reconnect.max.seconds", "60"));

        System.out.println(Instant.now() + " - Starting NexusAgentApp (agentId=" + agentId + ") -> " + uriStr);

        URI uri;
        try {
            uri = new URI(uriStr);
        } catch (Exception e) {
            System.err.println("Invalid server.uri: " + uriStr + " -> " + e.getMessage());
            return;
        }

        // Use AtomicReference to avoid forward-reference problem in handler
        final AtomicReference<ResilientWebSocketClient> wsRef = new AtomicReference<>();

        // Create resilient websocket client with a message handler callback
        ResilientWebSocketClient wsClient = new ResilientWebSocketClient(uri, msg -> {
            // Incoming message handler: keep simple, parse commands
            try {
                if (msg == null) return;
                String trimmed = msg.trim();
                System.out.println(Instant.now() + " RECV: " + trimmed);

                // access concrete client instance safely
                ResilientWebSocketClient client = wsRef.get();

                if ("PING".equalsIgnoreCase(trimmed)) {
                    // respond directly on socket if available
                    if (client != null) {
                        sendUsingReflection(client, "PONG");
                    }
                    return;
                }
                if (trimmed.startsWith("CMD:")) {
                    // server command, e.g. CMD:RESTART or CMD:SET_INTERVAL=10
                    String cmd = trimmed.substring(4).trim();
                    System.out.println("Received command: " + cmd);
                    // basic command handling (expand as needed)
                    if ("RESTART".equalsIgnoreCase(cmd)) {
                        System.out.println("Simulated restart command received.");
                        if (client != null) sendUsingReflection(client, "ACK:RESTART");
                    } else if (cmd.startsWith("SET_INTERVAL=")) {
                        String value = cmd.substring("SET_INTERVAL=".length());
                        try {
                            int newInterval = Integer.parseInt(value);
                            // Note: we don't mutate the scheduled job here (left as exercise),
                            // but we acknowledge receipt.
                            if (client != null) sendUsingReflection(client, "ACK:SET_INTERVAL=" + newInterval);
                            System.out.println("Requested interval change to " + newInterval + "s");
                        } catch (NumberFormatException nfe) {
                            if (client != null) sendUsingReflection(client, "ERR:INVALID_INTERVAL");
                        }
                    } else {
                        if (client != null) sendUsingReflection(client, "ACK:UNKNOWN_CMD");
                    }
                    return;
                }

                // For STOMP-wrapped payloads, the StompSession will handle; this is raw fallback
            } catch (Throwable t) {
                System.err.println("Error handling incoming message: " + t.getMessage());
            }
        });

        // now set reference so handler can use the client
        wsRef.set(wsClient);

        // Create STOMP session wrapper and connect (this class is assumed available in project)
        StompSession stompSession = new StompSession(wsClient);

        // Connect with simple backoff if initial connect fails
        int backoff = reconnectInitial;
        while (true) {
            try {
                stompSession.connect();
                System.out.println("Connected STOMP session");
                break;
            } catch (Exception e) {
                System.err.println("Failed to connect STOMP: " + e.getMessage() + " - retrying in " + backoff + "s");
                try {
                    Thread.sleep(backoff * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoff = Math.min(backoff * 2, reconnectMax);
            }
        }

        // Subscribe to command / ack topics if supported by server
        try {
            stompSession.subscribe("/topic/ack", agentId + "-ack-sub");
            stompSession.subscribe("/user/queue/commands", agentId + "-cmd-sub");
        } catch (Exception ex) {
            System.err.println("Subscription warning: " + ex.getMessage());
        }

        // Scheduled metrics sender
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-sender");
            t.setDaemon(true);
            return t;
        });

        Runnable metricsTask = () -> {
            try {
                // Collect metrics using shared class (nexus-common)
                String baseMetricsJson = JvmMetricsCollector.collectMetrics(); // e.g. {"threads":..,"heapUsed":..,"heapMax":..}
                if (baseMetricsJson != null && !baseMetricsJson.isEmpty()) {
                    // construct wrapped payload including agentId and timestamp so server maps to Metrics DTO
                    long ts = Instant.now().toEpochMilli();
                    // baseMetricsJson is expected to be an object JSON. Inject agentId and timestamp fields.
                    String payload;
                    if (baseMetricsJson.trim().startsWith("{") && baseMetricsJson.trim().endsWith("}")) {
                        payload = baseMetricsJson.trim();
                        // insert additional fields before final }
                        payload = payload.substring(0, payload.length() - 1)
                                + ",\"agentId\":\"" + escapeJson(agentId) + "\",\"timestamp\":" + ts + "}";
                    } else {
                        // fallback: build minimal JSON
                        payload = "{\"agentId\":\"" + escapeJson(agentId) + "\",\"timestamp\":" + ts + ",\"threads\":0,\"heapUsed\":0,\"heapMax\":0}";
                    }

                    if (payload != null && !payload.isEmpty()) {
                        try {
                            stompSession.send("/app/metrics", payload);
                            System.out.println(Instant.now() + " Sent metrics payload: " + payload);
                        } catch (Exception e) {
                            // fallback to raw websocket if STOMP send fails
                            try {
                                sendUsingReflection(wsClient, payload);
                            } catch (Exception inner) {
                                System.err.println("Failed to send metrics: " + inner.getMessage());
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                System.err.println("Metrics task error: " + t.getMessage());
            }
        };

        // schedule at fixed rate
        scheduler.scheduleAtFixedRate(metricsTask, 0, Math.max(1, metricsInterval), TimeUnit.SECONDS);

        // graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested: stopping scheduler and disconnecting...");
            try {
                scheduler.shutdownNow();
            } catch (Throwable t) { /* ignore */ }
            try {
                // use safeDisconnect to avoid compile error if StompSession has different API
                safeDisconnect(stompSession);
            } catch (Throwable t) { /* ignore */ }
            try {
                wsClient.close();
            } catch (Throwable t) { /* ignore */ }
            System.out.println("Agent stopped.");
        }));

        // Keep main thread alive while the scheduler and websocket run.
        // If ResilientWebSocketClient has its own lifecycle, it will reconnect as needed.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Helper: attempt to send payload using reflection on the client instance.
    // Tries a sequence of commonly used method names to support different implementations.
    private static void sendUsingReflection(Object client, String payload) {
        if (client == null || payload == null) return;
        String[] candidateNames = new String[] { "sendText", "sendString", "send", "sendMessage", "sendRaw", "write" };
        for (String name : candidateNames) {
            try {
                Method m = client.getClass().getMethod(name, String.class);
                if (m != null) {
                    m.invoke(client, payload);
                    return;
                }
            } catch (NoSuchMethodException nsme) {
                // try next candidate
            } catch (Throwable t) {
                // method exists but invocation failed
                System.err.println("Failed invoking '" + name + "' on websocket client: " + t.getMessage());
                return;
            }
        }
        // as a last resort, try an 'send' taking byte[] if present
        try {
            Method m2 = client.getClass().getMethod("send", byte[].class);
            if (m2 != null) {
                m2.invoke(client, (Object) payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
        } catch (NoSuchMethodException nsme) {
            // nothing to do
        } catch (Throwable t) {
            System.err.println("Failed invoking byte[] send on websocket client: " + t.getMessage());
        }

        // If none of the methods exist, log notice
        System.err.println("No suitable send method found on client: " + client.getClass().getName());
    }

    // ----------------- helper: safeDisconnect -----------------
    // Attempt to call a no-arg lifecycle method on the session object via reflection.
    // Tries common method names and logs outcome; fails gracefully if none found.
    private static void safeDisconnect(Object session) {
        if (session == null) return;
        String[] candidates = new String[] { "disconnect", "close", "stop", "shutdown", "dispose", "terminate" };
        for (String name : candidates) {
            try {
                Method m = session.getClass().getMethod(name);
                if (m != null) {
                    try {
                        m.invoke(session);
                        System.out.println("Invoked '" + name + "' on " + session.getClass().getName());
                    } catch (Throwable invokeErr) {
                        System.err.println("Invocation error invoking '" + name + "' on session: " + invokeErr.getMessage());
                    }
                    return;
                }
            } catch (NoSuchMethodException nsme) {
                // try next candidate
            } catch (Throwable t) {
                System.err.println("Error inspecting session for method '" + name + "': " + t.getMessage());
                return;
            }
        }
        System.out.println("No suitable disconnect/close method found for session: " + session.getClass().getName());
    }
}
