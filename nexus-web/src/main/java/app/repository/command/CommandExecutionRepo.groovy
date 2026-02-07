package app.repository.command

import app.entity.command.CommandExecution
import app.entity.agent.Agent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommandExecutionRepo extends JpaRepository<CommandExecution, Long> {

    /** Find by request ID */
    CommandExecution findByRequestId(String requestId)

    /** Check if request ID exists */
    boolean existsByRequestId(String requestId)

    /** Find all executions for an agent, ordered by sent time desc */
    List<CommandExecution> findByAgentOrderBySentAtDesc(Agent agent)

    /** Find recent executions (paginated) */
    Page<CommandExecution> findAllByOrderBySentAtDesc(Pageable pageable)

    /** Find executions by status */
    List<CommandExecution> findByStatus(String status)

    /** Find pending executions (waiting for response) */
    List<CommandExecution> findByStatusIn(List<String> statuses)

    /** Count executions by status */
    long countByStatus(String status)

    /** Find executions by initiator */
    List<CommandExecution> findByInitiatedByOrderBySentAtDesc(String initiatedBy)

    /** Count today's executions */
    @Query("SELECT COUNT(e) FROM CommandExecution e WHERE e.sentAt >= :startOfDay")
    long countTodayExecutions(@Param("startOfDay") Long startOfDay)
}

