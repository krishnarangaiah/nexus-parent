package app.dao.model.monitoring

import app.dao.service.monitoring.AgentRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Agent Service
 *
 * Provides business logic for agent management.
 */
@Service
class AgentService {

    @Autowired
    private AgentRepo repo

    // ==================== FIND METHODS ====================

    List<Agent> findAll() {
        return repo.findAll()
    }

    Agent findById(Long id) {
        return repo.findById(id).orElse(null)
    }

    Agent findByAgentUuid(String agentUuid) {
        return repo.findByAgentUuid(agentUuid)
    }

    List<Agent> findByStatus(String status) {
        return repo.findByStatus(status)
    }

    List<Agent> findOnlineAgents() {
        return repo.findByStatusAndEnabledTrue(Agent.STATUS_ONLINE)
    }

    List<Agent> findEnabledAgents() {
        return repo.findByEnabledTrue()
    }

    // ==================== VALIDATION ====================

    /**
     * Check if an agent UUID is registered and enabled.
     */
    boolean isValidAgent(String agentUuid) {
        Agent agent = repo.findByAgentUuid(agentUuid)
        return agent != null && agent.enabled
    }

    boolean existsByAgentUuid(String agentUuid) {
        return repo.existsByAgentUuid(agentUuid)
    }

    // ==================== COUNT METHODS ====================

    long countAll() {
        return repo.count()
    }

    long countOnline() {
        return repo.countOnlineAgents()
    }

    long countByStatus(String status) {
        return repo.countByStatus(status)
    }

    // ==================== SAVE/DELETE ====================

    Agent save(Agent agent) {
        if (agent.registeredAt == null) {
            agent.registeredAt = System.currentTimeMillis()
        }
        return repo.save(agent)
    }

    void deleteById(Long id) {
        repo.deleteById(id)
    }

    // ==================== STATUS UPDATES ====================

    /**
     * Mark agent as online and update metadata from registration.
     */
    Agent markOnline(String agentUuid, String hostname, String ipAddress,
                     String osName, String osVersion, String workingDirectory,
                     String agentVersion, String javaVersion) {
        Agent agent = repo.findByAgentUuid(agentUuid)
        if (agent != null) {
            agent.markOnline()
            agent.hostname = hostname
            agent.ipAddress = ipAddress
            agent.osName = osName
            agent.osVersion = osVersion
            agent.workingDirectory = workingDirectory
            agent.agentVersion = agentVersion
            agent.javaVersion = javaVersion
            return repo.save(agent)
        }
        return null
    }

    /**
     * Mark agent as offline.
     */
    Agent markOffline(String agentUuid) {
        Agent agent = repo.findByAgentUuid(agentUuid)
        if (agent != null) {
            agent.markOffline()
            return repo.save(agent)
        }
        return null
    }

    /**
     * Update agent heartbeat timestamp.
     */
    void updateHeartbeat(String agentUuid) {
        Agent agent = repo.findByAgentUuid(agentUuid)
        if (agent != null) {
            agent.updateHeartbeat()
            repo.save(agent)
        }
    }
}
