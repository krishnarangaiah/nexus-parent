package app.websocket.publisher;

import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard WebSocket Publisher
 *
 * Publishes real-time updates to dashboard topics.
 * Call these methods from controllers/services when data changes.
 *
 * Topics:
 *   /topic/dashboard        - General dashboard events
 *   /topic/dashboard/kpi    - KPI card updates
 *   /topic/dashboard/alerts - Alert notifications
 */
@Component
public class DashboardPublisher extends WebSocketPublisher {

    private static final Logger LOG = LogManager.getLogger(DashboardPublisher.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Publish KPI update to dashboard.
     */
    public void publishKpiUpdate(long totalAgents, long onlineAgents, long totalScripts, long todayExecutions) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "kpi_update");
        data.put("timestamp", now());
        data.put("totalAgents", totalAgents);
        data.put("onlineAgents", onlineAgents);
        data.put("totalScripts", totalScripts);
        data.put("todayExecutions", todayExecutions);

        publish(WebSocketTopics.DASHBOARD_KPI, data);
        LOG.debug("Published KPI update");
    }

    /**
     * Publish an alert notification.
     */
    public void publishAlert(String alertId, String severity, String title, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "alert");
        data.put("timestamp", now());
        data.put("alertId", alertId);
        data.put("severity", severity);
        data.put("title", title);
        data.put("message", message);

        publish(WebSocketTopics.DASHBOARD_ALERTS, data);
        LOG.info("Published alert: {} - {}", alertId, title);
    }

    /**
     * Publish a general dashboard event.
     */
    public void publishEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", eventType);
        event.put("timestamp", now());
        event.put("data", data);

        publish(WebSocketTopics.DASHBOARD, event);
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
