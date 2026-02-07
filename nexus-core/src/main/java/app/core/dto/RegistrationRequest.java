package app.core.dto;

import java.io.Serializable;

/**
 * Agent Registration Request
 *
 * Sent by agent to server when WebSocket connection is established.
 * Server validates the agentUuid against registered agents in database.
 */
public class RegistrationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique agent identifier (passed as command-line argument to agent jar) */
    private String agentUuid;

    /** Machine hostname */
    private String hostname;

    /** IP address of the machine */
    private String ipAddress;

    /** Operating system name */
    private String osName;

    /** Operating system version */
    private String osVersion;

    /** Directory where agent jar is running */
    private String workingDirectory;

    /** Agent jar version */
    private String agentVersion;

    /** Java version on the machine */
    private String javaVersion;

    /** Timestamp when agent started */
    private long startedAt;

    // ==================== CONSTRUCTORS ====================

    public RegistrationRequest() {}

    public RegistrationRequest(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    // ==================== BUILDER PATTERN ====================

    public static RegistrationRequest create(String agentUuid) {
        return new RegistrationRequest(agentUuid);
    }

    public RegistrationRequest hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public RegistrationRequest ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public RegistrationRequest osName(String osName) {
        this.osName = osName;
        return this;
    }

    public RegistrationRequest osVersion(String osVersion) {
        this.osVersion = osVersion;
        return this;
    }

    public RegistrationRequest workingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public RegistrationRequest agentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
        return this;
    }

    public RegistrationRequest javaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    public RegistrationRequest startedAt(long startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getAgentUuid() { return agentUuid; }
    public void setAgentUuid(String agentUuid) { this.agentUuid = agentUuid; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    @Override
    public String toString() {
        return "RegistrationRequest{" +
                "agentUuid='" + agentUuid + '\'' +
                ", hostname='" + hostname + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                '}';
    }
}

