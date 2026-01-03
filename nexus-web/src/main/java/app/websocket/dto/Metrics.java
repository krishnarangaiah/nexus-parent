package app.websocket.dto;

public class Metrics {
    private String agentId;
    private long timestamp;
    private String type;
    private int threads;
    private long heapUsed;
    private long heapMax;

    // Constructors
    public Metrics() {}

    public Metrics(String agentId, long timestamp, int threads, long heapUsed, long heapMax) {
        this.agentId = agentId;
        this.timestamp = timestamp;
        this.threads = threads;
        this.heapUsed = heapUsed;
        this.heapMax = heapMax;
        this.type = "metrics";
    }

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    public long getHeapUsed() { return heapUsed; }
    public void setHeapUsed(long heapUsed) { this.heapUsed = heapUsed; }

    public long getHeapMax() { return heapMax; }
    public void setHeapMax(long heapMax) { this.heapMax = heapMax; }

    @Override
    public String toString() {
        return "Metrics{" +
                "agentId='" + agentId + '\'' +
                ", timestamp=" + timestamp +
                ", type='" + type + '\'' +
                ", threads=" + threads +
                ", heapUsed=" + heapUsed +
                ", heapMax=" + heapMax +
                '}';
    }
}

