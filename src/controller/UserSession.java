package controller;

public final class UserSession {

    private static Integer currentUserId;
    private static String currentUsername;

    private UserSession() {
    }

    public static void login(int userId, String username) {
        currentUserId = userId;
        currentUsername = username;
    }

    public static void logout() {
        currentUserId = null;
        currentUsername = null;
    }

    public static Integer getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }
}
