package app.websocket.controller;

import app.websocket.dto.Metrics;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MetricsController {

    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(MetricsController.class);

    @MessageMapping("/metrics")
    @SendTo("/topic/metrics")
    public String getMetrics(Metrics metrics) {
        LOGGER.info("Received metrics: " + metrics);
        return "Processed Metrics: " + metrics;
    }

}
