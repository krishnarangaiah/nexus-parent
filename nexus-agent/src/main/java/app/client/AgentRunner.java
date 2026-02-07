package app.client;

import app.core.dto.CommandRequest;
import app.core.dto.CommandResponse;
import app.core.dto.HeartbeatMessage;
import app.core.dto.RegistrationRequest;
import app.core.protocol.MessageType;
import app.core.protocol.NexusProtocol;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.*;

/**
 * Agent Runner
 *
 * Main class that manages the WebSocket connection to the server.
 * Handles:
 * - Connection and reconnection
 * - Registration with server
 * - Heartbeat sending
 * - Command execution
 */
@Component
public class AgentRunner {

    private static final Logger LOG = LogManager.getLogger(AgentRunner.class);

    @Value("${agent.uuid}")
    private String agentUuid;

    @Value("${agent.websocket.url}")
    private String serverUrl;

    @Value("${agent.heartbeat.interval-ms:30000}")
    private long heartbeatIntervalMs;

    @Value("${agent.reconnect.delay-ms:5000}")
    private long reconnectDelayMs;

    @Value("${agent.version:1.0.0}")
    private String agentVersion;

    private volatile Session session;
    private volatile boolean running = true;
    private volatile boolean registered = false;

    private final CommandDispatcher commandDispatcher;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatTask;

    public AgentRunner(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = commandDispatcher;
    }

    // ==================== MAIN RUN LOOP ====================

    public void runAgent() {
        LOG.info("Starting Nexus Agent: uuid={}", agentUuid);

        while (running) {
            try {
                connect();

                // Keep running while connected
                while (session != null && session.isOpen() && running) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                LOG.error("Connection error: {}", e.getMessage());
            }

            registered = false;
            stopHeartbeat();

            if (running) {
                LOG.info("Reconnecting in {} ms...", reconnectDelayMs);
                sleep(reconnectDelayMs);
            }
        }

        cleanup();
        LOG.info("Agent stopped.");
    }

    // ==================== CONNECTION ====================

    private void connect() throws Exception {
        LOG.info("Connecting to server: {}", serverUrl);

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        CountDownLatch connectLatch = new CountDownLatch(1);

        session = container.connectToServer(new AgentEndpoint(connectLatch), URI.create(serverUrl));

        if (connectLatch.await(10, TimeUnit.SECONDS)) {
            LOG.info("Connected successfully!");
            sendRegistration();
        } else {
            LOG.error("Connection timeout!");
            closeSession();
        }
    }

    // ==================== REGISTRATION ====================

    private void sendRegistration() {
        try {
            RegistrationRequest request = RegistrationRequest.create(agentUuid)
                    .hostname(InetAddress.getLocalHost().getHostName())
                    .ipAddress(InetAddress.getLocalHost().getHostAddress())
                    .osName(System.getProperty("os.name"))
                    .osVersion(System.getProperty("os.version"))
                    .workingDirectory(System.getProperty("user.dir"))
                    .agentVersion(agentVersion)
                    .javaVersion(System.getProperty("java.version"))
                    .startedAt(System.currentTimeMillis());

            String message = NexusProtocol.createRegistrationMessage(request);
            sendMessage(message);
            LOG.info("Registration sent: uuid={}", agentUuid);

        } catch (Exception e) {
            LOG.error("Failed to send registration: {}", e.getMessage(), e);
        }
    }

    // ==================== HEARTBEAT ====================

