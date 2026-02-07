package app.entity.command

import app.entity.agent.Agent
import jakarta.persistence.*

/**
 * Command Execution Entity
 *
 * Records each command execution request and its result.
 * Provides audit trail and history of all commands sent to agents.
 */
@Entity
@Table(name = "command_execution")
class CommandExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    /** Unique request ID for tracking (used to correlate response) */
    @Column(unique = true, nullable = false)
    String requestId

    /** The agent this command was sent to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    Agent agent

    /** The script that was executed */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "script_id")
    CommandScript script

    /** Script content at time of execution (in case script is later modified) */
    @Column(columnDefinition = "TEXT")
    String executedScriptContent

    /** Arguments passed to the script */
    @Column(columnDefinition = "TEXT")
    String arguments

    /** Execution status: PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT, ERROR */
    @Column(nullable = false)
    String status = STATUS_PENDING

    /** Standard output from execution */
    @Column(columnDefinition = "TEXT")
    String output

    /** Error output from execution */
    @Column(columnDefinition = "TEXT")
    String errorOutput

    /** Exit code (0 = success) */
    @Column
    Integer exitCode

    /** Execution duration in milliseconds */
    @Column
    Long durationMs

    /** Username who initiated the command */
    @Column
    String initiatedBy

    /** When the command was sent */
    @Column
    Long sentAt = System.currentTimeMillis()

    /** When the response was received */
    @Column
    Long completedAt

    // ==================== STATUS CONSTANTS ====================

    static final String STATUS_PENDING = "PENDING"
    static final String STATUS_RUNNING = "RUNNING"
    static final String STATUS_SUCCESS = "SUCCESS"
    static final String STATUS_FAILED = "FAILED"
    static final String STATUS_TIMEOUT = "TIMEOUT"
    static final String STATUS_ERROR = "ERROR"

    // ==================== HELPER METHODS ====================

    boolean isComplete() {
        return status in [STATUS_SUCCESS, STATUS_FAILED, STATUS_TIMEOUT, STATUS_ERROR]
    }

    boolean isSuccess() {
        return STATUS_SUCCESS == status
    }

    void markSuccess(String output, Long durationMs) {
        this.status = STATUS_SUCCESS
        this.output = output
        this.exitCode = 0
        this.durationMs = durationMs
        this.completedAt = System.currentTimeMillis()
    }

    void markFailed(String errorOutput, Integer exitCode, Long durationMs) {
        this.status = STATUS_FAILED
        this.errorOutput = errorOutput
        this.exitCode = exitCode
        this.durationMs = durationMs
        this.completedAt = System.currentTimeMillis()
    }

    void markError(String errorMessage) {
        this.status = STATUS_ERROR
        this.errorOutput = errorMessage
        this.exitCode = -1
        this.completedAt = System.currentTimeMillis()
    }

    void markTimeout() {
        this.status = STATUS_TIMEOUT
        this.errorOutput = "Command execution timed out"
        this.completedAt = System.currentTimeMillis()
    }

    // ==================== FORMATTED DATE GETTERS ====================

    @Transient
    String getSentAtFormatted() {
        if (sentAt == null) return "-"
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(sentAt))
    }

    @Transient
    String getSentAtTimeOnly() {
        if (sentAt == null) return "-"
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new Date(sentAt))
    }

    @Transient
    String getCompletedAtFormatted() {
        if (completedAt == null) return "-"
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(completedAt))
    }
}

