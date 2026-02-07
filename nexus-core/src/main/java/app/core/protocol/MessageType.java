package app.core.protocol;

/**
 * Message Types for WebSocket Communication
 *
 * Used by both nexus-agent and nexus-web to ensure consistent message handling.
 *
 * Flow:
 *   Agent connects → sends REGISTRATION
 *   Server validates → sends REGISTRATION_ACK or REGISTRATION_REJECTED
 *   Agent sends HEARTBEAT periodically
 *   Server sends COMMAND_REQUEST
 *   Agent executes and sends COMMAND_RESPONSE
 */
public final class MessageType {

    private MessageType() {}

    // ==================== AGENT → SERVER ====================

    /** Agent registration request (sent on connect) */
    public static final String REGISTRATION = "registration";

    /** Agent heartbeat (sent periodically) */
    public static final String HEARTBEAT = "heartbeat";

    /** Command execution response */
    public static final String COMMAND_RESPONSE = "command_response";

    // ==================== SERVER → AGENT ====================

    /** Registration accepted */
    public static final String REGISTRATION_ACK = "registration_ack";

    /** Registration rejected (invalid UUID) */
    public static final String REGISTRATION_REJECTED = "registration_rejected";

    /** Command execution request */
    public static final String COMMAND_REQUEST = "command_request";

    // ==================== BIDIRECTIONAL ====================

    /** Error message */
    public static final String ERROR = "error";
}

