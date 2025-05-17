package app.client;

import app.websocket.JvmMetricsCollector;
import app.websocket.ResilientWebSocketClient;
import app.websocket.StompSession;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NexusAgentApp {

    public static void main(String[] args) throws Exception {
        URI uri = new URI("ws://localhost:9000/ws");

        ResilientWebSocketClient wsClient = new ResilientWebSocketClient(uri, msg -> {
            System.out.println("RECV: " + msg);
        });

        // Create and start STOMP session
        StompSession session = new StompSession(wsClient);
        session.connect();

        // Subscribe to /topic/ack
        session.subscribe("/topic/ack", "sub-001");

        // Scheduled task to send metrics every 10 seconds
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            String metrics = JvmMetricsCollector.collectMetrics(); // your existing logic
            session.send("/app/metrics", metrics);
        }, 0, 5, TimeUnit.SECONDS);
    }

}
