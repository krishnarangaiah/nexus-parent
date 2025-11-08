package app.controller;

import app.dao.model.monitoring.Agent;
import app.dao.model.monitoring.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
public class MonitoringAgentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringAgentController.class);

    @Autowired
    private AgentService agentService;

    @GetMapping("/Monitoring/Agent/Landing")
    public String landing(Model model) {
        LOGGER.info("Agent Monitoring Controller accessed.");
        model.addAttribute("agents", agentService.findAll());
        return "agent/monitoring/Landing.html";
    }

    @GetMapping("/Monitoring/Agent/CreateForm")
    public String getCreateForm() {
        LOGGER.info("Agent Monitoring Create Form accessed.");
        return "agent/monitoring/Create.html";
    }

    // -----------------------
    // Existing form-backed handler (keeps redirect/flash behavior)
    // -----------------------
    @PostMapping("/Monitoring/Agent/Create")
    public String createAgent(@ModelAttribute Agent agent, RedirectAttributes redirectAttributes) {
        try {
            agentService.save(agent);
            redirectAttributes.addFlashAttribute("actionMsg", "Agent created successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to create agent", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to create agent: " + e.getMessage());
        }
        return "redirect:/Monitoring/Agent/Landing";
    }

    // -----------------------
    // New JSON-aware endpoint for AJAX clients (consumes JSON)
    // -----------------------
    @PostMapping(path = "/Monitoring/Agent/Create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createAgentJson(@RequestBody Agent agent) {
        if (agent == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Request body is empty or invalid"));
        }
        // basic server-side validation
        if (agent.getAgentId() == null || agent.getAgentId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "agentId is required"));
        }
        if (agent.getDisplayName() == null || agent.getDisplayName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "displayName is required"));
        }

        try {
            Agent saved = agentService.save(agent);
            // return created entity (or minimal success payload)
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            LOGGER.error("Failed to create agent (JSON)", e);
            Map<String, String> body = Collections.singletonMap("error", e.getMessage() != null ? e.getMessage() : "Failed to create agent");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
    }

    // Update handler (form-backed)
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

    // JSON-aware update endpoint for AJAX
    @PostMapping(path = "/Monitoring/Agent/Update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateAgentJson(@RequestBody Agent agent) {
        if (agent == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Request body is empty or invalid"));
        }
        if (agent.getAgentId() == null || agent.getAgentId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "agentId is required"));
        }
        if (agent.getDisplayName() == null || agent.getDisplayName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "displayName is required"));
        }
        try {
            Agent saved = agentService.save(agent);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            LOGGER.error("Failed to update agent (JSON)", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage() != null ? e.getMessage() : "Failed to update agent"));
        }
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

    // optional: edit form endpoint (could be implemented later)
    @GetMapping("/Monitoring/Agent/Edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Agent a = agentService.findById(id);
        model.addAttribute("agent", a);
        return "agent/monitoring/Edit.html"; // implement later
    }
}
