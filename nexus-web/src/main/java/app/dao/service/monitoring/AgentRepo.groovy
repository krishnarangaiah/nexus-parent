package app.dao.service.monitoring

import app.dao.model.monitoring.Agent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AgentRepo extends JpaRepository<Agent, Long> {
    Agent findByAgentId(String agentId)
}
