package app.conf;

import app.dao.model.user.AppUser;
import app.service.AppBeanContextService;
import app.session.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppGlobalRequestInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LogManager.getLogger(AppGlobalRequestInterceptor.class);
    private static final String LOGIN_FORM = "/AppUser/LoginForm";
    private static final List<String> ALLOWED_URIS = Arrays.asList(
            "/AppUser/Login",
            LOGIN_FORM,
            "/webjars/bootstrap/5.3.3/css/bootstrap.min.css",
            "/webjars/font-awesome/4.7.0/css/font-awesome.min.css",
            "/webjars/bootstrap/5.3.3/js/bootstrap.min.js",
            "/webjars/jquery/3.5.1/jquery.min.js",
            "/webjars/d3js/5.16.0/d3.min.js",
            "/js/app.js",
            "/css/app.css",
            "/favicon.ico",
            "/error",
            "/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.woff2",
            "/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.woff",
            "/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.ttf",
            "/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.woff2"
    );
    private final AppProperty appProperty = AppBeanContextService.getBeanFromContext(AppProperty.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        AppUser sessionUser = SessionUtil.getSessionUser(session);

        if (ALLOWED_URIS.contains(request.getRequestURI())) {
            LOGGER.info("Allowed URI: {} without Session User", request.getRequestURI());
            return true;
        } else if (sessionUser == null || !session.getId().equals(sessionUser.getSessionId())) {
            LOGGER.warn("No Session User found for sessionId: {}, redirecting to {}", session.getId(), LOGIN_FORM);
            response.sendRedirect(request.getContextPath() + LOGIN_FORM);
            return false;
        } else {
            LOGGER.info("Session User: {} with sessionId: {}", sessionUser, session.getId());
            return true;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            Map<String, Object> model = modelAndView.getModel();
            HttpSession session = request.getSession();
            AppUser sessionUser = SessionUtil.getSessionUser(session);
            LOGGER.info("User: {}", sessionUser);
            model.put("session1", session);
            model.put("sessionUser", sessionUser);
            model.put("sessionId", session.getId());
            model.put("errorMsg", SessionUtil.getErrorMsg(session));
            model.put("actionMsg", SessionUtil.getActionMsg(session));
            model.put("warnMsg", SessionUtil.getWarnMsg(session));
            model.put("appProperty", appProperty);
            model.put("servletContext", request.getServletContext());
        }

    }
}
