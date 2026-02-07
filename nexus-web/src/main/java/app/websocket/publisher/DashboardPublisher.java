package app.websocket.publisher;

import app.websocket.simulation.SimulationDataProvider;
import app.websocket.simulation.SimulationDataProvider.AlertInfo;
import app.websocket.simulation.SimulationDataProvider.DeploymentInfo;
import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Dashboard WebSocket Publisher
 *
 * Publishes real-time updates to dashboard topics.
 * Currently uses SimulationDataProvider for mock data.
 *
 * Topics:
 *   /topic/dashboard       - General dashboard events
 *   /topic/dashboard/kpi   - KPI card updates (every 5 seconds)
 *   /topic/dashboard/alerts- Alert notifications (random intervals)
 *   /topic/dashboard/deploys - Deployment updates (random intervals)
 *
 * TO USE REAL DATA:
 *   Replace SimulationDataProvider calls with your actual services.
 *   See SimulationDataProvider.java for detailed instructions.
 */
@Component
public class DashboardPublisher extends WebSocketPublisher {

    private static final Logger LOGGER = LogManager.getLogger(DashboardPublisher.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private SimulationDataProvider simulation;

    // Counter to control how often alerts/deploys are sent
    private int tickCounter = 0;

    // ==================== PUBLIC PUBLISH METHODS ====================

    /**
     * Publish KPI (Key Performance Indicator) update.
     */
    public void publishKpiUpdate(int activeAgents, int deploymentsToday, int activeJobs, int activeAlerts) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "kpi_update");
        data.put("timestamp", now());
        data.put("activeAgents", activeAgents);
        data.put("deploymentsToday", deploymentsToday);
        data.put("activeJobs", activeJobs);
        data.put("activeAlerts", activeAlerts);

        publish(WebSocketTopics.DASHBOARD_KPI, data);
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
        LOGGER.info("Alert published: {} - {}", alertId, title);
    }

    /**
     * Publish a deployment status update.
     */
    public void publishDeploymentUpdate(String serviceName, String version, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "deploy_update");
        data.put("timestamp", now());
        data.put("serviceName", serviceName);
        data.put("version", version);
        data.put("status", status);

        publish(WebSocketTopics.DASHBOARD_DEPLOYS, data);
        LOGGER.info("Deployment published: {} v{} - {}", serviceName, version, status);
    }

    // ==================== SCHEDULED BROADCASTS ====================

    /**
     * Broadcasts dashboard updates every 5 seconds.
     *
     * This is the main scheduled method that pushes data to connected clients.
     * Modify the intervals or logic here as needed.
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastDashboardUpdates() {
        tickCounter++;

        try {
            // Always send KPI updates
            sendKpiUpdate();

            // Send deployment updates every ~15 seconds (every 3rd tick)
            if (tickCounter % 3 == 0) {
                sendDeploymentUpdate();
            }

            // Send alert updates every ~25 seconds (every 5th tick), with some randomness
            if (tickCounter % 5 == 0 && new Random().nextBoolean()) {
                sendAlertUpdate();
            }

        } catch (Exception e) {
            LOGGER.error("Error broadcasting dashboard updates: {}", e.getMessage(), e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void sendKpiUpdate() {
        int activeAgents = simulation.getActiveAgentCount();
        int deploymentsToday = simulation.getDeploymentsTodayCount();
        int activeJobs = simulation.getActiveJobsCount();
        int activeAlerts = simulation.getActiveAlertsCount();

        publishKpiUpdate(activeAgents, deploymentsToday, activeJobs, activeAlerts);

        LOGGER.debug("KPI update sent: agents={}, deploys={}, jobs={}, alerts={}",
                activeAgents, deploymentsToday, activeJobs, activeAlerts);
    }

    private void sendDeploymentUpdate() {
        DeploymentInfo deploy = simulation.getLatestDeployment();
        publishDeploymentUpdate(deploy.serviceName, deploy.version, deploy.status);
    }

    private void sendAlertUpdate() {
        AlertInfo alert = simulation.getRandomAlert();
        publishAlert(alert.alertId, alert.severity, alert.title, alert.message);
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}

