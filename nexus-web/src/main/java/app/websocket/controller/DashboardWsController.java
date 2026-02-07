package app.websocket.controller;

import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 */
@Controller
public class DashboardWsController {

    private static final Logger LOG = LogManager.getLogger(DashboardWsController.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Called when a client subscribes to /app/dashboard.
     */
    @SubscribeMapping("/dashboard")
    public Map<String, Object> onDashboardSubscribe() {
        LOG.info("Client subscribed to dashboard");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "welcome");
        response.put("message", "Connected to dashboard");
        response.put("timestamp", now());

        return response;
    }

    /**
     * Handle dashboard refresh request.
     */
    @MessageMapping("/dashboard/refresh")
    @SendTo(WebSocketTopics.DASHBOARD)
    public Map<String, Object> handleRefreshRequest(Map<String, Object> request) {
        LOG.info("Dashboard refresh requested");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "refresh_response");
        response.put("status", "success");
        response.put("timestamp", now());

        return response;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
