package app.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger LOGGER = LogManager.getLogger(DashboardController.class);

    @GetMapping(value = "/AppUser/Dashboard")
    public String showUserDashboard() {
        LOGGER.info("Displaying dashboard");
        return "app/dashboard/Dashboard.html";
    }

}
