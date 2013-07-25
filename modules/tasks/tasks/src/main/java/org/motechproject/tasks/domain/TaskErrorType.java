package org.motechproject.tasks.domain;

public enum TaskErrorType {
    BLANK("task.validation.error.blank"),
    EMPTY_COLLECTION("task.validation.error.emptyCollection"),
    NULL("task.validation.error.null"),
    VERSION("task.validation.error.version");

    private String message;

    private TaskErrorType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
