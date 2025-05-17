package app.websocket;

import org.java_websocket.client.WebSocketClient;


public class StompSession {
    private final WebSocketClient client;

    public StompSession(WebSocketClient client) {
        this.client = client;
    }

    public void connect() {
        client.connect();
        while (!client.isOpen()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        String connectFrame = "CONNECT\n" +
                "accept-version:1.2\n" +
                "host:localhost\n" +
                "heart-beat:10000,10000\n" +  // 10 seconds heartbeat intervals
                "\n\u0000";
        client.send(connectFrame);
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
