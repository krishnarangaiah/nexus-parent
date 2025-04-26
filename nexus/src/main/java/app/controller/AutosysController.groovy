package app.controller

import app.AppFileUtil
import app.controller.enums.AutosysAttribute
import app.dao.model.autosys.AutosysJob
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Controller
class AutosysController {

    private static final Logger LOGGER = LoggerFactory.getLogger AutosysController


    @GetMapping("/Autosys/Landing")
    def autosysLanding() {
        LOGGER.info "Autosys Landing Page"
        "app/autosys/AutosysDefinitionUploadForm"
    }

    @PostMapping("/Autosys/UploadDefinitionFile")
    def uploadAutosysDefinitionFile(@RequestParam("file") MultipartFile file) {
        Path filePath = AppFileUtil.getUserDir().resolve(file.getOriginalFilename())
        try (InputStream input = file.getInputStream()) {
            // Save the file
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
            LOGGER.info("Uploaded file: {}", filePath.toString())
            // Process the file
            filePath.toFile().withInputStream { fileInputStream ->
                // Read the file content
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream))
                String line
                String jobName
                boolean saveOldJob = false
                while ((line = reader.readLine()) != null) {
                    line = line.trim()
                    // Here you can parse the line and extract the attributes
                    if (null != line && !line.isBlank()) {
                        String[] parts = line.split("\\s+")
                        if (parts.length > 0) {
                            String attribute = parts[0]
                            if (AutosysAttribute.INSERT_JOB.attributeName.equalsIgnoreCase(attribute)) {
                                if(saveOldJob){
                                    saveTheJobToDB(new AutosysJob(jobName: jobName))
                                }
                                jobName = parts[1]
                                LOGGER.info("Start of Job: {}", jobName)
                                saveOldJob = true
                            } else {
                                LOGGER.info("Processing Job: {} for attribute: {}", jobName, attribute)
                            }
                        }
                    }
                }
                // Perform the specific task at the end of the file
                if (saveOldJob) {
                    saveTheJobToDB(new AutosysJob(jobName: jobName))
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error processing file: {}", e.getMessage(), e)
        }
        "app/autosys/AutosysDefinitionUploadForm"
    }

    private saveTheJobToDB(AutosysJob job) {
        // Implement the logic to save the job to the database
        LOGGER.info("Saving job to DB: {}", job)
    }

}
