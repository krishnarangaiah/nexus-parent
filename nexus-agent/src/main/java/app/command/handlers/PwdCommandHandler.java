package app.command.handlers;

import app.command.CommandHandler;
import app.command.NexusCommandHandler;

@NexusCommandHandler
public class PwdCommandHandler implements CommandHandler {
    @Override
    public String handle(String args) {
        return System.getProperty("user.dir");
    }
}

