package app;

import app.conf.AppProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;


public class NexusApplication {

    private static final Logger LOGGER = LogManager.getLogger(NexusApplication.class);

    public static void main(String[] args) {

        LOGGER.info("Application Starting");
        ConfigurableApplicationContext appContext = SpringApplication.run(AppConf.class, args);
        LOGGER.info("-----------------------------------");
        LOGGER.info("Application is Started successfully");
        LOGGER.info("-----------------------------------");
        AppProperty appProperty = appContext.getBean(AppProperty.class);
        LOGGER.info("Application is started with {}: {}", appProperty.getClass().getName(), appProperty);

    }

}
