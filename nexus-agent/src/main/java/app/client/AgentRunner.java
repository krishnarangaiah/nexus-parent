package app.client;

import app.websocket.ontology.CommunicationProtocol;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class AgentRunner {

    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${agent.websocket.url}")
    private String serverUrl;

    @Value("${agent.websocket.send-interval-ms:50000}")
    private long sendIntervalMs;

    private volatile Session session;
    private volatile boolean running = true;

    private final CommandDispatcher commandDispatcher;

    public AgentRunner(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = commandDispatcher;
    }

    public void runAgent() {
        while (running) {
            CountDownLatch connectionLatch = new CountDownLatch(1);
            try {
                System.out.println("🔗 Attempting to connect to: " + serverUrl);
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                session = container.connectToServer(new SimpleClientEndpoint(connectionLatch), URI.create(serverUrl));
                if (connectionLatch.await(10, TimeUnit.SECONDS)) {
                    System.out.println("Connection established successfully!");
                    while (session != null && session.isOpen() && running) {
                        TimeUnit.MILLISECONDS.sleep(sendIntervalMs);
                    }
                } else {
                    LOGGER.error("Connection timeout!");
                }
            } catch (Exception e) {
                LOGGER.error("Connection failed: {}", e.getMessage());
            }
            try {
                if (running) {
                    System.out.println("Waiting 5 seconds before reconnecting...");
                    TimeUnit.MILLISECONDS.sleep(sendIntervalMs);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        LOGGER.info("🏁 Agent stopped.");
    }

    private class SimpleClientEndpoint extends Endpoint {
        private final CountDownLatch connectionLatch;

        public SimpleClientEndpoint(CountDownLatch connectionLatch) {
            this.connectionLatch = connectionLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            LOGGER.info("Connected successfully!");
            connectionLatch.countDown();
            session.addMessageHandler(String.class, message -> {
                LOGGER.info("Received: {}", message);
                handleCommand(message, session);
            });
        }

        @Override
        public void onError(Session session, Throwable thr) {
            LOGGER.error("WebSocket error: {}", thr.getMessage());
            connectionLatch.countDown();
            closeSession();
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            LOGGER.info("Connection closed: {}", closeReason.getReasonPhrase());
            closeSession();
        }

        private void closeSession() {
            try {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    private void handleCommand(String message, Session session) {
        String command = message.trim().split(" ")[0].toUpperCase();
        String args = message.length() > command.length() ? message.substring(command.length()).trim() : "";
        String response = commandDispatcher.dispatch(command, args);
        try {
            session.getBasicRemote().sendText(response);
            LOGGER.info("Sent: {}", response);
        } catch (Exception e) {
            LOGGER.error("Failed to send response: {}", e.getMessage());
        }
    }

    // Example usage of CommunicationProtocol
    private void sendMessage(String type, JsonObject payload) {
        String message = CommunicationProtocol.createMessage(type, payload);
        // Logic to send the message via WebSocket
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = CommunicationProtocol.parseMessage(message);
            String type = CommunicationProtocol.getMessageType(json);
            JsonObject payload = CommunicationProtocol.getPayload(json);

            switch (type) {
                case "command":
                    // Handle command
                    break;
                case "response":
                    // Handle response
                    break;
                default:
                    System.err.println("Unknown message type: " + type);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid message format: " + e.getMessage());
        }
    }
}
