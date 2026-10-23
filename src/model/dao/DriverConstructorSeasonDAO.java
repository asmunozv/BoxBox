package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.db.DatabaseConnection;
import model.entity.DriverConstructorSeason;

public class DriverConstructorSeasonDAO {

    public int create(DriverConstructorSeason item) {
        validate(item);
        String sql = "INSERT INTO driver_constructor_seasons (id_driver, id_constructor, season) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, item.getIdDriver());
            statement.setInt(2, item.getIdConstructor());
            statement.setInt(3, item.getSeason());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    item.setIdDriverConstructorSeason(id);
                    return id;
                }
            }
            throw new SQLException("No key returned when creating driver-constructor season.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create driver-constructor season.", exception);
        }
    }

    public Optional<DriverConstructorSeason> findById(int id) {
        String sql = "SELECT id_driver_constructor_season, id_driver, id_constructor, season FROM driver_constructor_seasons WHERE id_driver_constructor_season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver-constructor season by id.", exception);
        }
    }

    public List<DriverConstructorSeason> findAll() {
        String sql = "SELECT id_driver_constructor_season, id_driver, id_constructor, season FROM driver_constructor_seasons ORDER BY season DESC, id_driver";
        List<DriverConstructorSeason> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapRow(resultSet));
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver-constructor seasons.", exception);
        }
    }

    public Optional<DriverConstructorSeason> findByDriverAndSeason(int idDriver, int season) {
        String sql = "SELECT id_driver_constructor_season, id_driver, id_constructor, season "
            + "FROM driver_constructor_seasons WHERE id_driver = ? AND season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idDriver);
            statement.setInt(2, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver-constructor season mapping.", exception);
        }
    }

    public Optional<DriverConstructorSeason> findLatestByDriver(int idDriver) {
        String sql = "SELECT id_driver_constructor_season, id_driver, id_constructor, season "
            + "FROM driver_constructor_seasons WHERE id_driver = ? ORDER BY season DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idDriver);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch latest driver-constructor mapping.", exception);
        }
    }

    public boolean update(DriverConstructorSeason item) {
        validateForUpdate(item);
        String sql = "UPDATE driver_constructor_seasons SET id_driver = ?, id_constructor = ?, season = ? WHERE id_driver_constructor_season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, item.getIdDriver());
            statement.setInt(2, item.getIdConstructor());
            statement.setInt(3, item.getSeason());
            statement.setInt(4, item.getIdDriverConstructorSeason());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update driver-constructor season.", exception);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM driver_constructor_seasons WHERE id_driver_constructor_season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete driver-constructor season.", exception);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM driver_constructor_seasons";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all driver-constructor seasons.", exception);
        }
    }

    private DriverConstructorSeason mapRow(ResultSet resultSet) throws SQLException {
        DriverConstructorSeason item = new DriverConstructorSeason();
        item.setIdDriverConstructorSeason(resultSet.getInt("id_driver_constructor_season"));
        item.setIdDriver(resultSet.getInt("id_driver"));
        item.setIdConstructor(resultSet.getInt("id_constructor"));
        item.setSeason(resultSet.getInt("season"));
        return item;
    }

    private void validate(DriverConstructorSeason item) {
        if (item == null) {
            throw new IllegalArgumentException("DriverConstructorSeason cannot be null.");
        }
        if (item.getIdDriver() == null || item.getIdDriver() <= 0) {
            throw new IllegalArgumentException("Driver id is required.");
        }
        if (item.getIdConstructor() == null || item.getIdConstructor() <= 0) {
            throw new IllegalArgumentException("Constructor id is required.");
        }
        if (item.getSeason() == null || item.getSeason() < 1950) {
            throw new IllegalArgumentException("Valid season is required.");
        }
    }

    private void validateForUpdate(DriverConstructorSeason item) {
        validate(item);
        if (item.getIdDriverConstructorSeason() == null || item.getIdDriverConstructorSeason() <= 0) {
            throw new IllegalArgumentException("Driver-constructor season id is required for update.");
        }
    }
}
