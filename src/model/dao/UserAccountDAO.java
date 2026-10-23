package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import model.db.DatabaseConnection;

public class UserAccountDAO {

    public UserAccountDAO() {
        ensureSchema();
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = "SELECT id_user, username, password FROM app_users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserAccount(
                        resultSet.getInt("id_user"),
                        resultSet.getString("username"),
                        resultSet.getString("password")
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch user.", exception);
        }
    }

    public int createUser(String username, String password) {
        String sql = "INSERT INTO app_users (username, password) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("No generated key returned for user create.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create user.", exception);
        }
    }

    private void ensureSchema() {
        // Schema preparation is administrative; opening the application only validates it.
        String sql = "SELECT id_user, username, password FROM app_users WHERE 1 = 0";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet ignored = statement.executeQuery()) {
            // Selecting the columns verifies that the restored table is available.
        } catch (SQLException exception) {
            throw new DataAccessException("La tabla app_users de BoxBox no está preparada.", exception);
        }
    }

    public static class UserAccount {
        private final int idUser;
        private final String username;
        private final String password;

        public UserAccount(int idUser, String username, String password) {
            this.idUser = idUser;
            this.username = username;
            this.password = password;
        }

        public int getIdUser() {
            return idUser;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
