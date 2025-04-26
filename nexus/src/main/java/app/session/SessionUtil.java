package app.session;

import app.dao.model.user.AppUser;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionUtil.class);
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
        LOGGER.debug("Clearing ERROR/ACTION/WARN messages after viewed them");
        session.removeAttribute(ERROR_MSG);
        session.removeAttribute(WARN_MSG);
        session.removeAttribute(ACTION_MSG);
    }

}
