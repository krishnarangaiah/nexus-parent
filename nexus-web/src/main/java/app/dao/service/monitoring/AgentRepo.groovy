package app.dao.service.monitoring

import app.dao.model.monitoring.Agent
import app.dao.model.monitoring.AgentGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AgentRepo extends JpaRepository<Agent, Long> {

    /** Find agent by UUID */
    Agent findByAgentUuid(String agentUuid)

    /** Check if agent with UUID exists */
    boolean existsByAgentUuid(String agentUuid)

    /** Find all agents by status */
    List<Agent> findByStatus(String status)

    /** Find all agents in a group */
    List<Agent> findByAgentGroup(AgentGroup agentGroup)

    /** Find all enabled agents */
    List<Agent> findByEnabledTrue()

    /** Find online agents */
    List<Agent> findByStatusAndEnabledTrue(String status)

    /** Count agents by status */
    long countByStatus(String status)

    /** Count online agents */
    @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = 'ONLINE'")
    long countOnlineAgents()
}
