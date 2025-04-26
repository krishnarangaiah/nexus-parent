package app.conf;

import app.service.AppBeanContextService;
import app.session.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppGlobalRequestInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppGlobalRequestInterceptor.class);
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

        // LOGGER.info("PreHandle SessionId: {}, RequestContextPath: {}, RequestURI: {}", request.getSession().getId(), request.getContextPath(), request.getRequestURI());

        HttpSession session = request.getSession();
        if (ALLOWED_URIS.contains(request.getRequestURI())) {
            return true;
        } else if ((null == SessionUtil.getSessionUser(session))
                || (!session.getId().equals(SessionUtil.getSessionUser(session).getSessionId()))) {
            LOGGER.warn("No Session User found for sessionId: {}, redirecting to {}", session.getId(), LOGIN_FORM);
            response.sendRedirect(request.getContextPath() + LOGIN_FORM);
            return false;
        } else {
            return true;
        }
        // return super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        LOGGER.debug("PostHandle SessionId: {}", request.getSession().getId());
        LOGGER.debug("Session User: {}", SessionUtil.getSessionUser(request.getSession()));

        if (null != modelAndView) {
            Map<String, Object> x = modelAndView.getModel();
            x.put("appProperty", appProperty);
           // x.put("session", request.getSession());
        }

    }

}
