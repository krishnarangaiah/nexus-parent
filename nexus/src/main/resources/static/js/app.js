function displayActionMsg(msg) {
    $("#actionMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayErrorMsg(msg) {
    $("#errorMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayWarningMsg(msg) {
    $("#warnMsgId").text(msg).show().delay(5000).fadeOut();
}