package app.controller;

import app.conf.AppProperty;
import app.dao.model.user.AppUser;
import app.dao.model.user.Role;
import app.dao.service.UserService;
import app.session.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private AppProperty appProperty;
    @Autowired
    private UserService userService;

    @GetMapping(value = "/AppUser/LoginForm")
    public String loginForm(RedirectAttributes attributes) {
        return "app/user/Login.html";
    }

    @PostMapping(value = "/AppUser/Login")
    public RedirectView login(HttpSession session, @RequestParam String userName, @RequestParam String password) {

        final String appAdminUser = appProperty.getAppAdminUser();
        final String appAdminPassword = appProperty.getAppAdminPassword();
        final String appName = appProperty.getAppName();
        boolean authenticated = false;

        if (appAdminUser.equals(userName) && appAdminPassword.equals(password)) {

            AppUser systemAdmin = new AppUser();
            systemAdmin.setId(-1L);
            systemAdmin.setUserName(appAdminUser);
            systemAdmin.setPassword(appAdminPassword);
            systemAdmin.setSessionId(session.getId());
            SessionUtil.setSessionUser(session, systemAdmin);
            authenticated = true;

            LOGGER.info("AppUser {} Authenticated successfully", userName);
            SessionUtil.setActionMsg(session, "AppUser " + userName + " logged in as System AppUser");
            return new RedirectView("/");

        } else {

            List<AppUser> appUsers = userService.authenticate(userName);

            if (null != appUsers) {
                if (!appUsers.isEmpty()) {
                    if (appUsers.size() == 1) {
                        AppUser appUser = appUsers.get(0);
                        if (appUser.getPassword().equals(password)) {
                            appUser.setSessionId(session.getId());
                            LOGGER.info("AppUser {} Authenticated successfully", userName);
                            return new RedirectView("/");
                        } else {
                            SessionUtil.setErrorMsg(session, "AppUser " + userName + " is not recognized");
                            return new RedirectView("/AppUser/LoginForm");
                        }
                    } else {
                        SessionUtil.setErrorMsg(session, "Multiple appUsers exists with name " + userName);
                        return new RedirectView("/AppUser/LoginForm");
                    }
                } else {
                    SessionUtil.setErrorMsg(session, "AppUser " + userName + " is not recognized");
                    return new RedirectView("/AppUser/LoginForm");
                }
            } else {
                SessionUtil.setErrorMsg(session, "AppUser " + userName + " is not recognized");
                return new RedirectView("/AppUser/LoginForm");
            }
        }

    }

    @GetMapping("/AppUser/Logout")
    public RedirectView logout(HttpSession session) {
        SessionUtil.removeSessionUser(session);
        return new RedirectView("/");
    }

    @GetMapping("/AppUser/Landing")
    public String userLanding() {
        return "app/user/Landing.html";
    }

    @GetMapping(value = "/AppUser/Create")
    public String create(Model model) {
        return "app/user/Create.html";
    }

    @PostMapping(value = "/AppUser/Save")
    public RedirectView save(Model model, @RequestParam String userName, @RequestParam String role, @RequestParam String email) {
        AppUser appUser = new AppUser();
        appUser.setUserName(userName);
        appUser.setPassword("0000");
        appUser.setRole(Role.valueOf(role));
        appUser.setEmail(email);
        userService.save(appUser);
        return new RedirectView("/AppUser/List");
    }

    @GetMapping(value = "/AppUser/List")
    public String list(Model model) {
        List<AppUser> appUsers = userService.findAll();
        model.addAttribute("appUsers", appUsers);
        return "app/user/List.html";
    }

    @GetMapping(value = "/AppUser/Read")
    public String read(Model model, @RequestParam String id) {
        AppUser appUser = userService.findById(Long.parseLong(id));
        model.addAttribute(appUser);
        return "app/appUser/Read.html";
    }

    @PostMapping(value = "/AppUser/Update")
    public RedirectView update(Model model, @RequestParam String id, @RequestParam String userName, @RequestParam String role, @RequestParam String email) {
        AppUser appUser = userService.findById(Long.parseLong(id));
        appUser.setUserName(userName);
        appUser.setPassword("0000");
        appUser.setRole(Role.valueOf(role));
        appUser.setEmail(email);
        userService.save(appUser);
        return new RedirectView("/AppUser/List");
    }

    @GetMapping(value = "/AppUser/Delete")
    public RedirectView delete(Model model, @RequestParam String id) {
        AppUser appUser = userService.findById(Long.parseLong(id));
        userService.delete(appUser);
        model.addAttribute(appUser);
        return new RedirectView("/AppUser/List");
    }

}