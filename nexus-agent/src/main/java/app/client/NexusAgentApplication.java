package app.client;

import app.websocket.JvmMetricsCollector;
import app.websocket.ResilientWebSocketClient;
import app.websocket.StompSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/*
  Converted to a Spring Boot non-web application:
  - reads runtime config from application.properties via Spring Environment
  - connects to server WebSocket/STOMP endpoint
  - schedules metrics push at configured interval
  - processes simple incoming commands (CMD:, PING)
  - provides graceful shutdown via @PreDestroy
*/
@SpringBootApplication
public class NexusAgentApplication {

    private static final Logger LOG = LoggerFactory.getLogger(NexusAgentApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NexusAgentApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Component
    public static class AgentRunner implements ApplicationRunner {
        private final Environment env;
        private final AtomicReference<ResilientWebSocketClient> wsRef = new AtomicReference<>();
        private volatile ResilientWebSocketClient wsClient;
        private volatile StompSession stompSession;
        private volatile ScheduledExecutorService scheduler;

        public AgentRunner(Environment env) {
            this.env = env;
        }

        @Override
        public void run(ApplicationArguments args) {
            final String uriStr = env.getProperty("server.uri", "ws://localhost:9000/ws");
            final String agentId = env.getProperty("agent.id", "nexus-agent-unknown");
            final int metricsInterval = Integer.parseInt(env.getProperty("metrics.interval.seconds", "5"));
            final int reconnectInitial = Integer.parseInt(env.getProperty("reconnect.initial.seconds", "2"));
            final int reconnectMax = Integer.parseInt(env.getProperty("reconnect.max.seconds", "60"));

            LOG.info("{} - Starting NexusAgentApplication (agentId={}) -> {}", Instant.now(), agentId, uriStr);

            URI uri;
            try {
                uri = new URI(uriStr);
            } catch (Exception e) {
                LOG.error("Invalid server.uri: {} -> {}", uriStr, e.getMessage());
                return;
            }

            // create client with message handler
            wsClient = new ResilientWebSocketClient(uri, msg -> {
                try {
                    if (msg == null) return;
                    String trimmed = msg.trim();
                    LOG.info("{} RECV: {}", Instant.now(), trimmed);

                    ResilientWebSocketClient client = wsRef.get();

                    if ("PING".equalsIgnoreCase(trimmed)) {
                        if (client != null) {
                            sendUsingReflection(client, "PONG");
                        }
                        return;
                    }
                    if (trimmed.startsWith("CMD:")) {
                        String cmd = trimmed.substring(4).trim();
                        LOG.info("Received command: {}", cmd);
                        if ("RESTART".equalsIgnoreCase(cmd)) {
                            LOG.info("Simulated restart command received.");
                            if (client != null) sendUsingReflection(client, "ACK:RESTART");
                        } else if (cmd.startsWith("SET_INTERVAL=")) {
                            String value = cmd.substring("SET_INTERVAL=".length());
                            try {
                                int newInterval = Integer.parseInt(value);
                                if (client != null) sendUsingReflection(client, "ACK:SET_INTERVAL=" + newInterval);
                                LOG.info("Requested interval change to {}s", newInterval);
                            } catch (NumberFormatException nfe) {
                                if (client != null) sendUsingReflection(client, "ERR:INVALID_INTERVAL");
                            }
                        } else {
                            if (client != null) sendUsingReflection(client, "ACK:UNKNOWN_CMD");
                        }
                        return;
                    }
                } catch (Throwable t) {
                    LOG.error("Error handling incoming message: {}", t.getMessage());
                }
            });

            wsRef.set(wsClient);

            // Build agent metadata headers so the server can evaluate the incoming CONNECT
            Map<String,String> agentHeaders = new LinkedHashMap<>();
            try {
                String host = InetAddress.getLocalHost().getHostName();
                agentHeaders.put("agent-host", host);
            } catch (Exception ignored) { /* best-effort */ }
            String pid = ManagementFactory.getRuntimeMXBean().getName(); // typically "pid@host"
            agentHeaders.put("agent-pid", pid == null ? "" : pid);
            agentHeaders.put("agent-id", agentId);
            agentHeaders.put("agent-version", env.getProperty("agent.version", "0.0.0"));
            agentHeaders.put("java-version", System.getProperty("java.version", ""));
            agentHeaders.put("os-name", System.getProperty("os.name", ""));

            stompSession = new StompSession(wsClient, agentHeaders);

            int backoff = reconnectInitial;
            while (true) {
                try {
                    stompSession.connect();
                    LOG.info("Connected STOMP session");
                    break;
                } catch (Exception e) {
                    LOG.error("Failed to connect STOMP: {} - retrying in {}s", e.getMessage(), backoff);
                    try {
                        Thread.sleep(backoff * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoff = Math.min(backoff * 2, reconnectMax);
                }
            }

            try {
                stompSession.subscribe("/topic/ack", agentId + "-ack-sub");
                stompSession.subscribe("/user/queue/commands", agentId + "-cmd-sub");
            } catch (Exception ex) {
                LOG.warn("Subscription warning: {}", ex.getMessage());
            }

            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "metrics-sender");
                t.setDaemon(true);
                return t;
            });

            final ResilientWebSocketClient wsForTask = wsClient;
            final StompSession stompForTask = stompSession;

            Runnable metricsTask = () -> {
                try {
                    String baseMetricsJson = JvmMetricsCollector.collectMetrics();
                    if (baseMetricsJson != null && !baseMetricsJson.isEmpty()) {
                        long ts = Instant.now().toEpochMilli();
                        String payload;
                        if (baseMetricsJson.trim().startsWith("{") && baseMetricsJson.trim().endsWith("}")) {
                            payload = baseMetricsJson.trim();
                            payload = payload.substring(0, payload.length() - 1)
                                    + ",\"agentId\":\"" + escapeJson(agentId) + "\",\"timestamp\":" + ts + "}";
                        } else {
                            payload = "{\"agentId\":\"" + escapeJson(agentId) + "\",\"timestamp\":" + ts + ",\"threads\":0,\"heapUsed\":0,\"heapMax\":0}";
                        }

                        if (payload != null && !payload.isEmpty()) {
                            try {
                                stompForTask.send("/app/metrics", payload);
                                LOG.info("{} Sent metrics payload: {}", Instant.now(), payload);
                            } catch (Exception e) {
                                try {
                                    sendUsingReflection(wsForTask, payload);
                                } catch (Exception inner) {
                                    LOG.error("Failed to send metrics: {}", inner.getMessage());
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("Metrics task error: {}", t.getMessage());
                }
            };

            scheduler.scheduleAtFixedRate(metricsTask, 0, Math.max(1, metricsInterval), TimeUnit.SECONDS);
        }

        @PreDestroy
        public void shutdown() {
            LOG.info("Shutdown requested: stopping scheduler and disconnecting...");
            try {
                if (scheduler != null) scheduler.shutdownNow();
            } catch (Throwable t) { /* ignore */ }
            try {
                safeDisconnect(stompSession);
            } catch (Throwable t) { /* ignore */ }
            try {
                if (wsClient != null) wsClient.close();
            } catch (Throwable t) { /* ignore */ }
            LOG.info("Agent stopped.");
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
                System.err.println("Failed invoking '" + name + "' on websocket client: " + t.getMessage());
                return;
            }
        }
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
                        LOG.info("Invoked '{}' on {}", name, session.getClass().getName());
                    } catch (Throwable invokeErr) {
                        LOG.error("Invocation error invoking '{}' on session: {}", name, invokeErr.getMessage());
                    }
                    return;
                }
            } catch (NoSuchMethodException nsme) {
                // try next candidate
            } catch (Throwable t) {
                LOG.error("Error inspecting session for method '{}': {}", name, t.getMessage());
                return;
            }
        }
        LOG.info("No suitable disconnect/close method found for session: {}", session.getClass().getName());
    }
}
