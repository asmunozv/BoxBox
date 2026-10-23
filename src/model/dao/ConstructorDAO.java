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
import model.entity.Constructor;
import model.entity.ConstructorSeasonStats;

public class ConstructorDAO {

    public int create(Constructor constructorEntity) {
        validate(constructorEntity);
        String sql = "INSERT INTO constructors (ergast_constructor_id, name, country) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, constructorEntity.getErgastConstructorId());
            statement.setString(2, constructorEntity.getName());
            statement.setString(3, constructorEntity.getCountry());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    constructorEntity.setIdConstructor(id);
                    return id;
                }
            }
            throw new SQLException("No key returned when creating constructor.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create constructor.", exception);
        }
    }

    public Optional<Constructor> findById(int idConstructor) {
        String sql = "SELECT id_constructor, ergast_constructor_id, name, country FROM constructors WHERE id_constructor = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idConstructor);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructor by id.", exception);
        }
    }

    public Optional<Constructor> findByErgastConstructorId(String ergastConstructorId) {
        String sql = "SELECT id_constructor, ergast_constructor_id, name, country FROM constructors WHERE ergast_constructor_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ergastConstructorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructor by Ergast id.", exception);
        }
    }

    public Optional<Constructor> findByName(String name) {
        String sql = "SELECT id_constructor, ergast_constructor_id, name, country FROM constructors WHERE LOWER(name) = LOWER(?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructor by name.", exception);
        }
    }

    public List<Constructor> findAll() {
        String sql = "SELECT id_constructor, ergast_constructor_id, name, country FROM constructors ORDER BY name";
        List<Constructor> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapRow(resultSet));
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructors.", exception);
        }
    }

    public boolean update(Constructor constructorEntity) {
        validateForUpdate(constructorEntity);
        String sql = "UPDATE constructors SET ergast_constructor_id = ?, name = ?, country = ? WHERE id_constructor = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, constructorEntity.getErgastConstructorId());
            statement.setString(2, constructorEntity.getName());
            statement.setString(3, constructorEntity.getCountry());
            statement.setInt(4, constructorEntity.getIdConstructor());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update constructor.", exception);
        }
    }

    public boolean delete(int idConstructor) {
        String sql = "DELETE FROM constructors WHERE id_constructor = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idConstructor);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete constructor.", exception);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM constructors";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all constructors.", exception);
        }
    }

    public int deleteConstructorsWithoutResultsInSeason(int season) {
        String sql = "DELETE FROM constructors WHERE id_constructor NOT IN ("
            + "SELECT DISTINCT r.id_constructor FROM results r "
            + "INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE ra.season = ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete constructors not used in season.", exception);
        }
    }

    public int createIfNotExists(Constructor constructorEntity) {
        validate(constructorEntity);
        Optional<Constructor> existing = findByErgastConstructorId(constructorEntity.getErgastConstructorId());
        if (existing.isPresent()) {
            return existing.get().getIdConstructor();
        }
        return create(constructorEntity);
    }

    public void mergeConstructorReferences(int sourceConstructorId, int targetConstructorId) {
        String updateResults = "UPDATE results SET id_constructor = ? WHERE id_constructor = ?";
        String updateMappings = "UPDATE driver_constructor_seasons SET id_constructor = ? WHERE id_constructor = ?";
        String deleteSource = "DELETE FROM constructors WHERE id_constructor = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement updateResultsStatement = connection.prepareStatement(updateResults);
                 PreparedStatement updateMappingsStatement = connection.prepareStatement(updateMappings);
                 PreparedStatement deleteSourceStatement = connection.prepareStatement(deleteSource)) {
                updateResultsStatement.setInt(1, targetConstructorId);
                updateResultsStatement.setInt(2, sourceConstructorId);
                updateResultsStatement.executeUpdate();

                updateMappingsStatement.setInt(1, targetConstructorId);
                updateMappingsStatement.setInt(2, sourceConstructorId);
                updateMappingsStatement.executeUpdate();

                deleteSourceStatement.setInt(1, sourceConstructorId);
                deleteSourceStatement.executeUpdate();

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to merge constructor references.", exception);
        }
    }

    public ConstructorSeasonStats findSeasonStats(int idConstructor, int season) {
        String sql = "SELECT COUNT(*) AS races, "
            + "COALESCE(SUM(CASE WHEN r.finishing_position = 1 THEN 1 ELSE 0 END), 0) AS wins, "
            + "COALESCE(SUM(r.points), 0) AS points "
            + "FROM results r INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE r.id_constructor = ? AND ra.season = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idConstructor);
            statement.setInt(2, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                ConstructorSeasonStats stats = new ConstructorSeasonStats();
                if (resultSet.next()) {
                    stats.setRaces(resultSet.getInt("races"));
                    stats.setWins(resultSet.getInt("wins"));
                    stats.setPoints(resultSet.getBigDecimal("points"));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructor season stats.", exception);
        }
    }

    private Constructor mapRow(ResultSet resultSet) throws SQLException {
        Constructor constructorEntity = new Constructor();
        constructorEntity.setIdConstructor(resultSet.getInt("id_constructor"));
        constructorEntity.setErgastConstructorId(resultSet.getString("ergast_constructor_id"));
        constructorEntity.setName(resultSet.getString("name"));
        constructorEntity.setCountry(resultSet.getString("country"));
        return constructorEntity;
    }

    private void validate(Constructor constructorEntity) {
        if (constructorEntity == null) {
            throw new IllegalArgumentException("Constructor cannot be null.");
        }
        if (isBlank(constructorEntity.getErgastConstructorId())) {
            throw new IllegalArgumentException("Ergast constructor id is required.");
        }
        if (isBlank(constructorEntity.getName())) {
            throw new IllegalArgumentException("Constructor name is required.");
        }
    }

    private void validateForUpdate(Constructor constructorEntity) {
        validate(constructorEntity);
        if (constructorEntity.getIdConstructor() == null || constructorEntity.getIdConstructor() <= 0) {
            throw new IllegalArgumentException("Constructor id is required for update.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
