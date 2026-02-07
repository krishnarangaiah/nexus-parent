package app.core.dto;

import java.io.Serializable;

/**
 * Heartbeat Message
 *
 * Sent by agent to server periodically to indicate it's still alive.
 */
public class HeartbeatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Agent UUID */
    private String agentUuid;

    /** Current timestamp */
    private long timestamp;

    /** CPU usage percentage (optional) */
    private Double cpuUsage;

    /** Memory usage percentage (optional) */
    private Double memoryUsage;

    /** Disk usage percentage (optional) */
    private Double diskUsage;

    /** Number of active threads */
    private Integer activeThreads;

    // ==================== CONSTRUCTORS ====================

    public HeartbeatMessage() {}

    public HeartbeatMessage(String agentUuid) {
        this.agentUuid = agentUuid;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== STATIC FACTORY ====================

    public static HeartbeatMessage create(String agentUuid) {
        return new HeartbeatMessage(agentUuid);
    }

    public HeartbeatMessage cpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
        return this;
    }

    public HeartbeatMessage memoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
        return this;
    }

    public HeartbeatMessage diskUsage(Double diskUsage) {
        this.diskUsage = diskUsage;
        return this;
    }

    public HeartbeatMessage activeThreads(Integer activeThreads) {
        this.activeThreads = activeThreads;
        return this;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getAgentUuid() { return agentUuid; }
    public void setAgentUuid(String agentUuid) { this.agentUuid = agentUuid; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }

    public Double getDiskUsage() { return diskUsage; }
    public void setDiskUsage(Double diskUsage) { this.diskUsage = diskUsage; }

    public Integer getActiveThreads() { return activeThreads; }
    public void setActiveThreads(Integer activeThreads) { this.activeThreads = activeThreads; }

    @Override
    public String toString() {
        return "HeartbeatMessage{agentUuid='" + agentUuid + "', timestamp=" + timestamp + '}';
    }
}

