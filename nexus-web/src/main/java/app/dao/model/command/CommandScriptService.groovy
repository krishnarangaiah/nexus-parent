package app.dao.model.command

import app.dao.service.command.CommandScriptRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Command Script Service
 *
 * Manages Groovy scripts that can be executed on agents.
 */
@Service
class CommandScriptService {

    @Autowired
    private CommandScriptRepo repo

    // ==================== FIND METHODS ====================

    List<CommandScript> findAll() {
        return repo.findAll()
    }

    CommandScript findById(Long id) {
        return repo.findById(id).orElse(null)
    }

    CommandScript findByName(String name) {
        return repo.findByName(name)
    }

    List<CommandScript> findActive() {
        return repo.findByActiveTrue()
    }

    List<CommandScript> findByCategory(String category) {
        return repo.findByCategoryAndActiveTrue(category)
    }

    List<CommandScript> findBuiltIn() {
        return repo.findByBuiltInTrue()
    }

    // ==================== SAVE/DELETE ====================

    CommandScript save(CommandScript script) {
        if (script.id != null) {
            // Update - increment version
            script.version = (script.version ?: 0) + 1
            script.updatedAt = System.currentTimeMillis()
        }
        return repo.save(script)
    }

    void deleteById(Long id) {
        CommandScript script = repo.findById(id).orElse(null)
        if (script != null && !script.builtIn) {
            repo.deleteById(id)
        }
    }

    // ==================== VALIDATION ====================

    boolean existsByName(String name) {
        return repo.existsByName(name)
    }

    // ==================== INITIALIZATION ====================

    /**
     * Create default built-in scripts if they don't exist.
     */
    void initializeBuiltInScripts() {
        createBuiltInIfNotExists(
            "system-info",
            "System Info",
            "Returns basic system information",
            '''
// System Info Script
def info = [:]
info.hostname = InetAddress.localHost.hostName
info.osName = System.getProperty("os.name")
info.osVersion = System.getProperty("os.version")
info.javaVersion = System.getProperty("java.version")
info.userDir = System.getProperty("user.dir")
info.freeMemory = Runtime.runtime.freeMemory()
info.totalMemory = Runtime.runtime.totalMemory()
info.availableProcessors = Runtime.runtime.availableProcessors()
return groovy.json.JsonOutput.toJson(info)
''',
            "System"
        )

        createBuiltInIfNotExists(
            "list-files",
            "List Files",
            "Lists files in the working directory",
            '''
// List Files Script
def dir = new File(System.getProperty("user.dir"))
def files = dir.listFiles()?.collect { f ->
    [name: f.name, size: f.length(), isDir: f.isDirectory(), lastModified: f.lastModified()]
} ?: []
return groovy.json.JsonOutput.toJson([count: files.size(), files: files])
''',
            "File System"
        )

        createBuiltInIfNotExists(
            "echo",
            "Echo",
            "Simple echo command - returns the input arguments",
            '''
// Echo Script
return "Echo: ${args ?: 'No arguments provided'}"
''',
            "General"
        )
    }

    private void createBuiltInIfNotExists(String name, String displayName, String description,
                                          String scriptContent, String category) {
        if (!repo.existsByName(name)) {
            CommandScript script = new CommandScript(
                name: name,
                displayName: displayName,
                description: description,
                scriptContent: scriptContent.trim(),
                category: category,
                builtIn: true,
                createdBy: "system"
            )
            repo.save(script)
        }
    }
}

