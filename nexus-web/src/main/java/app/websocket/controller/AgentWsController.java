package app.websocket.controller;

import app.websocket.publisher.AgentPublisher;
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
 * WebSocket controller for agent monitoring messages.
 *
 * This controller handles:
 * - Client subscriptions to agent topics
 * - Agent status requests
 *
 * Note: Metrics handling is done in MonitoringAgentController to avoid duplication.
 *
 * Client sends to: /app/agents/*
 * Server broadcasts to: /topic/agents/*
 */
@Controller
public class AgentWsController {

    private static final Logger LOGGER = LogManager.getLogger(AgentWsController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private AgentPublisher agentPublisher;

    /**
     * Handle subscription to agent status topic.
     * Sends initial connection confirmation when client subscribes.
     */
    @SubscribeMapping("/agents/status")
    public Map<String, Object> onAgentStatusSubscribe() {
        LOGGER.info("Client subscribed to agent status topic");

        Map<String, Object> welcomeData = new LinkedHashMap<>();
        welcomeData.put("type", "subscribed");
        welcomeData.put("topic", "agents/status");
        welcomeData.put("message", "Connected to agent status updates");
        welcomeData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        return welcomeData;
    }


    /**
     * Handle agent status refresh requests.
     */
    @MessageMapping("/agents/refresh")
    @SendTo(WebSocketTopics.AGENTS_STATUS)
    public Map<String, Object> handleAgentRefresh(Map<String, Object> request) {
        LOGGER.info("Agent status refresh requested: {}", request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "refresh_initiated");
        response.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        return response;
    }
}

