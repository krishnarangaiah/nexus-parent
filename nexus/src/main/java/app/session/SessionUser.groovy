package app.session


import app.dao.model.user.AppUser

class SessionUser implements Serializable {

    private static final long serialVersionUID = -3394981889554859781L

    String sessionId
    AppUser user
    SessionMsgType msgType
    String msg
}

enum SessionMsgType {
    ACTION, WARN, ERROR
}