package app;

import app.websocket.WebsocketClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.CountDownLatch;


public class NexusAgentApplication {

    private static final Logger LOGGER = LogManager.getLogger(NexusAgentApplication.class);
    private static CountDownLatch latch;

    public static void main(String[] args) {
        LOGGER.info("Application Starting");
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