package model.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.config.DatabaseConfig;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
            DatabaseConfig.getUrl(),
            DatabaseConfig.getUsername(),
            DatabaseConfig.getPassword()
        );
        try {
            try (PreparedStatement statement = connection.prepareStatement("SELECT DATABASE(), CURRENT_USER()");
                 ResultSet result = statement.executeQuery()) {
                if (!result.next() || !DatabaseConfig.DATABASE_NAME.equals(result.getString(1))
                    || !(DatabaseConfig.DATABASE_USER + "@localhost").equals(result.getString(2))) {
                    throw new SQLException("La conexión no corresponde a la base y cuenta exclusivas de BoxBox.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SET SESSION time_zone = '+00:00'")) {
                statement.execute();
            }
            return connection;
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
