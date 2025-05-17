package app.websocket.eventHandler;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompSessionEventListener {


    private final Map<String, ClientInfo> sessionMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userAgent = accessor.getFirstNativeHeader("user-agent"); // Or custom headers
        sessionMap.put(sessionId, new ClientInfo(sessionId, userAgent, Instant.now()));
        System.out.println("CONNECTED: " + sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionMap.remove(sessionId);
        System.out.println("DISCONNECTED: " + sessionId);
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
