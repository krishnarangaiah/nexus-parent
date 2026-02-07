package app.websocket.ontology;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CommunicationProtocol {

    private static final Gson GSON = new Gson();

    public static String createMessage(String type, JsonObject payload) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("payload", payload);
        return GSON.toJson(message);
    }

    public static JsonObject parseMessage(String message) throws IllegalArgumentException {
        try {
            return GSON.fromJson(message, JsonObject.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid message format", e);
        }
    }

    public static String getMessageType(JsonObject message) {
        return message.has("type") ? message.get("type").getAsString() : "unknown";
    }

    public static JsonObject getPayload(JsonObject message) {
        return message.has("payload") ? message.getAsJsonObject("payload") : new JsonObject();
    }
}
