package app.client;

import app.command.CommandHandler;
import app.command.handlers.PwdCommandHandler;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Command Dispatcher
 *
 * Handles command execution on the agent.
 * Supports:
 * - Built-in command handlers
 * - Dynamic Groovy script execution
 */
@Component
public class CommandDispatcher {

    private static final Logger LOG = LogManager.getLogger(CommandDispatcher.class);

    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();
    private final GroovyShell groovyShell;

    public CommandDispatcher() {
        // Register built-in handlers
        register("PWD", new PwdCommandHandler());

        // Initialize Groovy shell
        groovyShell = new GroovyShell();

        LOG.info("CommandDispatcher initialized with {} built-in handlers", commandHandlers.size());
    }

    /**
     * Register a built-in command handler.
     */
    public void register(String command, CommandHandler handler) {
        commandHandlers.put(command.toUpperCase(), handler);
    }

    /**
     * Dispatch a simple command (for built-in handlers).
     */
    public String dispatch(String command, String args) {
        CommandHandler handler = commandHandlers.get(command.toUpperCase());
        if (handler != null) {
            return handler.handle(args);
        }
        return "Unknown command: " + command;
    }

    /**
     * Execute a Groovy script.
     *
     * @param commandName Name of the command (for logging)
     * @param scriptContent The Groovy script to execute
     * @param arguments Arguments to pass to the script (available as 'args' variable)
     * @return Script output as string
     */
    public String executeScript(String commandName, String scriptContent, String arguments) {
        LOG.info("Executing script: {}", commandName);

        // Check for built-in command first
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            CommandHandler handler = commandHandlers.get(commandName.toUpperCase());
            if (handler != null) {
                return handler.handle(arguments);
            }
            return "No script content provided for command: " + commandName;
        }

        try {
            // Set up binding with variables available to the script
            Binding binding = new Binding();
            binding.setVariable("args", arguments);
            binding.setVariable("commandName", commandName);

            // Capture output
            StringWriter outputWriter = new StringWriter();
            binding.setVariable("out", new PrintWriter(outputWriter));

            // Execute script
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(scriptContent);

            // Get captured output
            String capturedOutput = outputWriter.toString();

            // Return result or captured output
            if (result != null) {
                return result.toString();
            } else if (!capturedOutput.isEmpty()) {
                return capturedOutput;
            } else {
                return "Script executed successfully (no output)";
            }

        } catch (Exception e) {
            LOG.error("Script execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }
}
