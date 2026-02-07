package app.websocket.controller;

import app.websocket.simulation.SimulationDataProvider;
import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard WebSocket Controller
 *
 * Handles incoming WebSocket messages for the dashboard.
 *
 * Message Destinations:
 *   /app/dashboard/refresh  -> Request full dashboard refresh
 *   /app/dashboard/kpi      -> Request KPI data
 *
 * Response Topics:
 *   /topic/dashboard        -> General dashboard responses
 *   /topic/dashboard/kpi    -> KPI data responses
 */
@Controller
public class DashboardWsController {

    private static final Logger LOGGER = LogManager.getLogger(DashboardWsController.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private SimulationDataProvider simulation;

    // ==================== SUBSCRIPTION HANDLERS ====================

    /**
     * Called when a client subscribes to /app/dashboard.
     * Returns a welcome message with connection confirmation.
     */
    @SubscribeMapping("/dashboard")
    public Map<String, Object> onDashboardSubscribe() {
        LOGGER.info("Client subscribed to dashboard");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "welcome");
        response.put("message", "Connected to dashboard");
        response.put("timestamp", now());

        return response;
    }

    // ==================== MESSAGE HANDLERS ====================

    /**
     * Handle dashboard refresh request.
     *
     * Client sends: { "action": "refresh" } to /app/dashboard/refresh
     * Server responds to: /topic/dashboard
     */
    @MessageMapping("/dashboard/refresh")
    @SendTo(WebSocketTopics.DASHBOARD)
    public Map<String, Object> handleRefreshRequest(Map<String, Object> request) {
        LOGGER.info("Dashboard refresh requested");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "refresh_response");
        response.put("status", "success");
        response.put("timestamp", now());

        return response;
    }

    /**
     * Handle KPI data request.
     *
     * Client sends: { "action": "get" } to /app/dashboard/kpi
     * Server responds to: /topic/dashboard/kpi
     */
    @MessageMapping("/dashboard/kpi")
    @SendTo(WebSocketTopics.DASHBOARD_KPI)
    public Map<String, Object> handleKpiRequest(Map<String, Object> request) {
        LOGGER.info("KPI data requested");

        // Get data from simulation (replace with real service later)
        int activeAgents = simulation.getActiveAgentCount();
        int deploymentsToday = simulation.getDeploymentsTodayCount();
        int activeJobs = simulation.getActiveJobsCount();
        int activeAlerts = simulation.getActiveAlertsCount();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "kpi_data");
        response.put("timestamp", now());
        response.put("activeAgents", activeAgents);
        response.put("deploymentsToday", deploymentsToday);
        response.put("activeJobs", activeJobs);
        response.put("activeAlerts", activeAlerts);

        return response;
    }

    // ==================== HELPER METHODS ====================

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}


