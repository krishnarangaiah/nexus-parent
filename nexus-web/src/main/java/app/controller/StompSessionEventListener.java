package app.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompSessionEventListener {

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(StompSessionEventListener.class);

    private final Map<String, ClientInfo> sessionMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.info("StompSessionEventListener initialized.");
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        // Basic server-side evaluation of agent headers provided in CONNECT
        String agentId = accessor.getFirstNativeHeader("agent-id");
        Map<String, java.util.List<String>> nativeHeaders = accessor.toNativeHeaderMap();
        if (agentId == null || agentId.trim().isEmpty()) {
            LOGGER.warn("CONNECT requested WITHOUT agent-id; rejecting (sessionId={} headers={})", sessionId, nativeHeaders);
            // Do not add to sessionMap -> server treats this as not an accepted agent.
            return;
        }
        String userAgent = accessor.getFirstNativeHeader("user-agent");
        sessionMap.put(sessionId, new ClientInfo(sessionId, userAgent + " agentId=" + agentId, Instant.now()));
        LOGGER.info("CONNECT requested: sessionId={} agentId={} nativeHeaders={}", sessionId, agentId, nativeHeaders);
       }

    @EventListener
    public void handleSessionConnectedComplete(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        LOGGER.info("CONNECT completed: sessionId={} headers={}", sessionId, accessor.toNativeHeaderMap());
        }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        LOGGER.info("SUBSCRIBE: sessionId={} destination={} nativeHeaders={}", sessionId, destination, accessor.toNativeHeaderMap());
        }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subId = accessor.getSubscriptionId();
        LOGGER.info("UNSUBSCRIBE: sessionId={} subscriptionId={} headers={}", sessionId, subId, accessor.toNativeHeaderMap());
       }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionMap.remove(sessionId);
        LOGGER.info("DISCONNECTED: {} ", sessionId);
        }

    public Map<String, ClientInfo> getConnectedClients() {
        return sessionMap;
    }

    public static class ClientInfo {
        public String sessionId;
        public String userAgent;
        public Instant connectedAt;

        public ClientInfo(String sid, String agent, Instant time) {
            this.sessionId = sid;
            this.userAgent = agent;
            this.connectedAt = time;
        }
    }
}
