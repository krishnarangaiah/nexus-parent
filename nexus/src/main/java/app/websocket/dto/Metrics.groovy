package app.websocket.dto

import com.google.gson.Gson;

public class Metrics implements Serializable {

    static Gson GSON = new Gson()

    int threads;
    long heapUsed;
    long heapMax;

    @Override
    String toString() {
        return GSON.toJson(this)
    }
}
