package app.websocket.publisher;

import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Base WebSocket publisher that provides common functionality for
 * sending messages to WebSocket topics.
 *
 * Usage:
 * - Extend this class for page-specific publishers, OR
 * - Inject and use directly with the publish() method
 */
@Component
public class WebSocketPublisher {

    private static final Logger LOGGER = LogManager.getLogger(WebSocketPublisher.class);

    @Autowired
    protected SimpMessagingTemplate messagingTemplate;

    /**
     * Publish a message to a specific topic.
     *
     * @param topic   The topic to publish to (use constants from {@link WebSocketTopics})
     * @param payload The payload to send (will be serialized to JSON)
     */
    public void publish(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
            LOGGER.debug("Published message to topic {}: {}", topic, payload);
        } catch (Exception e) {
            LOGGER.error("Failed to publish to topic {}: {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Publish a message to a specific user on a topic.
     *
     * @param user    The username to send the message to
     * @param topic   The topic destination (will be prefixed with /user/{username})
     * @param payload The payload to send
     */
    public void publishToUser(String user, String topic, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(user, topic, payload);
            LOGGER.debug("Published message to user {} on topic {}", user, topic);
        } catch (Exception e) {
            LOGGER.error("Failed to publish to user {} on topic {}: {}", user, topic, e.getMessage(), e);
        }
    }
}

