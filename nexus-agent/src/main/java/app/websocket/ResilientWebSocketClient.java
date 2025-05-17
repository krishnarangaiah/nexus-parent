package app.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ResilientWebSocketClient extends WebSocketClient {
    private final URI serverUri;
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final Consumer<String> onMessageCallback;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int RECONNECT_BACKOFF_MAX = 60; // in seconds

    public ResilientWebSocketClient(URI serverUri, Consumer<String> onMessageCallback) {
        super(serverUri);
        this.serverUri = serverUri;
        this.onMessageCallback = onMessageCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket Opened");
        // Reset ping monitoring
        // startPingMonitoring();
    }

    @Override
    public void onMessage(String message) {
        if (onMessageCallback != null) onMessageCallback.accept(message);
        // Every message received is a "live" connection; reset ping timeout
        resetPingTimeout();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket Closed: " + reason);
        if (shouldReconnect.get()) {
            reconnectWithBackoff();
        }
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    private void reconnectWithBackoff() {
        new Thread(() -> {
            int backoff = 2;
            while (!isOpen() && shouldReconnect.get()) {
                try {
                    System.out.println("Attempting reconnect...");
                    reconnectBlocking();
                    Thread.sleep(backoff * 1000L);
                    backoff = Math.min(backoff * 2, RECONNECT_BACKOFF_MAX); // cap at 60s
                } catch ( Exception e) {
                    System.err.println("Reconnect failed: " + e.getMessage());
                }
            }
        }).start();
    }

    public void stopReconnect() {
        shouldReconnect.set(false);
    }

    private void startPingMonitoring() {
        // Send ping every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                String metrics = JvmMetricsCollector.collectMetrics();
                send(metrics +"\n\n\u0000"); // Minimal ping frame
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void resetPingTimeout() {
        // Reset last received ping time on any message received
        System.out.println("Resetting ping timeout.");
    }
}

