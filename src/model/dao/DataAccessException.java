package model.dao;

public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(buildMessage(message, cause), cause);
    }

    private static String buildMessage(String message, Throwable cause) {
        if (cause == null) {
            return message;
        }
        String causeName = cause.getClass().getSimpleName();
        String causeMessage = cause.getMessage();
        if (causeMessage == null || causeMessage.isBlank()) {
            return message + " [" + causeName + "]";
        }
        return message + " [" + causeName + ": " + causeMessage + "]";
    }
}
