package app.websocket;

import app.websocket.dto.Metrics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component("plainWebSocketHandler")
public class PlainWebSocketHandler extends TextWebSocketHandler {

    private static final org.apache.logging.log4j.Logger LOGGER =
        org.apache.logging.log4j.LogManager.getLogger(PlainWebSocketHandler.class);

    private static final Gson GSON = new Gson();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        LOGGER.info("Plain WebSocket connection established: {}", sessionId);

        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"welcome\",\"message\":\"Connected to Nexus WebSocket\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        LOGGER.info("Received message from {}: {}", session.getId(), payload);

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "unknown";

            switch (type) {
                case "registration":
                    handleRegistration(session, json);
                    break;
                case "metrics":
                    handleMetrics(session, json);
                    break;
                case "test":
                    handleTest(session, json);
                    break;
                default:
                    LOGGER.warn("Unknown message type: {}", type);
                    session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Unknown message type: " + type + "\"}"));
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message: {}", e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Invalid JSON format\"}"));
        }
    }

    private void handleRegistration(WebSocketSession session, JsonObject json) throws Exception {
        String agentId = json.has("agentId") ? json.get("agentId").getAsString() : "unknown";
        LOGGER.info("Agent registration: {} from session {}", agentId, session.getId());

        // Store agent info in session attributes
        session.getAttributes().put("agentId", agentId);
        session.getAttributes().put("agentHost", json.has("agentHost") ? json.get("agentHost").getAsString() : "");
        session.getAttributes().put("agentPid", json.has("agentPid") ? json.get("agentPid").getAsString() : "");

        // Send confirmation
        session.sendMessage(new TextMessage("{\"type\":\"registration_ack\",\"message\":\"Agent registered successfully\"}"));
    }

    private void handleMetrics(WebSocketSession session, JsonObject json) throws Exception {
        // Convert JSON to Metrics object
        Metrics metrics = GSON.fromJson(json, Metrics.class);

        // Forward to STOMP message broker for processing by @MessageMapping("/metrics")
        messagingTemplate.convertAndSend("/app/metrics", metrics);

        // Also broadcast directly to topic for immediate delivery
        messagingTemplate.convertAndSend("/topic/metrics", json.toString());

        LOGGER.info("Forwarded metrics from agent: {} to STOMP broker", metrics.getAgentId());
    }

    private void handleTest(WebSocketSession session, JsonObject json) throws Exception {
        String message = json.has("message") ? json.get("message").getAsString() : "No message";
        LOGGER.info("Test message from {}: {}", session.getId(), message);

        // Echo back
        session.sendMessage(new TextMessage("{\"type\":\"test_response\",\"message\":\"Echo: " + message + "\"}"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        String agentId = (String) session.getAttributes().get("agentId");
        sessions.remove(sessionId);
        LOGGER.info("Plain WebSocket connection closed: {} (agent: {}), status: {}",
                   sessionId, agentId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

