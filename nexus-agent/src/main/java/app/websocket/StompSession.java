package app.websocket;

import org.java_websocket.client.WebSocketClient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class StompSession {
    private final WebSocketClient client;
    private final Map<String,String> additionalHeaders;

    public StompSession(WebSocketClient client, Map<String,String> additionalHeaders) {
        this.client = client;
        this.additionalHeaders = additionalHeaders;
    }

    public void connect() {
        client.connect();
        while (!client.isOpen()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("CONNECT\n");
        b.append("accept-version:1.2\n");
        b.append("host:localhost\n");
        // Add provided agent / metadata headers (server will see them in the CONNECT)
        if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
            for (Entry<String,String> e : additionalHeaders.entrySet()) {
                String k = Objects.toString(e.getKey(), "").trim();
                String v = Objects.toString(e.getValue(), "").trim();
                if (!k.isEmpty()) {
                    b.append(k).append(":").append(escapeHeaderValue(v)).append("\n");
                }
            }
        }
        b.append("heart-beat:10000,10000\n");  // 10 seconds heartbeat intervals
        b.append("\n\u0000");
        String connectFrame = b.toString();

        client.send(connectFrame);
    }

    private String escapeHeaderValue(String v) {
        if (v == null) return "";
        // ensure no newlines or null bytes inside header values
        return v.replace("\n", " ").replace("\r", " ").replace("\u0000", "");
    }

    public void subscribe(String destination, String id) {
        String frame = String.format("SUBSCRIBE\nid:%s\ndestination:%s\n\n\u0000", id, destination);
        client.send(frame);
    }

    public void send(String destination, String payload) {
        String frame = "SEND\n" +
                "destination:" + destination + "\n" +
                "content-type:application/json\n\n" +
                payload + "\u0000";
        client.send(frame);
    }
}
