package app.controller;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReleaseController {

    private static final Logger LOGGER = LogManager.getLogger(ReleaseController.class);

    // C
    @PostMapping(value = "/Release/Create")
    public String create() {
        return "app/release/List.html";
    }

    // Rs
    @GetMapping(value = "/Release/List")
    public String landing() {
        return "app/release/List.html";
    }

    //R

    //U

    //D

}
