package app.controller;

import app.service.command.CommandExecutionService;
import app.service.command.CommandScriptService;
import app.service.agent.AgentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger LOGGER = LogManager.getLogger(DashboardController.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CommandScriptService scriptService;

    @Autowired
    private CommandExecutionService executionService;


    @GetMapping(value = "/AppUser/Dashboard")
    public String showUserDashboard(Model model) {
        LOGGER.info("Displaying dashboard");

        // KPI data
        model.addAttribute("totalAgents", agentService.countAll());
        model.addAttribute("onlineAgents", agentService.countOnline());
        model.addAttribute("totalScripts", scriptService.findActive().size());
        model.addAttribute("todayExecutions", executionService.countTodayExecutions());

        // Agent list
        model.addAttribute("agents", agentService.findAll());

        // Recent executions (last 10)
        model.addAttribute("recentExecutions", executionService.findRecent(0, 10).getContent());

        return "app/dashboard/Dashboard.html";
    }

}
