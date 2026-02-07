package app.dao.model.command

import app.dao.model.monitoring.Agent
import app.dao.service.command.CommandExecutionRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Command Execution Service
 *
 * Manages command execution records and history.
 */
@Service
class CommandExecutionService {

    @Autowired
    private CommandExecutionRepo repo

    // ==================== FIND METHODS ====================

    CommandExecution findById(Long id) {
        return repo.findById(id).orElse(null)
    }

    CommandExecution findByRequestId(String requestId) {
        return repo.findByRequestId(requestId)
    }

    List<CommandExecution> findByAgent(Agent agent) {
        return repo.findByAgentOrderBySentAtDesc(agent)
    }

    List<CommandExecution> findPending() {
        return repo.findByStatusIn([CommandExecution.STATUS_PENDING, CommandExecution.STATUS_RUNNING])
    }

    Page<CommandExecution> findRecent(int page, int size) {
        return repo.findAllByOrderBySentAtDesc(PageRequest.of(page, size))
    }

    List<CommandExecution> findByInitiator(String username) {
        return repo.findByInitiatedByOrderBySentAtDesc(username)
    }

    // ==================== CREATE ====================

    /**
     * Create a new command execution record.
     */
    CommandExecution create(Agent agent, CommandScript script, String arguments, String initiatedBy) {
        String requestId = generateRequestId()

        CommandExecution execution = new CommandExecution(
            requestId: requestId,
            agent: agent,
            script: script,
            executedScriptContent: script.scriptContent,
            arguments: arguments,
            status: CommandExecution.STATUS_PENDING,
            initiatedBy: initiatedBy,
            sentAt: System.currentTimeMillis()
        )

        return repo.save(execution)
    }

    /**
     * Create execution records for multiple agents.
     */
    List<CommandExecution> createForAgents(List<Agent> agents, CommandScript script,
                                            String arguments, String initiatedBy) {
        return agents.collect { agent ->
            create(agent, script, arguments, initiatedBy)
        }
    }

    // ==================== UPDATE ====================

    CommandExecution save(CommandExecution execution) {
        return repo.save(execution)
    }

    /**
     * Update execution with successful response.
     */
    CommandExecution markSuccess(String requestId, String output, Long durationMs) {
        CommandExecution execution = repo.findByRequestId(requestId)
        if (execution != null) {
            execution.markSuccess(output, durationMs)
            return repo.save(execution)
        }
        return null
    }

    /**
     * Update execution with failed response.
     */
    CommandExecution markFailed(String requestId, String errorOutput, Integer exitCode, Long durationMs) {
        CommandExecution execution = repo.findByRequestId(requestId)
        if (execution != null) {
            execution.markFailed(errorOutput, exitCode, durationMs)
            return repo.save(execution)
        }
        return null
    }

    /**
     * Update execution with error.
     */
    CommandExecution markError(String requestId, String errorMessage) {
        CommandExecution execution = repo.findByRequestId(requestId)
        if (execution != null) {
            execution.markError(errorMessage)
            return repo.save(execution)
        }
        return null
    }

    // ==================== COUNTS ====================

    long countAll() {
        return repo.count()
    }

    long countByStatus(String status) {
        return repo.countByStatus(status)
    }

    long countPending() {
        return repo.countByStatus(CommandExecution.STATUS_PENDING)
    }

    long countTodayExecutions() {
        Calendar cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return repo.countTodayExecutions(cal.timeInMillis)
    }

    // ==================== HELPERS ====================

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8)
    }
}

