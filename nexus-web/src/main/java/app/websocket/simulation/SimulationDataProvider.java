package app.websocket.simulation;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Simulation Data Provider
 *
 * This class provides simulated/mock data for WebSocket testing.
 * Replace these methods with real service calls when ready.
 *
 * HOW TO REPLACE WITH REAL DATA:
 * 1. Inject your actual service (e.g., AgentService, DeploymentService)
 * 2. Replace the simulated return values with actual service calls
 * 3. Remove the Random-based logic
 *
 * Example:
 *   // Before (simulated):
 *   public int getActiveAgentCount() {
 *       return randomBetween(8, 15);
 *   }
 *
 *   // After (real):
 *   @Autowired private AgentService agentService;
 *   public int getActiveAgentCount() {
 *       return agentService.countActiveAgents();
 *   }
 */
@Component
public class SimulationDataProvider {

    private final Random random = new Random();

    // ==================== DASHBOARD KPIs ====================

    /**
     * Get the count of currently active/online agents.
     * TODO: Replace with agentService.countActiveAgents()
     */
    public int getActiveAgentCount() {
        return randomBetween(8, 15);
    }

    /**
     * Get the count of deployments done today.
     * TODO: Replace with deploymentService.countTodayDeployments()
     */
    public int getDeploymentsTodayCount() {
        return randomBetween(15, 30);
    }

    /**
     * Get the count of currently running jobs.
     * TODO: Replace with jobService.countActiveJobs()
     */
    public int getActiveJobsCount() {
        return randomBetween(3, 10);
    }

    /**
     * Get the count of active/unacknowledged alerts.
     * TODO: Replace with alertService.countActiveAlerts()
     */
    public int getActiveAlertsCount() {
        return randomBetween(0, 5);
    }

    // ==================== AGENT METRICS ====================

    /**
     * Get simulated CPU usage percentage for an agent.
     * TODO: Replace with real metrics from agent heartbeat
     */
    public double getAgentCpuUsage(String agentId) {
        return randomDoubleBetween(10.0, 85.0);
    }

    /**
     * Get simulated memory usage percentage for an agent.
     * TODO: Replace with real metrics from agent heartbeat
     */
    public double getAgentMemoryUsage(String agentId) {
        return randomDoubleBetween(20.0, 75.0);
    }

    /**
     * Get simulated disk usage percentage for an agent.
     * TODO: Replace with real metrics from agent heartbeat
     */
    public double getAgentDiskUsage(String agentId) {
        return randomDoubleBetween(30.0, 90.0);
    }

    // ==================== DEPLOYMENTS ====================

    /**
     * Get a simulated recent deployment.
     * TODO: Replace with deploymentService.getLatestDeployment()
     */
    public DeploymentInfo getLatestDeployment() {
        String[] services = {"user-service", "order-service", "payment-service", "notification-service", "api-gateway"};
        String[] statuses = {"Success", "Success", "Success", "Partial", "Failed"}; // Weighted towards success

        String service = services[random.nextInt(services.length)];
        String version = "1." + random.nextInt(10) + "." + random.nextInt(100);
        String status = statuses[random.nextInt(statuses.length)];

        return new DeploymentInfo(service, version, status);
    }

    // ==================== ALERTS ====================

    /**
     * Get a simulated alert (randomly generated).
     * TODO: Replace with alertService.getLatestAlert()
     */
    public AlertInfo getRandomAlert() {
        String[] severities = {"info", "warning", "danger"};
        String[] titles = {
            "High CPU Usage",
            "Memory Warning",
            "Disk Space Low",
            "Service Restart",
            "Connection Lost"
        };
        String[] messages = {
            "Agent experiencing high resource usage",
            "Threshold exceeded on monitoring check",
            "Immediate attention required",
            "Automatic recovery initiated",
            "Check network connectivity"
        };

        int index = random.nextInt(titles.length);
        String alertId = "alert-" + System.currentTimeMillis();
        String severity = severities[random.nextInt(severities.length)];

        return new AlertInfo(alertId, severity, titles[index], messages[index]);
    }

    // ==================== HELPER METHODS ====================

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private double randomDoubleBetween(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    // ==================== DATA CLASSES ====================

    /**
     * Simple data class for deployment information.
     */
    public static class DeploymentInfo {
        public final String serviceName;
        public final String version;
        public final String status;

        public DeploymentInfo(String serviceName, String version, String status) {
            this.serviceName = serviceName;
            this.version = version;
            this.status = status;
        }
    }

    /**
     * Simple data class for alert information.
     */
    public static class AlertInfo {
        public final String alertId;
        public final String severity;
        public final String title;
        public final String message;

        public AlertInfo(String alertId, String severity, String title, String message) {
            this.alertId = alertId;
            this.severity = severity;
            this.title = title;
            this.message = message;
        }
    }
}

