package app.client;

import app.command.CommandHandler;
import app.command.handlers.PwdCommandHandler;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();

    public CommandDispatcher() {
        register("PWD", new PwdCommandHandler());
        // register("LS", new LsCommandHandler());
        // ...register more handlers here...
    }

    public void register(String command, CommandHandler handler) {
        commandHandlers.put(command.toUpperCase(), handler);
    }

    public String dispatch(String command, String args) {
        CommandHandler handler = commandHandlers.get(command.toUpperCase());
        if (handler != null) {
            return handler.handle(args);
        }
        return "Unknown command: " + command;
    }
}
