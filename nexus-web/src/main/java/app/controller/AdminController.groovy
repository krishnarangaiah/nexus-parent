package app.controller

import app.config.AppProperty
import app.service.user.UserService
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
import org.thymeleaf.spring6.view.ThymeleafViewResolver

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

}
