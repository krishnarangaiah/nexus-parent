package app.core.protocol;

import app.core.dto.CommandRequest;
import app.core.dto.CommandResponse;
import app.core.dto.HeartbeatMessage;
import app.core.dto.RegistrationRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Protocol Utility for WebSocket Message Handling
 *
 * Provides methods to create and parse JSON messages for agent-server communication.
 *
 * Message Format:
 * {
 *   "type": "message_type",
 *   "payload": { ... }
 * }
 */
public final class NexusProtocol {

    private NexusProtocol() {}

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // ==================== MESSAGE CREATION ====================

    /**
     * Create a registration request message.
     */
    public static String createRegistrationMessage(RegistrationRequest request) {
        return createMessage(MessageType.REGISTRATION, request);
    }

    /**
     * Create a registration acknowledgment message.
     */
    public static String createRegistrationAck(String agentUuid, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("agentUuid", agentUuid);
        payload.addProperty("message", message);
        payload.addProperty("timestamp", System.currentTimeMillis());
        return createMessage(MessageType.REGISTRATION_ACK, payload);
    }

    /**
     * Create a registration rejected message.
     */
    public static String createRegistrationRejected(String agentUuid, String reason) {
        JsonObject payload = new JsonObject();
        payload.addProperty("agentUuid", agentUuid);
        payload.addProperty("reason", reason);
        payload.addProperty("timestamp", System.currentTimeMillis());
        return createMessage(MessageType.REGISTRATION_REJECTED, payload);
    }

    /**
     * Create a heartbeat message.
     */
    public static String createHeartbeatMessage(HeartbeatMessage heartbeat) {
        return createMessage(MessageType.HEARTBEAT, heartbeat);
    }

    /**
     * Create a command request message.
     */
    public static String createCommandRequest(CommandRequest request) {
        return createMessage(MessageType.COMMAND_REQUEST, request);
    }

    /**
     * Create a command response message.
     */
    public static String createCommandResponse(CommandResponse response) {
        return createMessage(MessageType.COMMAND_RESPONSE, response);
    }

    /**
     * Create an error message.
     */
    public static String createErrorMessage(String errorCode, String errorMessage) {
        JsonObject payload = new JsonObject();
        payload.addProperty("errorCode", errorCode);
        payload.addProperty("errorMessage", errorMessage);
        payload.addProperty("timestamp", System.currentTimeMillis());
        return createMessage(MessageType.ERROR, payload);
    }

    // ==================== MESSAGE PARSING ====================

    /**
     * Parse a JSON message string into a JsonObject.
     */
    public static JsonObject parse(String message) {
        try {
            return JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON message: " + e.getMessage(), e);
        }
    }

    /**
     * Get the message type from a parsed message.
     */
    public static String getType(JsonObject message) {
        return message.has("type") ? message.get("type").getAsString() : "unknown";
    }

    /**
     * Get the payload from a parsed message.
     */
    public static JsonObject getPayload(JsonObject message) {
        return message.has("payload") ? message.getAsJsonObject("payload") : new JsonObject();
    }

    /**
     * Parse payload into RegistrationRequest.
     */
    public static RegistrationRequest parseRegistrationRequest(JsonObject message) {
        JsonObject payload = getPayload(message);
        return GSON.fromJson(payload, RegistrationRequest.class);
    }

    /**
     * Parse payload into HeartbeatMessage.
     */
    public static HeartbeatMessage parseHeartbeat(JsonObject message) {
        JsonObject payload = getPayload(message);
        return GSON.fromJson(payload, HeartbeatMessage.class);
    }

    /**
     * Parse payload into CommandRequest.
     */
    public static CommandRequest parseCommandRequest(JsonObject message) {
        JsonObject payload = getPayload(message);
        return GSON.fromJson(payload, CommandRequest.class);
    }

    /**
     * Parse payload into CommandResponse.
     */
    public static CommandResponse parseCommandResponse(JsonObject message) {
        JsonObject payload = getPayload(message);
        return GSON.fromJson(payload, CommandResponse.class);
    }

    // ==================== PRIVATE HELPERS ====================

    private static String createMessage(String type, Object payload) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("payload", GSON.toJsonTree(payload));
        return GSON.toJson(message);
    }

    private static String createMessage(String type, JsonObject payload) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("payload", payload);
        return GSON.toJson(message);
    }
}

