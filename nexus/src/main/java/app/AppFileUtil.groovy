package app

import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

import java.nio.file.Files
import java.nio.file.Path

class AppFileUtil {

    private static final Logger LOGGER = LogManager.getLogger AppFileUtil
    public static final String USER_HOME_KEY = "user.home"
    public static final String CURRENT_USER_HOME_DIR = System.getProperty USER_HOME_KEY
    public static final String AUTOSYS_UPLOAD_DIR_NAME = "autosys_uploads"
    private static Path userDir

    static {
        LOGGER.info "Initializing the AppFileUtil"
        userDir = Path.of(CURRENT_USER_HOME_DIR, AUTOSYS_UPLOAD_DIR_NAME)
        if (!userDir.toFile().exists()) {
            LOGGER.info "Creating the directory: {}", userDir.toString()
            Files.createDirectories(userDir)
            LOGGER.info "Created the directory: {}", userDir.toString()
        } else {
            LOGGER.info "Directory: {} already exists, not recreating", userDir.toString()
        }
    }

    static def getUserDir(){
        userDir
    }

}
