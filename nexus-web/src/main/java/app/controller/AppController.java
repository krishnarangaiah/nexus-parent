package app.controller;

import app.config.AppProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AppController {

    private static final Logger LOGGER = LogManager.getLogger(AppController.class);

    @Autowired
    private AppProperty appProperty;

    @GetMapping(value = "/")
    public RedirectView home(RedirectAttributes attributes) {
        return new RedirectView("/AppUser/Landing");
    }

}