    private void startHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (registered && session != null && session.isOpen()) {
                try {
                    HeartbeatMessage heartbeat = HeartbeatMessage.create(agentUuid)
                            .cpuUsage(getSystemCpuUsage())
                            .memoryUsage(getMemoryUsage())
                            .activeThreads(Thread.activeCount());

                    sendMessage(NexusProtocol.createHeartbeatMessage(heartbeat));
                    LOG.debug("Heartbeat sent");

                } catch (Exception e) {
                    LOG.error("Failed to send heartbeat: {}", e.getMessage());
                }
            }
        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);

        LOG.info("Heartbeat started: interval={}ms", heartbeatIntervalMs);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    // ==================== MESSAGE HANDLING ====================

    private void handleMessage(String message) {
        try {
            JsonObject json = NexusProtocol.parse(message);
            String type = NexusProtocol.getType(json);

            switch (type) {
                case MessageType.REGISTRATION_ACK:
                    handleRegistrationAck(json);
                    break;
                case MessageType.REGISTRATION_REJECTED:
                    handleRegistrationRejected(json);
                    break;
                case MessageType.COMMAND_REQUEST:
                    handleCommandRequest(json);
                    break;
                case MessageType.ERROR:
                    handleError(json);
                    break;
                default:
                    LOG.warn("Unknown message type: {}", type);
            }

        } catch (Exception e) {
            LOG.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    private void handleRegistrationAck(JsonObject json) {
        JsonObject payload = NexusProtocol.getPayload(json);
        String welcomeMessage = payload.has("message") ? payload.get("message").getAsString() : "Registered";

        LOG.info("Registration accepted: {}", welcomeMessage);
        registered = true;
        startHeartbeat();
    }

    private void handleRegistrationRejected(JsonObject json) {
        JsonObject payload = NexusProtocol.getPayload(json);
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "Unknown reason";

        LOG.error("Registration rejected: {}", reason);
        registered = false;
        running = false; // Stop the agent - UUID not valid
    }

    private void handleCommandRequest(JsonObject json) {
        CommandRequest request = NexusProtocol.parseCommandRequest(json);
        LOG.info("Command received: requestId={}, command={}", request.getRequestId(), request.getCommandName());

        // Execute command asynchronously
        CompletableFuture.runAsync(() -> executeCommand(request));
    }

    private void executeCommand(CommandRequest request) {
        long startTime = System.currentTimeMillis();
        CommandResponse response;

        try {
            // Execute via CommandDispatcher (supports Groovy scripts)
            String output = commandDispatcher.executeScript(
                    request.getCommandName(),
                    request.getScriptContent(),
                    request.getArguments()
            );

            long duration = System.currentTimeMillis() - startTime;
            response = CommandResponse.success(request.getRequestId(), agentUuid, output)
                    .durationMs(duration);

            LOG.info("Command executed successfully: requestId={}, duration={}ms",
                request.getRequestId(), duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response = CommandResponse.failed(request.getRequestId(), agentUuid, e.getMessage())
                    .durationMs(duration);

            LOG.error("Command execution failed: requestId={}, error={}",
                request.getRequestId(), e.getMessage());
        }

        // Send response
        sendMessage(NexusProtocol.createCommandResponse(response));
    }

    private void handleError(JsonObject json) {
        JsonObject payload = NexusProtocol.getPayload(json);
        String errorMessage = payload.has("errorMessage") ? payload.get("errorMessage").getAsString() : "Unknown error";
        LOG.error("Error from server: {}", errorMessage);
    }

    // ==================== UTILITIES ====================

    private void sendMessage(String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            LOG.error("Failed to send message: {}", e.getMessage());
        }
    }

    private void closeSession() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            LOG.debug("Error closing session: {}", e.getMessage());
        }
        session = null;
    }

    private void cleanup() {
        stopHeartbeat();
        scheduler.shutdown();
        closeSession();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private Double getSystemCpuUsage() {
        // Simplified - return approximate CPU usage
        return Runtime.getRuntime().availableProcessors() > 0 ?
            Math.random() * 50 + 10 : null; // Placeholder
    }

    private Double getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        return (used * 100.0) / max;
    }

    public void stop() {
        running = false;
    }

    // ==================== WEBSOCKET ENDPOINT ====================

    private class AgentEndpoint extends Endpoint {

        private final CountDownLatch connectLatch;

        AgentEndpoint(CountDownLatch connectLatch) {
            this.connectLatch = connectLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            LOG.info("WebSocket opened");
            connectLatch.countDown();

            session.addMessageHandler(String.class, message -> {
                LOG.debug("Received: {}", message.length() > 200 ? message.substring(0, 200) + "..." : message);
                handleMessage(message);
            });
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            LOG.error("WebSocket error: {}", throwable.getMessage());
            connectLatch.countDown();
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            LOG.info("WebSocket closed: {}", closeReason.getReasonPhrase());
            registered = false;
        }
    }
}
