package app.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(
        exclude = {
                HibernateJpaAutoConfiguration.class,
                DataSourceAutoConfiguration.class
        }
)
public class NexusAgentApplication implements CommandLineRunner {

    private static final Logger LOGGER = LogManager.getLogger(NexusAgentApplication.class);

    @Autowired
    private AgentRunner agentRunner;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NexusAgentApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        LOGGER.info("------------------------------------------------");
        LOGGER.info("Nexus Agent started successfully.");
        LOGGER.info("------------------------------------------------");
    }

    @Override
    public void run(String... args) {
        agentRunner.runAgent();
    }
}
