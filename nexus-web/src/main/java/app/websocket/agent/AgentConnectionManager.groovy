package app.websocket.agent

import app.core.dto.CommandRequest
import app.core.dto.CommandResponse
import app.core.dto.RegistrationRequest
import app.core.protocol.MessageType
import app.core.protocol.NexusProtocol
import app.entity.command.CommandExecution
import app.entity.command.CommandScript
import app.entity.agent.Agent
import app.service.command.CommandExecutionService
import app.service.agent.AgentService
import app.websocket.publisher.AgentPublisher
import com.google.gson.JsonObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

import java.util.concurrent.ConcurrentHashMap

/**
 * Agent Connection Manager
 *
 * Manages WebSocket connections from agents:
 * - Validates agent UUID on registration
 * - Tracks active sessions
 * - Routes commands to agents
 * - Processes responses
 */
@Component
class AgentConnectionManager {

    private static final Logger LOG = LogManager.getLogger(AgentConnectionManager)

    /** Map of agentUuid -> WebSocketSession for connected agents */
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>()

    /** Map of sessionId -> agentUuid for reverse lookup */
    private final ConcurrentHashMap<String, String> sessionToAgent = new ConcurrentHashMap<>()

    @Autowired
    private AgentService agentService

    @Autowired
    private CommandExecutionService executionService

    @Autowired
    private AgentPublisher agentPublisher

    // ==================== CONNECTION LIFECYCLE ====================

    /**
     * Handle new WebSocket connection.
     * At this point, agent is connected but not yet registered.
     */
    void onConnect(WebSocketSession session) {
        LOG.info("New agent connection: sessionId={}", session.id)
        // Wait for registration message
    }

    /**
     * Handle WebSocket disconnection.
     */
    void onDisconnect(WebSocketSession session, CloseStatus status) {
        String sessionId = session.id
        String agentUuid = sessionToAgent.remove(sessionId)

        if (agentUuid) {
            activeSessions.remove(agentUuid)
            Agent agent = agentService.markOffline(agentUuid)

            // Notify UI with full agent details
            if (agent != null) {
                agentPublisher.publishAgentConnectionEvent(agent, false)
                // Also publish updated dashboard stats
                agentPublisher.publishDashboardStats(agentService.countAll(), agentService.countOnline())
            } else {
                agentPublisher.publishAgentConnectionEvent(agentUuid, false)
            }

            LOG.info("Agent disconnected: uuid={}, reason={}", agentUuid, status.reason)
        } else {
            LOG.info("Unregistered session disconnected: sessionId={}", sessionId)
        }
    }

    // ==================== MESSAGE HANDLING ====================

    /**
     * Handle incoming message from agent.
     */
    void onMessage(WebSocketSession session, String message) {
        try {
            JsonObject json = NexusProtocol.parse(message)
            String type = NexusProtocol.getType(json)

            switch (type) {
                case MessageType.REGISTRATION:
                    handleRegistration(session, json)
                    break
                case MessageType.HEARTBEAT:
                    handleHeartbeat(session, json)
                    break
                case MessageType.COMMAND_RESPONSE:
                    handleCommandResponse(session, json)
                    break
                default:
                    LOG.warn("Unknown message type: {} from session {}", type, session.id)
            }
        } catch (Exception e) {
            LOG.error("Error processing message from session {}: {}", session.id, e.message, e)
            sendError(session, "PARSE_ERROR", "Invalid message format: " + e.message)
        }
    }

    // ==================== REGISTRATION ====================

    private void handleRegistration(WebSocketSession session, JsonObject json) {
        RegistrationRequest request = NexusProtocol.parseRegistrationRequest(json)
        String agentUuid = request.agentUuid

        LOG.info("Registration request: uuid={}, hostname={}", agentUuid, request.hostname)

        // Validate agent UUID
        if (!agentService.isValidAgent(agentUuid)) {
            LOG.warn("Registration rejected - invalid UUID: {}", agentUuid)
            sendMessage(session, NexusProtocol.createRegistrationRejected(agentUuid,
                "Agent UUID not registered or disabled"))

            // Close connection after rejection
            try {
                session.close(CloseStatus.POLICY_VIOLATION)
            } catch (Exception e) {
                LOG.error("Error closing rejected session", e)
            }
            return
        }

        // Check if already connected
        if (activeSessions.containsKey(agentUuid)) {
            LOG.warn("Agent already connected, closing old session: {}", agentUuid)
            WebSocketSession oldSession = activeSessions.get(agentUuid)
            try {
                oldSession.close(CloseStatus.POLICY_VIOLATION)
            } catch (Exception e) {
                // Ignore
            }
        }

        // Register session
        activeSessions.put(agentUuid, session)
        sessionToAgent.put(session.id, agentUuid)

        // Update agent in database
        Agent agent = agentService.markOnline(
            agentUuid,
            request.hostname,
            request.ipAddress,
            request.osName,
            request.osVersion,
            request.workingDirectory,
            request.agentVersion,
            request.javaVersion
        )

        // Send acknowledgment
        sendMessage(session, NexusProtocol.createRegistrationAck(agentUuid,
            "Registration successful. Welcome ${agent?.displayName ?: agentUuid}!"))

        // Notify UI with full agent details
        if (agent != null) {
            agentPublisher.publishAgentConnectionEvent(agent, true)
            // Also publish updated dashboard stats
            agentPublisher.publishDashboardStats(agentService.countAll(), agentService.countOnline())
        } else {
            agentPublisher.publishAgentConnectionEvent(agentUuid, true)
        }

        LOG.info("Agent registered successfully: uuid={}, displayName={}",
            agentUuid, agent?.displayName)
    }

