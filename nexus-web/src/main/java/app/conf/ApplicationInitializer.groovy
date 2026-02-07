package app.conf

import app.dao.model.command.CommandScriptService
import app.dao.model.monitoring.Agent
import app.dao.model.monitoring.AgentGroup
import app.dao.model.monitoring.AgentService
import app.dao.service.monitoring.AgentGroupRepo
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Application Initializer
 *
 * Runs on application startup to initialize:
 * - Built-in command scripts
 * - Default agent groups
 * - Sample agents (for development)
 */
@Component
class ApplicationInitializer {

    private static final Logger LOG = LogManager.getLogger(ApplicationInitializer)

    @Autowired
    private CommandScriptService scriptService

    @Autowired
    private AgentService agentService

    @Autowired
    private AgentGroupRepo agentGroupRepo

    @EventListener(ApplicationReadyEvent)
    void onApplicationReady() {
        LOG.info("Initializing application data...")

        initializeBuiltInScripts()
        initializeDefaultGroups()
        initializeSampleAgents()

        LOG.info("Application initialization complete.")
    }

    private void initializeBuiltInScripts() {
        LOG.info("Initializing built-in command scripts...")
        scriptService.initializeBuiltInScripts()
    }

    private void initializeDefaultGroups() {
        LOG.info("Initializing default agent groups...")

        createGroupIfNotExists("default", "Default", "Default agent group", 0, "#6c757d", "bi-collection")
        createGroupIfNotExists("production", "Production", "Production servers", 1, "#dc3545", "bi-server")
        createGroupIfNotExists("staging", "Staging", "Staging/UAT servers", 2, "#fd7e14", "bi-hdd-stack")
        createGroupIfNotExists("development", "Development", "Development machines", 3, "#28a745", "bi-laptop")
    }

    private void createGroupIfNotExists(String name, String displayName, String description,
                                         int order, String color, String icon) {
        if (!agentGroupRepo.existsByName(name)) {
            AgentGroup group = new AgentGroup(
                name: name,
                displayName: displayName,
                description: description,
                displayOrder: order,
                color: color,
                icon: icon
            )
            agentGroupRepo.save(group)
            LOG.info("Created agent group: {}", displayName)
        }
    }

    private void initializeSampleAgents() {
        // Only create sample agents if none exist (for development)
        if (agentService.countAll() == 0) {
            LOG.info("Creating sample agents for development...")

            AgentGroup devGroup = agentGroupRepo.findByName("development")

            createSampleAgent("agent-dev-001", "Development Agent 1", devGroup,
                "Sample agent for development and testing")
            createSampleAgent("agent-dev-002", "Development Agent 2", devGroup,
                "Another sample agent")
            createSampleAgent("agent-dev-003", "Development Agent 3", devGroup,
                "Third sample agent")

            LOG.info("Sample agents created. Use these UUIDs when starting agents.")
        }
    }

    private void createSampleAgent(String uuid, String displayName, AgentGroup group, String description) {
        Agent agent = new Agent(
            agentUuid: uuid,
            displayName: displayName,
            agentGroup: group,
            description: description,
            status: Agent.STATUS_OFFLINE,
            enabled: true,
            registeredAt: System.currentTimeMillis()
        )
        agentService.save(agent)
        LOG.info("Created sample agent: {} (uuid={})", displayName, uuid)
    }
}

