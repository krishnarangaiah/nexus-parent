package app.controller;

import app.dao.model.monitoring.Agent;
import app.dao.model.monitoring.AgentService;
import app.websocket.dto.Metrics;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.ConcurrentHashMap;


/*
    Enhanced: maintain a server-side view of connected agents (last-seen)
    and broadcast status updates to /topic/agents/status so Landing.html can update live.
*/
@Controller
public class MonitoringAgentController {

    private static final Gson GSON = new Gson();

    // Added: direct Log4j2 logger to ensure messages go through configured log4j2 appenders
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(MonitoringAgentController.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // agentId -> last-seen timestamp (ms)
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    // heartbeat timeout (ms) to consider agent inactive — configurable as reasonable default
    private static final long HEARTBEAT_TIMEOUT_MS = 15_000L;

    @PostConstruct
    public void init() {
        // Dual-write: logger + stdout to ensure visibility regardless of logging config
        LOGGER.info("MonitoringAgentController initialized. lastSeen size={}", lastSeen.size());
        System.out.println("MonitoringAgentController initialized. (stdout fallback)");
    }

    @GetMapping("/Monitoring/Agent/Landing")
    public String landing(Model model) {
         LOGGER.info("Agent Monitoring Controller accessed.");
        System.out.println("Agent Monitoring Controller accessed. (stdout fallback)");
        model.addAttribute("agents", agentService.findAll());
        // add a snapshot of current statuses for initial page render (optional)
        model.addAttribute("agentLastSeenMap", lastSeen);
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
     *
     * Changed: accept Message<Metrics> so MappingJackson2MessageConverter can deserialize JSON object into Metrics.
     * Logs headers and payload to help trace incoming frames and avoid conversion errors seen in logs.
     */
    @MessageMapping("/metrics")
    @SendTo("/topic/metrics")
    public String handleMetrics(Message<Metrics> message) {
        // Console fallback first so we always see arrival
        try {
            System.out.println("STOMP /metrics inbound (stdout): message=" + message);
        } catch (Throwable t) {
            // ignore
        }

        // Log STOMP/native headers and session info for full traceability
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            LOGGER.info("STOMP /metrics received headers={}, sessionId={}", accessor.toNativeHeaderMap(), accessor.getSessionId());
            LOGGER.info("STOMP /metrics received headers={}, sessionId={}", accessor.toNativeHeaderMap(), accessor.getSessionId());
            System.out.println("STOMP /metrics received headers (stdout): " + accessor.toNativeHeaderMap() + " sessionId=" + accessor.getSessionId());
        } catch (Exception he) {
            LOGGER.debug("Could not extract StompHeaderAccessor for message headers: {}", he.getMessage());
            LOGGER.debug("Could not extract StompHeaderAccessor for message headers: {}", he.getMessage());
            System.out.println("Could not extract StompHeaderAccessor for message headers: " + he.getMessage());
        }

        Metrics metrics = message == null ? null : message.getPayload();
        if (metrics == null) {
            LOGGER.warn("Received /metrics with null payload (conversion may have failed). Raw message: {}", message);
            LOGGER.warn("Received /metrics with null payload (conversion may have failed). Raw message: {}", message);
            System.out.println("Received /metrics with null payload (stdout fallback). Raw message: " + message);
            return "Invalid metrics";
        }

        try {
            String agentId = metrics.getAgentId();
            LOGGER.info("Parsed metrics from agent={}: threads={}, heapUsed={}, heapMax={}, timestamp={}",
                    agentId, metrics.getThreads(), metrics.getHeapUsed(), metrics.getHeapMax(), metrics.getTimestamp());
            LOGGER.info("Parsed metrics from agent={}: threads={}, heapUsed={}, heapMax={}, timestamp={}",
                    agentId, metrics.getThreads(), metrics.getHeapUsed(), metrics.getHeapMax(), metrics.getTimestamp());
            System.out.println("Parsed metrics from agent (stdout): " + agentId + " ts=" + metrics.getTimestamp());

            long ts = metrics.getTimestamp() > 0 ? metrics.getTimestamp() : Instant.now().toEpochMilli();
            lastSeen.put(agentId, ts);

            // Determine status based on lastSeen timestamp
            String status;
            long now = Instant.now().toEpochMilli();
            if ((now - ts) > HEARTBEAT_TIMEOUT_MS) {
                status = "INACTIVE";
            } else {
                status = "ACTIVE";
            }

            // Broadcast a compact status update to subscribed dashboards
            messagingTemplate.convertAndSend("/topic/agents/status",
                    Collections.unmodifiableMap(new ConcurrentHashMap<String, Object>() {{
                        put("agentId", agentId);
                        put("status", status);
                        put("timestamp", ts);
                    }}));

            LOGGER.info("Metrics processed for agent={}, threads={}, heapUsed={}, status={}", agentId, metrics.getThreads(), metrics.getHeapUsed(), status);
            LOGGER.info("Metrics processed for agent={}, threads={}, heapUsed={}, status={}", agentId, metrics.getThreads(), metrics.getHeapUsed(), status);
            System.out.println("Metrics processed for agent (stdout): " + agentId + " status=" + status);
        } catch (Exception e) {
            LOGGER.error("Failed processing metrics payload", e);
            LOGGER.error("Failed processing metrics payload", e);
            System.out.println("Failed processing metrics payload (stdout): " + e.getMessage());
            return "Processing error";
        }

        return "Processed Metrics";
    }

}