    // ==================== HEARTBEAT ====================

    private void handleHeartbeat(WebSocketSession session, JsonObject json) {
        String agentUuid = sessionToAgent.get(session.id)
        if (agentUuid) {
            agentService.updateHeartbeat(agentUuid)
            LOG.debug("Heartbeat received from agent: {}", agentUuid)
        }
    }

    // ==================== COMMAND RESPONSE ====================

    private void handleCommandResponse(WebSocketSession session, JsonObject json) {
        CommandResponse response = NexusProtocol.parseCommandResponse(json)
        String requestId = response.requestId

        LOG.info("Command response received: requestId={}, status={}", requestId, response.status)

        // Update execution record
        if (response.success) {
            executionService.markSuccess(requestId, response.output, response.durationMs)
        } else {
            executionService.markFailed(requestId, response.errorOutput, response.exitCode, response.durationMs)
        }

        // Notify UI about command completion
        // This will be picked up by the WebSocket topic subscribers
        agentPublisher.publish("/topic/command/response", [
            requestId: requestId,
            agentUuid: response.agentUuid,
            status: response.status,
            output: response.output,
            errorOutput: response.errorOutput,
            durationMs: response.durationMs
        ])
    }

    // ==================== SEND COMMANDS ====================

    /**
     * Send a command to a specific agent.
     * Returns the CommandExecution record.
     */
    CommandExecution sendCommand(String agentUuid, CommandScript script, String arguments, String initiatedBy) {
        WebSocketSession session = activeSessions.get(agentUuid)

        if (session == null || !session.isOpen()) {
            LOG.warn("Cannot send command - agent not connected: {}", agentUuid)
            return null
        }

        Agent agent = agentService.findByAgentUuid(agentUuid)
        if (agent == null) {
            LOG.warn("Cannot send command - agent not found: {}", agentUuid)
            return null
        }

        // Create execution record
        CommandExecution execution = executionService.create(agent, script, arguments, initiatedBy)

        // Build command request
        CommandRequest request = CommandRequest.create(execution.requestId, script.name)
            .scriptContent(script.scriptContent)
            .arguments(arguments)
            .initiatedBy(initiatedBy)
            .timeoutMs(script.defaultTimeoutMs ?: 30000L)

        // Send to agent
        String message = NexusProtocol.createCommandRequest(request)
        sendMessage(session, message)

        LOG.info("Command sent to agent: uuid={}, requestId={}, command={}",
            agentUuid, execution.requestId, script.name)

        return execution
    }

    /**
     * Send a command to multiple agents.
     */
    List<CommandExecution> sendCommandToAgents(List<String> agentUuids, CommandScript script,
                                                String arguments, String initiatedBy) {
        return agentUuids.collect { uuid ->
            sendCommand(uuid, script, arguments, initiatedBy)
        }.findAll { it != null }
    }

    // ==================== UTILITIES ====================

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message))
            }
        } catch (Exception e) {
            LOG.error("Failed to send message to session {}: {}", session.id, e.message)
        }
    }

    private void sendError(WebSocketSession session, String errorCode, String errorMessage) {
        sendMessage(session, NexusProtocol.createErrorMessage(errorCode, errorMessage))
    }

    // ==================== STATUS QUERIES ====================

    /**
     * Get list of connected agent UUIDs.
     */
    Set<String> getConnectedAgentUuids() {
        return new HashSet<>(activeSessions.keySet())
    }

    /**
     * Check if an agent is currently connected.
     */
    boolean isAgentConnected(String agentUuid) {
        WebSocketSession session = activeSessions.get(agentUuid)
        return session != null && session.isOpen()
    }

    /**
     * Get count of connected agents.
     */
    int getConnectedCount() {
        return activeSessions.size()
    }
}

