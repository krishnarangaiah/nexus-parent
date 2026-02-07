package app.websocket.topic;

/**
 * Centralized WebSocket topic definitions.
 * Each page that needs real-time updates should have its dedicated topic defined here.
 *
 * Naming convention:
 * - Topics are prefixed with /topic/ (for broadcasting)
 * - Followed by the page/feature name in lowercase
 * - Use hyphens for multi-word names
 *
 * Example: /topic/dashboard, /topic/agent-monitoring
 */
public final class WebSocketTopics {

    private WebSocketTopics() {
        // Utility class - prevent instantiation
    }

    // ========== Dashboard Topics ==========
    public static final String DASHBOARD = "/topic/dashboard";
    public static final String DASHBOARD_KPI = "/topic/dashboard/kpi";
    public static final String DASHBOARD_ALERTS = "/topic/dashboard/alerts";
    public static final String DASHBOARD_DEPLOYS = "/topic/dashboard/deploys";

    // ========== Agent Monitoring Topics ==========
    public static final String AGENTS_STATUS = "/topic/agents/status";
    public static final String AGENTS_METRICS = "/topic/agents/metrics";

    // ========== Command Topics ==========
    public static final String COMMAND_RESPONSE = "/topic/command/response";

    // ========== Release Topics ==========
    public static final String RELEASE_STATUS = "/topic/release/status";
    public static final String RELEASE_PROGRESS = "/topic/release/progress";

    // ========== System Topics ==========
    public static final String SYSTEM_NOTIFICATIONS = "/topic/system/notifications";
    public static final String SYSTEM_LOGS = "/topic/system/logs";

    // ========== User Topics ==========
    public static final String USER_ACTIVITY = "/topic/user/activity";
}

