package app.controller

import app.entity.command.CommandExecution
import app.entity.command.CommandScript
import app.entity.agent.Agent
import app.service.command.CommandExecutionService
import app.service.command.CommandScriptService
import app.service.agent.AgentService
import app.websocket.agent.AgentConnectionManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

/**
 * Command Controller
 *
 * Handles:
 * - Command script management (CRUD)
 * - Sending commands to agents
 * - Viewing command execution history
 */
@Controller
@RequestMapping("/Command")
class CommandController {

    private static final Logger LOG = LogManager.getLogger(CommandController)

    @Autowired
    private CommandScriptService scriptService

    @Autowired
    private CommandExecutionService executionService

    @Autowired
    private AgentService agentService

    @Autowired
    private AgentConnectionManager connectionManager

    // ==================== SCRIPT MANAGEMENT PAGES ====================

    /**
     * List all command scripts.
     */
    @GetMapping("/Scripts")
    String listScripts(Model model) {
        model.addAttribute("scripts", scriptService.findAll())
        model.addAttribute("activeScripts", scriptService.findActive())
        return "command/ScriptList.html"
    }

    /**
     * Create script form.
     */
    @GetMapping("/Script/Create")
    String createScriptForm(Model model) {
        model.addAttribute("script", new CommandScript())
        return "command/ScriptForm.html"
    }

    /**
     * Edit script form.
     */
    @GetMapping("/Script/Edit/{id}")
    String editScriptForm(@PathVariable("id") Long id, Model model) {
        CommandScript script = scriptService.findById(id)
        if (script == null) {
            return "redirect:/Command/Scripts"
        }
        model.addAttribute("script", script)
        return "command/ScriptForm.html"
    }

    /**
     * Save script (create or update).
     */
    @PostMapping("/Script/Save")
    String saveScript(@ModelAttribute CommandScript script,
                      @RequestParam(value = "currentUser", required = false) String currentUser) {
        if (script.id == null) {
            script.createdBy = currentUser ?: "admin"
        } else {
            script.updatedBy = currentUser ?: "admin"
        }
        scriptService.save(script)
        return "redirect:/Command/Scripts"
    }

    /**
     * Delete script.
     */
    @PostMapping("/Script/Delete/{id}")
    String deleteScript(@PathVariable("id") Long id) {
        scriptService.deleteById(id)
        return "redirect:/Command/Scripts"
    }

    // ==================== COMMAND EXECUTION PAGE ====================

    /**
     * Command console - send commands to agents.
     */
    @GetMapping("/Console")
    String commandConsole(Model model) {
        model.addAttribute("agents", agentService.findAll())
        model.addAttribute("onlineAgents", agentService.findOnlineAgents())
        model.addAttribute("scripts", scriptService.findActive())
        model.addAttribute("connectedAgentUuids", connectionManager.getConnectedAgentUuids())
        return "command/Console.html"
    }

    /**
     * Execute a command on selected agents (AJAX).
     */
    @PostMapping("/Execute")
    @ResponseBody
    ResponseEntity<?> executeCommand(@RequestBody ExecuteCommandRequest request) {
        LOG.info("Execute command request: script={}, agents={}",
            request.scriptId, request.agentUuids)

        // Validate script
        CommandScript script = scriptService.findById(request.scriptId)
        if (script == null) {
            return ResponseEntity.badRequest().body([
                error: "Script not found: ${request.scriptId}"
            ])
        }

        // Validate agents
        if (request.agentUuids == null || request.agentUuids.isEmpty()) {
            return ResponseEntity.badRequest().body([
                error: "No agents selected"
            ])
        }

        // Execute on each agent
        List<Map<String, Object>> results = []

        request.agentUuids.each { agentUuid ->
            CommandExecution execution = connectionManager.sendCommand(
                agentUuid, script, request.arguments, request.initiatedBy ?: "admin"
            )

            if (execution != null) {
                results.add([
                    agentUuid: agentUuid,
                    requestId: execution.requestId,
                    status: "SENT"
                ])
            } else {
                results.add([
                    agentUuid: agentUuid,
                    requestId: null,
                    status: "FAILED",
                    error: "Agent not connected"
                ])
            }
        }

        return ResponseEntity.ok([
            success: true,
            executions: results
        ])
    }

    // ==================== EXECUTION HISTORY ====================

    /**
     * View execution history.
     */
    @GetMapping("/History")
    String executionHistory(Model model,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "50") int size) {
        model.addAttribute("executions", executionService.findRecent(page, size))
        model.addAttribute("currentPage", page)
        return "command/History.html"
    }

    /**
     * View single execution details.
     */
    @GetMapping("/Execution/{id}")
    String executionDetails(@PathVariable("id") Long id, Model model) {
        CommandExecution execution = executionService.findById(id)
        if (execution == null) {
            return "redirect:/Command/History"
        }
        model.addAttribute("execution", execution)
        return "command/ExecutionDetail.html"
    }

    /**
     * Get execution status (AJAX - for polling).
     */
    @GetMapping("/Execution/Status/{requestId}")
    @ResponseBody
    ResponseEntity<?> getExecutionStatus(@PathVariable("requestId") String requestId) {
        CommandExecution execution = executionService.findByRequestId(requestId)
        if (execution == null) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok([
            requestId: execution.requestId,
            status: execution.status,
            output: execution.output,
            errorOutput: execution.errorOutput,
            exitCode: execution.exitCode,
            durationMs: execution.durationMs,
            completedAt: execution.completedAt,
            isComplete: execution.isComplete()
        ])
    }

    // ==================== REQUEST DTOs ====================

    static class ExecuteCommandRequest {
        Long scriptId
        List<String> agentUuids
        String arguments
        String initiatedBy
    }
}

