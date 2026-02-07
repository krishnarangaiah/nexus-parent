package app.websocket.handler;

import app.websocket.agent.AgentConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Plain WebSocket Handler for Agent Connections
 *
 * This handler receives WebSocket connections from nexus-agent instances.
 * All message processing is delegated to AgentConnectionManager.
 *
 * Endpoint: /ws-plain
 */
@Component
public class PlainWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LogManager.getLogger(PlainWebSocketHandler.class);

    @Autowired
    private AgentConnectionManager connectionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("WebSocket connection established: sessionId={}", session.getId());
        connectionManager.onConnect(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        LOG.debug("Message received from session {}: {}", session.getId(),
            payload.length() > 200 ? payload.substring(0, 200) + "..." : payload);

        connectionManager.onMessage(session, payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOG.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        connectionManager.onDisconnect(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        LOG.info("WebSocket connection closed: sessionId={}, status={}",
            session.getId(), closeStatus.getCode());
        connectionManager.onDisconnect(session, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
