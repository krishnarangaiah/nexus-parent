package app.session;

import app.entity.user.AppUser;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.Map;


public class SessionUtil {

    private static final Logger LOGGER = LogManager.getLogger(SessionUtil.class);
    private static final String ACTION_MSG = "ACTION_MSG";
    private static final String WARN_MSG = "WARN_MSG";
    private static final String ERROR_MSG = "ERROR_MSG";
    private static final String USER = "USER";

    private SessionUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Cannot create object for class " + SessionUtil.class);
    }

    public static boolean isSessionNew(HttpSession session) {
        return session.isNew();
    }

    public static void setSessionUser(HttpSession session, AppUser appUser) {
        session.setAttribute(USER, appUser);
    }

    public static AppUser getSessionUser(HttpSession session) {
        return (AppUser) session.getAttribute(USER);
    }

    public static void removeSessionUser(HttpSession session) {
        session.removeAttribute(USER);
    }

    public static void setActionMsg(HttpSession session, String msg) {
        session.removeAttribute(ERROR_MSG);
        session.removeAttribute(WARN_MSG);
        session.setAttribute(ACTION_MSG, msg);
    }

    public static String getActionMsg(HttpSession session) {
        return (String) session.getAttribute(ACTION_MSG);
    }

    public static void setErrorMsg(HttpSession session, String msg) {
        session.removeAttribute(WARN_MSG);
        session.removeAttribute(ACTION_MSG);
        session.setAttribute(ERROR_MSG, msg);
    }

    public static String getErrorMsg(HttpSession session) {
        return (String) session.getAttribute(ERROR_MSG);
    }

    public static void setWarnMsg(HttpSession session, String msg) {
        session.removeAttribute(ERROR_MSG);
        session.removeAttribute(ACTION_MSG);
        session.setAttribute(WARN_MSG, msg);
    }

    public static String getWarnMsg(HttpSession session) {
        return (String) session.getAttribute(WARN_MSG);
    }

    public static void cleanViewedMsgs(HttpSession session) {
        if(session != null) {
            LOGGER.debug("Clearing ERROR/ACTION/WARN messages after viewed them");
            session.removeAttribute(ERROR_MSG);
            session.removeAttribute(WARN_MSG);
            session.removeAttribute(ACTION_MSG);
        }else {
            LOGGER.warn("Session is null, cannot clear messages");
        }

    }

    // ----------------------------------------------------------------
    // Map-based overloads to support Thymeleaf session maps (e.g. WebEngineContext$SessionAttributeMap)
    // These allow templates to call T(app.session.SessionUtil).getErrorMsg(session) where session is a map.
    // ----------------------------------------------------------------

    public static String getErrorMsg(Map<?, ?> sessionMap) {
        if (sessionMap == null) return null;
        Object v = sessionMap.get(ERROR_MSG);
        return v != null ? v.toString() : null;
    }

    public static String getWarnMsg(Map<?, ?> sessionMap) {
        if (sessionMap == null) return null;
        Object v = sessionMap.get(WARN_MSG);
        return v != null ? v.toString() : null;
    }

    public static String getActionMsg(Map<?, ?> sessionMap) {
        if (sessionMap == null) return null;
        Object v = sessionMap.get(ACTION_MSG);
        return v != null ? v.toString() : null;
    }

    /**
     * Remove message keys from a session attribute map (used by Thymeleaf render path).
     * If the map supports remove, keys will be removed; otherwise a debug log is emitted.
     */
    public static void cleanViewedMsgs(Map<?, ?> sessionMap) {
        if (sessionMap == null) {
            LOGGER.warn("Session map is null, cannot clear messages");
            return;
        }
        try {
            // remove entries if map is mutable
            if (sessionMap instanceof java.util.concurrent.ConcurrentMap) {
                sessionMap.remove(ERROR_MSG);
                sessionMap.remove(WARN_MSG);
                sessionMap.remove(ACTION_MSG);
            } else {
                // attempt remove for general Map implementations
                sessionMap.remove(ERROR_MSG);
                sessionMap.remove(WARN_MSG);
                sessionMap.remove(ACTION_MSG);
            }
            LOGGER.debug("Cleared messages from session map after rendering");
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Session map is read-only; cannot remove keys: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Failed to clear messages from session map: {}", e.getMessage());
        }
    }

}
