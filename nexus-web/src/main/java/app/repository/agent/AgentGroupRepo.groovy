package app.repository.agent

import app.entity.agent.AgentGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AgentGroupRepo extends JpaRepository<AgentGroup, Long> {

    /** Find group by name */
    AgentGroup findByName(String name)

    /** Check if group exists */
    boolean existsByName(String name)

    /** Find all active groups ordered by display order */
    List<AgentGroup> findByActiveTrueOrderByDisplayOrderAsc()
}

