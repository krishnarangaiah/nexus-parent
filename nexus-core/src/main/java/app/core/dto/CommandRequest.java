package app.core.dto;

import java.io.Serializable;

/**
 * Command Request
 *
 * Sent by server to agent to execute a command.
 * The requestId is used to correlate the response.
 */
public class CommandRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique request identifier for tracking */
    private String requestId;

    /** Command name or script identifier */
    private String commandName;

    /** Script content (Groovy script to execute) */
    private String scriptContent;

    /** Command arguments (if any) */
    private String arguments;

    /** User who initiated the command */
    private String initiatedBy;

    /** Timestamp when command was sent */
    private long sentAt;

    /** Timeout in milliseconds (0 = no timeout) */
    private long timeoutMs;

    // ==================== CONSTRUCTORS ====================

    public CommandRequest() {}

    public CommandRequest(String requestId, String commandName) {
        this.requestId = requestId;
        this.commandName = commandName;
        this.sentAt = System.currentTimeMillis();
    }

    // ==================== STATIC FACTORY ====================

    public static CommandRequest create(String requestId, String commandName) {
        return new CommandRequest(requestId, commandName);
    }

    public CommandRequest scriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
        return this;
    }

    public CommandRequest arguments(String arguments) {
        this.arguments = arguments;
        return this;
    }

    public CommandRequest initiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
        return this;
    }

    public CommandRequest timeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCommandName() { return commandName; }
    public void setCommandName(String commandName) { this.commandName = commandName; }

    public String getScriptContent() { return scriptContent; }
    public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }

    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    @Override
    public String toString() {
        return "CommandRequest{" +
                "requestId='" + requestId + '\'' +
                ", commandName='" + commandName + '\'' +
                '}';
    }
}

