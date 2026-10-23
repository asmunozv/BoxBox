package controller;

import model.dao.UserAccountDAO;

public class AuthController {

    private final UserAccountDAO userAccountDAO;

    public AuthController() {
        this.userAccountDAO = new UserAccountDAO();
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String normalized = username.trim();
        if (normalized.isBlank() || password.isBlank()) {
            return false;
        }

        return userAccountDAO.findByUsername(normalized)
            .filter(user -> user.getPassword().equals(password))
            .map(user -> {
                UserSession.login(user.getIdUser(), user.getUsername());
                return true;
            })
            .orElse(false);
    }

    public boolean register(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String normalized = username.trim();
        if (normalized.isBlank() || password.isBlank()) {
            return false;
        }
        if (userAccountDAO.findByUsername(normalized).isPresent()) {
            return false;
        }
        int id = userAccountDAO.createUser(normalized, password);
        UserSession.login(id, normalized);
        return true;
    }

}
