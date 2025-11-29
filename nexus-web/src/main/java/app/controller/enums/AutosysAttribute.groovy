package app.controller.enums

public enum AutosysAttribute {
    INSERT_JOB("insert_job:"),
    JOB_TYPE("job_type:"),
    COMMAND("command:"),
    MACHINE("machine:"),
    OWNER("owner:"),
    PERMISSION("permission:"),
    DATE_CONDITIONS("date_conditions:"),
    DAYS_OF_WEEK("days_of_week:"),
    START_TIMES("start_times:"),
    DESCRIPTION("description:"),
    STD_OUT_FILE("std_out_file:"),
    STD_ERR_FILE("std_err_file:");

    private final String attributeName;

    AutosysAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    String getAttributeName() {
        return attributeName;
    }
}