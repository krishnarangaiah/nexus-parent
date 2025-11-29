package app.controller

import app.dao.model.component.RatesComponent
import app.dao.model.enums.Environment
import app.dao.service.RatesComponentService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.view.RedirectView
import org.thymeleaf.spring6.view.ThymeleafViewResolver

@Controller
public class RatesComponentController {

    private static final Logger LOGGER = LogManager.getLogger(RatesComponentController.class);

    @Autowired
    private RatesComponentService ratesComponentService
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    @GetMapping(value = "/RatesComponent/Landing")
    def home(RedirectAttributes attributes) {
        LOGGER.info "RatesComponent Landing Page"
        ratesComponentService.findAll().each { component ->
            LOGGER.info "RatesComponent: ${component}"
        }
        "app/rates/component/Landing"
    }

    @GetMapping(value = "/Rates/Component/SaveForm")
    String getComponentList(Model model) {
        model.addAttribute("environments", Environment.values());
        return "app/rates/component/SaveForm.html"
    }

    @PostMapping("/Rates/Component/Save")
    public RedirectView addComponent(
            @RequestParam(name = "componentName") String componentName,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "environment") String environment,
            @RequestParam(name = "hasSubprocess", required = false, defaultValue = "false") String hasSubprocess,
            @RequestParam(name = "isActive", required = false, defaultValue = "false") String isActive,
            @RequestParam(name = "host") String host,
            @RequestParam(name = "logFileLocation") String logFileLocation,
            Model model) {

        RatesComponent component = new RatesComponent();
        component.setComponentName(componentName);
        component.setDescription(description);
        component.setEnvironment(Environment.valueOf(environment));
        component.setHasSubprocess("on".equalsIgnoreCase(hasSubprocess));
        component.setIsActive("on".equalsIgnoreCase(isActive));
        component.setHost(host);
        component.setLogFileLocation(logFileLocation);

        ratesComponentService.save(component);
        return new RedirectView("/Rates/Component/List");
    }

    @PostMapping("/Rates/Component/Delete")
    public RedirectView deleteComponent(@RequestBody Map<String, Object> payload, Model model) {
        Long id = Long.valueOf(payload.get("id").toString())
        LOGGER.info "Deleting RatesComponent with ID: ${id}"
        ratesComponentService.deleteById(id);
        return new RedirectView("/Rates/Component/List");
    }

    @GetMapping("/Rates/Component/List")
    String list(Model model, HttpServletRequest request) {
        LOGGER.info "Topology RatesComponent Landing Page"
        var topologyComponents = ratesComponentService.findAll();
        Map<Environment, List<RatesComponent>> grouped = topologyComponents?.groupBy { it.environment } ?: [:]
        model.addAttribute("groupedComponents", grouped)
        model.addAttribute("environments", Environment.values());
        return "app/rates/component/List.html";
    }

    @PostMapping("/RatesComponent/UpdateTopologyComponent")
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
