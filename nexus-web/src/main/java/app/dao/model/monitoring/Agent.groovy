package app.dao.model.monitoring

import com.google.gson.Gson
import jakarta.persistence.*

/**
 * Agent Entity
 *
 * Represents a registered agent that can connect to the server.
 * Agents must be pre-registered with a UUID before they can connect.
 */
@Entity
@Table(name = "agent")
class Agent {

    private static final long serialVersionUID = 3327643027629528402L
    private static Gson gson = new Gson()

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    /** Unique agent identifier (UUID passed when starting agent jar) */
    @Column(unique = true, nullable = false)
    String agentUuid

    /** Display name shown in UI */
    @Column(nullable = false)
    String displayName

    /** Current status: ONLINE, OFFLINE, UNKNOWN */
    @Column(nullable = false)
    String status = "OFFLINE"

    /** Agent group for logical grouping */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    AgentGroup agentGroup

    /** Hostname of the machine */
    @Column
    String hostname

    /** IP address of the machine */
    @Column
    String ipAddress

    /** Operating system name */
    @Column
    String osName

    /** Operating system version */
    @Column
    String osVersion

    /** Working directory where agent jar is running */
    @Column
    String workingDirectory

    /** Agent version */
    @Column
    String agentVersion

    /** Java version on the machine */
    @Column
    String javaVersion

    /** Last heartbeat timestamp */
    @Column
    Long lastHeartbeat = 0L

    /** When the agent was registered in the system */
    @Column
    Long registeredAt

    /** When the agent last connected */
    @Column
    Long lastConnectedAt

    /** Description/notes about this agent */
    @Column(columnDefinition = "TEXT")
    String description

    /** Is this agent enabled (allowed to connect) */
    @Column
    Boolean enabled = true

    // ==================== STATUS CONSTANTS ====================

    public static final String STATUS_ONLINE = "ONLINE"
    public static final String STATUS_OFFLINE = "OFFLINE"
    public static final String STATUS_UNKNOWN = "UNKNOWN"

    // ==================== HELPER METHODS ====================

    boolean isOnline() {
        return STATUS_ONLINE == status
    }

    void markOnline() {
        this.status = STATUS_ONLINE
        this.lastConnectedAt = System.currentTimeMillis()
    }

    void markOffline() {
        this.status = STATUS_OFFLINE
    }

    void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis()
    }

    // ==================== FORMATTED DATE GETTERS ====================

    @Transient
    String getLastHeartbeatFormatted() {
        if (lastHeartbeat == null || lastHeartbeat == 0) return "Never"
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastHeartbeat))
    }

    @Transient
    String getLastConnectedAtFormatted() {
        if (lastConnectedAt == null || lastConnectedAt == 0) return "Never"
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastConnectedAt))
    }
}
