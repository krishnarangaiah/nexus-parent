package app.controller;

import app.dao.model.monitoring.Agent;
import app.dao.model.monitoring.AgentService;
import app.websocket.dto.Metrics;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
// Added imports for scheduler
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
    Enhanced: maintain a server-side view of connected agents (last-seen)
    and broadcast status updates to /topic/agents/status so Landing.html can update live.
*/
@Controller
public class MonitoringAgentController {

    private static final Gson GSON = new Gson();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(MonitoringAgentController.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Agent> agentSnapshot = new ConcurrentHashMap<>();

    // heartbeat timeout (ms) to consider agent inactive — configurable as reasonable default
    private static final long HEARTBEAT_TIMEOUT_MS = 15_000L;

    // Scheduler to periodically push snapshots to /topic/agents/status
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long SNAPSHOT_PUSH_INTERVAL_MS = 10_000L; // push every 10s
    private static final long SNAPSHOT_PUSH_INITIAL_DELAY_MS = 5_000L; // start after 5s

    @PostConstruct
    public void init() {

        // start periodic snapshot push task
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int count = agentSnapshot.size();
                if (count == 0) {
                    LOGGER.debug("Scheduled push: no agent snapshots to broadcast");
                    return;
                }
                // broadcast each agent's snapshot

                try {
                    messagingTemplate.convertAndSend("/topic/agents/status", agentSnapshot);
                } catch (Throwable t) {
                    LOGGER.debug("Failed to push snapshot for agent");
                }

                LOGGER.info("Scheduled push: broadcasted {} agent snapshots", count);
               } catch (Throwable t) {
                LOGGER.error("Scheduled snapshot push failed", t);
            }
        }, SNAPSHOT_PUSH_INITIAL_DELAY_MS, SNAPSHOT_PUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        try {
            scheduler.shutdownNow();
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOGGER.debug("Error shutting down scheduler: {}", t.getMessage());
        }
    }

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


    /*
     * Websocket-based metrics streaming
     */
    @MessageMapping("/metrics")
    @SendTo("/topic/metrics")
    public String handleMetrics(Message<Metrics> message) {

        Metrics metrics = message == null ? null : message.getPayload();
        if (metrics != null) {
            try {
                String agentId = metrics.getAgentId();
                LOGGER.info("Received metrics from agentId: {}", agentId);
                try {
                    Agent agent = agentService.findByAgentId(agentId);
                    if (agent != null) {
                        agent.setHeartbeat(new Random().nextLong());
                        agentSnapshot.put(agentId, agent);
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Failed retrieving DB agent info for displayName: {}", t.getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Failed processing metrics payload", e);
                return "Processing error";
            }
        }
        return "Processed Metrics";
    }

}
