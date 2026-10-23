package controller;

public class ControllerException extends RuntimeException {

    public ControllerException(String message, Throwable cause) {
        super(buildMessage(message, cause), cause);
    }

    private static String buildMessage(String message, Throwable cause) {
        if (cause == null) {
            return message;
        }

        Throwable root = cause;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String detail = root.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = cause.getMessage();
        }
        if (detail == null || detail.isBlank()) {
            detail = root.getClass().getSimpleName();
        }
        return message + "\nDetails: " + detail;
    }
}
