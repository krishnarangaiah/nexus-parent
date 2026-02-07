package app;

import app.config.AppProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusWebApplication {

    private static final Logger LOGGER = LogManager.getLogger(NexusWebApplication.class);

    public static void main(String[] args) {
        LOGGER.info("Application Starting");
        ConfigurableApplicationContext appContext = SpringApplication.run(NexusWebApplication.class, args);
        LOGGER.info("-----------------------------------");
        LOGGER.info("Application is Started successfully");
        LOGGER.info("-----------------------------------");
        AppProperty appProperty = appContext.getBean(AppProperty.class);
        LOGGER.info("Application is started with {}: {}", appProperty.getClass().getName(), appProperty);
    }
}

