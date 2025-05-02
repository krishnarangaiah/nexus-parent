package app.controller

import app.conf.AppProperty
import app.dao.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminController {

    private static final Logger LOGGER = LogManager.getLogger(AdminController.class)

    @Autowired
    private AppProperty appProperty
    @Autowired
    private UserService userService

    @GetMapping("/Admin/Landing")
    public String userLanding(Model model, HttpServletRequest request) {

        List<Map<String, String>> breadcrumbs = new ArrayList<>()
        breadcrumbs.add(Map.of("label", "Home", "url", "/"))
        breadcrumbs.add(Map.of("label", "Back", "url", request.getHeader("referer")))

        model.addAttribute("breadcrumbs", breadcrumbs);

        return "app/admin/Landing.html";
    }

}
