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
        LOGGER.info("StompSessionEventListener initialized.");
        System.out.println("StompSessionEventListener initialized. (stdout fallback)");
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userAgent = accessor.getFirstNativeHeader("user-agent"); // Or custom headers
        sessionMap.put(sessionId, new ClientInfo(sessionId, userAgent, Instant.now()));
        LOGGER.info("CONNECT requested: sessionId={} nativeHeaders={}", sessionId, accessor.toNativeHeaderMap());
        LOGGER.info("CONNECT requested: sessionId={} nativeHeaders={}", sessionId, accessor.toNativeHeaderMap());
        System.out.println("CONNECT requested (stdout): sessionId=" + sessionId + " nativeHeaders=" + accessor.toNativeHeaderMap());
    }

    @EventListener
    public void handleSessionConnectedComplete(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        LOGGER.info("CONNECT completed: sessionId={} headers={}", sessionId, accessor.toNativeHeaderMap());
        LOGGER.info("CONNECT completed: sessionId={} headers={}", sessionId, accessor.toNativeHeaderMap());
        System.out.println("CONNECT completed (stdout): sessionId=" + sessionId + " headers=" + accessor.toNativeHeaderMap());
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        LOGGER.info("SUBSCRIBE: sessionId={} destination={} nativeHeaders={}", sessionId, destination, accessor.toNativeHeaderMap());
        LOGGER.info("SUBSCRIBE: sessionId={} destination={} nativeHeaders={}", sessionId, destination, accessor.toNativeHeaderMap());
        System.out.println("SUBSCRIBE (stdout): sessionId=" + sessionId + " destination=" + destination + " headers=" + accessor.toNativeHeaderMap());
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subId = accessor.getSubscriptionId();
        LOGGER.info("UNSUBSCRIBE: sessionId={} subscriptionId={} headers={}", sessionId, subId, accessor.toNativeHeaderMap());
        LOGGER.info("UNSUBSCRIBE: sessionId={} subscriptionId={} headers={}", sessionId, subId, accessor.toNativeHeaderMap());
        System.out.println("UNSUBSCRIBE (stdout): sessionId=" + sessionId + " subscriptionId=" + subId + " headers=" + accessor.toNativeHeaderMap());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionMap.remove(sessionId);
        LOGGER.info("DISCONNECTED: {} ", sessionId);
        LOGGER.info("DISCONNECTED: {} ", sessionId);
        System.out.println("DISCONNECTED (stdout): sessionId=" + sessionId);
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
