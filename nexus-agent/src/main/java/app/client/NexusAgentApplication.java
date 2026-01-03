package app.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(
    exclude = {
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class
    }
)
public class NexusAgentApplication implements CommandLineRunner {

    @Value("${agent.websocket.url}")
    private String serverUrl;

    @Value("${agent.websocket.test-message:{\"type\":\"test\",\"message\":\"Hello from Spring Boot agent\"}}")
    private String testMessage;

    @Value("${agent.websocket.send-interval-ms:5000}")
    private long sendIntervalMs;

    private volatile Session session;
    private volatile boolean running = true;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NexusAgentApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // Disable web server
        app.run(args);
    }

    @Override
    public void run(String... args) {
        while (running) {
            CountDownLatch connectionLatch = new CountDownLatch(1);
            try {
                System.out.println("🔗 Attempting to connect to: " + serverUrl);
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                session = container.connectToServer(new SimpleClientEndpoint(connectionLatch), URI.create(serverUrl));

                // Wait for connection
                if (connectionLatch.await(10, TimeUnit.SECONDS)) {
                    System.out.println("✅ Connection established successfully!");
                    while (session != null && session.isOpen() && running) {
                        session.getBasicRemote().sendText(testMessage);
                        System.out.println("📤 Sent: " + testMessage);
                        Thread.sleep(sendIntervalMs);
                    }
                } else {
                    System.err.println("❌ Connection timeout!");
                }
            } catch (Exception e) {
                System.err.println("❌ Connection failed: " + e.getMessage());
            }

            // Wait before reconnecting
            try {
                if (running) {
                    System.out.println("⏳ Waiting 5 seconds before reconnecting...");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("🏁 Agent stopped.");
    }

    public class SimpleClientEndpoint extends Endpoint {

        private final CountDownLatch connectionLatch;

        public SimpleClientEndpoint(CountDownLatch connectionLatch) {
            this.connectionLatch = connectionLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            System.out.println("✅ Connected successfully!");
            connectionLatch.countDown();
            session.addMessageHandler(String.class, message -> {
                System.out.println("📥 Received: " + message);
            });
        }

        @Override
        public void onError(Session session, Throwable thr) {
            System.err.println("❌ WebSocket error: " + thr.getMessage());
            connectionLatch.countDown();
            closeSession();
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            System.out.println("🔌 Connection closed: " + closeReason.getReasonPhrase());
            closeSession();
        }

        private void closeSession() {
            try {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (Exception ignore) {}
        }
    }
}
