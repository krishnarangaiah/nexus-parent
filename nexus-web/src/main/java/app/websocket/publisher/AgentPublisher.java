package app.websocket.publisher;

import app.entity.agent.Agent;
import app.websocket.topic.WebSocketTopics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent Monitoring WebSocket publisher.
 * Publishes real-time agent status and metrics updates.
 *
 * Topics handled:
 * - /topic/agents/status - Agent status updates
 * - /topic/agents/metrics - Agent metrics data
 */
@Component
public class AgentPublisher extends WebSocketPublisher {

    private static final Logger LOGGER = LogManager.getLogger(AgentPublisher.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Publish agent status updates (bulk).
     *
     * @param agents Map of agentId to Agent data
     */
    public void publishAgentStatusBulk(Map<String, Agent> agents) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "agent_status_bulk");
        payload.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        payload.put("agents", agents);

        publish(WebSocketTopics.AGENTS_STATUS, payload);
        LOGGER.debug("Published bulk agent status for {} agents", agents.size());
    }

    /**
     * Publish a single agent status update.
     *
     * @param agentId     Agent identifier
     * @param status      Agent status (ONLINE, OFFLINE, etc.)
     * @param displayName Agent display name
     */
    public void publishAgentStatus(String agentId, String status, String displayName) {
        Map<String, Object> agentData = new LinkedHashMap<>();
        agentData.put("type", "agent_status");
        agentData.put("agentId", agentId);
        agentData.put("status", status);
        agentData.put("displayName", displayName);
        agentData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        publish(WebSocketTopics.AGENTS_STATUS, agentData);
        LOGGER.debug("Published agent status: {} - {}", agentId, status);
    }

    /**
     * Publish agent metrics data.
     *
     * @param agentId   Agent identifier
     * @param cpuUsage  CPU usage percentage
     * @param memoryUsage Memory usage percentage
     * @param diskUsage Disk usage percentage
     */
    public void publishAgentMetrics(String agentId, double cpuUsage, double memoryUsage, double diskUsage) {
        Map<String, Object> metricsData = new LinkedHashMap<>();
        metricsData.put("type", "agent_metrics");
        metricsData.put("agentId", agentId);
        metricsData.put("cpuUsage", cpuUsage);
        metricsData.put("memoryUsage", memoryUsage);
        metricsData.put("diskUsage", diskUsage);
        metricsData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        publish(WebSocketTopics.AGENTS_METRICS, metricsData);
        LOGGER.debug("Published metrics for agent {}: cpu={}%, mem={}%, disk={}%",
                agentId, cpuUsage, memoryUsage, diskUsage);
    }

    /**
     * Publish agent connection event.
     *
     * @param agentId Agent identifier
     * @param connected true if connected, false if disconnected
     */
    public void publishAgentConnectionEvent(String agentId, boolean connected) {
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("type", connected ? "agent_connected" : "agent_disconnected");
        eventData.put("agentUuid", agentId);
        eventData.put("status", connected ? "ONLINE" : "OFFLINE");
        eventData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        publish(WebSocketTopics.AGENTS_STATUS, eventData);
        LOGGER.info("Agent {} {}", agentId, connected ? "connected" : "disconnected");
    }

    /**
     * Publish agent connection event with full agent details.
     *
     * @param agent The agent entity
     * @param connected true if connected, false if disconnected
     */
    public void publishAgentConnectionEvent(Agent agent, boolean connected) {
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("type", connected ? "agent_connected" : "agent_disconnected");
        eventData.put("agentUuid", agent.getAgentUuid());
        eventData.put("displayName", agent.getDisplayName());
        eventData.put("hostname", agent.getHostname());
        eventData.put("status", connected ? "ONLINE" : "OFFLINE");
        eventData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        publish(WebSocketTopics.AGENTS_STATUS, eventData);
        LOGGER.info("Agent {} ({}) {}", agent.getDisplayName(), agent.getAgentUuid(),
            connected ? "connected" : "disconnected");
    }

    /**
     * Publish dashboard statistics update.
     * Call this when agent counts change.
     */
    public void publishDashboardStats(long totalAgents, long onlineAgents) {
        Map<String, Object> statsData = new LinkedHashMap<>();
        statsData.put("type", "dashboard_stats");
        statsData.put("totalAgents", totalAgents);
        statsData.put("onlineAgents", onlineAgents);
        statsData.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));

        publish(WebSocketTopics.AGENTS_STATUS, statsData);
        LOGGER.debug("Published dashboard stats: total={}, online={}", totalAgents, onlineAgents);
    }
}


