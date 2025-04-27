package app.websocket;

import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

@ClientEndpoint
public class WebsocketClient {

    private static CountDownLatch latch;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to server");
        try {
            session.getBasicRemote().sendText("Hello Server!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Session closed: " + reason);
        latch.countDown();  // Signal to shutdown
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error occurred: " + throwable.getMessage());
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            String uri = "ws://localhost:9000/ws"; // Replace with your server URI
            System.out.println("Connecting to " + uri);
            container.connectToServer(WebsocketClient.class, URI.create(uri));
            latch.await(); // Wait until session closed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
