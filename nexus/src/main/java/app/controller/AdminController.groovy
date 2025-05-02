package app.controller

import app.conf.AppProperty
import app.dao.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class AdminController {

    private static final Logger LOGGER = LogManager.getLogger(AdminController.class)

    @Autowired
    private AppProperty appProperty
    @Autowired
    private UserService userService

    @GetMapping("/Admin/Landing")
    String userLanding(Model model, HttpServletRequest request) {
        return "app/admin/Landing.html";
    }

    @GetMapping("/Admin/ManageTopologyComponent")
    String manageEnvForm(Model model, HttpServletRequest request) {
        return "app/admin/ManageTopologyComponent.html";
    }

    @PostMapping("/Admin/UpdateTopologyComponent")
    ResponseEntity<?> updateRecord(@RequestBody Map<String, String> payload) {
        String id = payload.get("id");
        String field = payload.get("field");
        String value = payload.get("value");

        // Update the record in the database (pseudo-code)
        boolean success = true; // Replace with actual update logic
        LOGGER.info("Updating record with ID: " + id + ", field: " + field + ", value: " + value);

        if (success) {

            return ResponseEntity.ok("Record updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update record");
        }
    }

}
