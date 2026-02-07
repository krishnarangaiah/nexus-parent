package app.core.dto;

import java.io.Serializable;

/**
 * Command Response
 *
 * Sent by agent to server after executing a command.
 * The requestId matches the original CommandRequest.
 */
public class CommandResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Request ID from the original command request */
    private String requestId;

    /** Agent UUID that executed the command */
    private String agentUuid;

    /** Execution status: SUCCESS, FAILED, TIMEOUT, ERROR */
    private String status;

    /** Standard output from command execution */
    private String output;

    /** Error output (if any) */
    private String errorOutput;

    /** Exit code (0 = success, non-zero = error) */
    private int exitCode;

    /** Execution duration in milliseconds */
    private long durationMs;

    /** Timestamp when execution completed */
    private long completedAt;

    // ==================== CONSTANTS ====================

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
    public static final String STATUS_ERROR = "ERROR";

    // ==================== CONSTRUCTORS ====================

    public CommandResponse() {}

    public CommandResponse(String requestId, String agentUuid) {
        this.requestId = requestId;
        this.agentUuid = agentUuid;
        this.completedAt = System.currentTimeMillis();
    }

    // ==================== STATIC FACTORY ====================

    public static CommandResponse success(String requestId, String agentUuid, String output) {
        CommandResponse response = new CommandResponse(requestId, agentUuid);
        response.status = STATUS_SUCCESS;
        response.output = output;
        response.exitCode = 0;
        return response;
    }

    public static CommandResponse failed(String requestId, String agentUuid, String errorOutput) {
        CommandResponse response = new CommandResponse(requestId, agentUuid);
        response.status = STATUS_FAILED;
        response.errorOutput = errorOutput;
        response.exitCode = 1;
        return response;
    }

    public static CommandResponse error(String requestId, String agentUuid, String errorMessage) {
        CommandResponse response = new CommandResponse(requestId, agentUuid);
        response.status = STATUS_ERROR;
        response.errorOutput = errorMessage;
        response.exitCode = -1;
        return response;
    }

    public CommandResponse durationMs(long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAgentUuid() { return agentUuid; }
    public void setAgentUuid(String agentUuid) { this.agentUuid = agentUuid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getErrorOutput() { return errorOutput; }
    public void setErrorOutput(String errorOutput) { this.errorOutput = errorOutput; }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }

    @Override
    public String toString() {
        return "CommandResponse{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", exitCode=" + exitCode +
                '}';
    }
}

