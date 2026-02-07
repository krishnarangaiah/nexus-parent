package app.controller;

import app.dao.model.monitoring.Agent;
import app.dao.model.monitoring.AgentService;
import app.websocket.publisher.AgentPublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

/**
 * Agent Monitoring Controller
 *
 * Handles agent CRUD operations and the agent list page.
 */
@Controller
public class MonitoringAgentController {

    private static final Logger LOGGER = LogManager.getLogger(MonitoringAgentController.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentPublisher agentPublisher;

    @GetMapping("/Monitoring/Agent/Landing")
    public String landing(Model model) {
        LOGGER.info("Agent Monitoring Controller accessed.");
        model.addAttribute("agents", agentService.findAll());
        model.addAttribute("onlineCount", agentService.countOnline());
        return "agent/monitoring/Landing.html";
    }

    @GetMapping("/Monitoring/Agent/CreateForm")
    public String getCreateForm(Model model) {
        LOGGER.info("Agent Monitoring Create Form accessed.");
        model.addAttribute("agent", new Agent());
        return "agent/monitoring/Create.html";
    }

    @PostMapping("/Monitoring/Agent/Create")
    public String createAgent(@ModelAttribute Agent agent, RedirectAttributes redirectAttributes) {
        try {
            // Set defaults
            if (agent.getStatus() == null) {
                agent.setStatus(Agent.STATUS_OFFLINE);
            }
            if (agent.getEnabled() == null) {
                agent.setEnabled(true);
            }
            agentService.save(agent);
            redirectAttributes.addFlashAttribute("actionMsg", "Agent registered successfully. UUID: " + agent.getAgentUuid());
        } catch (Exception e) {
            LOGGER.error("Failed to create agent", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to create agent: " + e.getMessage());
        }
        return "redirect:/Monitoring/Agent/Landing";
    }

    @PostMapping(path = "/Monitoring/Agent/Create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createAgentJson(@RequestBody Agent agent) {
        if (agent == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Request body is empty"));
        }
        if (agent.getAgentUuid() == null || agent.getAgentUuid().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "agentUuid is required"));
        }
        if (agent.getDisplayName() == null || agent.getDisplayName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "displayName is required"));
        }

        try {
            if (agent.getStatus() == null) agent.setStatus(Agent.STATUS_OFFLINE);
            if (agent.getEnabled() == null) agent.setEnabled(true);
            Agent saved = agentService.save(agent);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            LOGGER.error("Failed to create agent (JSON)", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/Monitoring/Agent/Update")
    public String updateAgent(@ModelAttribute Agent agent, RedirectAttributes redirectAttributes) {
        try {
            agentService.save(agent);
            redirectAttributes.addFlashAttribute("actionMsg", "Agent updated successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to update agent", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update agent: " + e.getMessage());
        }
        return "redirect:/Monitoring/Agent/Landing";
    }

    @PostMapping("/Monitoring/Agent/Delete/{id}")
    public String deleteAgent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            agentService.deleteById(id);
            redirectAttributes.addFlashAttribute("actionMsg", "Agent deleted");
        } catch (Exception e) {
            LOGGER.error("Failed to delete agent", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete agent: " + e.getMessage());
        }
        return "redirect:/Monitoring/Agent/Landing";
    }

    @GetMapping("/Monitoring/Agent/Edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Agent agent = agentService.findById(id);
        if (agent == null) {
            return "redirect:/Monitoring/Agent/Landing";
        }
        model.addAttribute("agent", agent);
        return "agent/monitoring/Edit.html";
    }

}
