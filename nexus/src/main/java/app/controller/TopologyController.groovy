package app.controller;

import app.conf.AppProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class TopologyController {

    private static final Logger LOGGER = LogManager.getLogger(TopologyController.class);

    @Autowired
    private AppProperty appProperty;

    @GetMapping(value = "/Topology/Landing")
    def home(RedirectAttributes attributes) {
        LOGGER.info "Autosys Landing Page"
        "app/topology/Landing"
    }

}
