package model.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.db.DatabaseConnection;
import model.entity.ConstructorStandingRow;
import model.entity.DriverStandingRow;
import model.entity.Result;

public class ResultDAO {

    private static volatile boolean schemaChecked;

    public int create(Result result) {
        ensureSchema();
        validate(result);
        ensureForeignKeysExist(result);
        ensureUniqueRaceDriver(result.getIdRace(), result.getIdDriver(), result.getSessionType(), null);
        String sql = "INSERT INTO results (id_race, id_driver, id_constructor, session_type, finishing_position, points) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, result.getIdRace());
            statement.setInt(2, result.getIdDriver());
            statement.setInt(3, result.getIdConstructor());
            statement.setString(4, normalizeSessionType(result.getSessionType()));
            statement.setInt(5, result.getFinishingPosition());
            statement.setBigDecimal(6, calculatePoints(result.getFinishingPosition(), result.getSessionType()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    result.setIdResult(id);
                    result.setPoints(calculatePoints(result.getFinishingPosition(), result.getSessionType()));
                    return id;
                }
            }
            throw new SQLException("No key returned when creating result.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create result.", exception);
        }
    }

    public Optional<Result> findById(int idResult) {
        ensureSchema();
        String sql = "SELECT id_result, id_race, id_driver, id_constructor, session_type, finishing_position, points FROM results WHERE id_result = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idResult);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch result by id.", exception);
        }
    }

    public List<Result> findAll() {
        ensureSchema();
        String sql = "SELECT id_result, id_race, id_driver, id_constructor, session_type, finishing_position, points FROM results ORDER BY id_race, session_type, finishing_position";
        List<Result> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapRow(resultSet));
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch results.", exception);
        }
    }

    public List<Result> findBySeason(int season) {
        ensureSchema();
        String sql = "SELECT r.id_result, r.id_race, r.id_driver, r.id_constructor, r.session_type, r.finishing_position, r.points "
            + "FROM results r INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE ra.season = ? ORDER BY ra.round_number, r.finishing_position";
        List<Result> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapRow(resultSet));
                }
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch results by season.", exception);
        }
    }

    public boolean update(Result result) {
        ensureSchema();
        validateForUpdate(result);
        ensureForeignKeysExist(result);
        ensureUniqueRaceDriver(result.getIdRace(), result.getIdDriver(), result.getSessionType(), result.getIdResult());
        String sql = "UPDATE results SET id_race = ?, id_driver = ?, id_constructor = ?, session_type = ?, finishing_position = ?, points = ? WHERE id_result = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, result.getIdRace());
            statement.setInt(2, result.getIdDriver());
            statement.setInt(3, result.getIdConstructor());
            statement.setString(4, normalizeSessionType(result.getSessionType()));
            statement.setInt(5, result.getFinishingPosition());
            statement.setBigDecimal(6, calculatePoints(result.getFinishingPosition(), result.getSessionType()));
            statement.setInt(7, result.getIdResult());
            boolean updated = statement.executeUpdate() > 0;
            if (updated) {
                result.setPoints(calculatePoints(result.getFinishingPosition(), result.getSessionType()));
            }
            return updated;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update result.", exception);
        }
    }

    public Optional<Result> findByRaceAndDriver(int idRace, int idDriver) {
        ensureSchema();
        String sql = "SELECT id_result, id_race, id_driver, id_constructor, session_type, finishing_position, points FROM results WHERE id_race = ? AND id_driver = ? AND session_type = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            statement.setInt(2, idDriver);
            statement.setString(3, "RACE");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch result by race and driver.", exception);
        }
    }

    public List<Result> findByRace(int idRace) {
        ensureSchema();
        String sql = "SELECT id_result, id_race, id_driver, id_constructor, session_type, finishing_position, points FROM results WHERE id_race = ? ORDER BY session_type, finishing_position";
        List<Result> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapRow(resultSet));
                }
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch results by race.", exception);
        }
    }

    public int createIfNotExists(Result result) {
        ensureSchema();
        validate(result);
        Optional<Result> existing = findByRaceAndDriverAndSession(result.getIdRace(), result.getIdDriver(), result.getSessionType());
        if (existing.isPresent()) {
            return existing.get().getIdResult();
        }
        return create(result);
    }

    public boolean delete(int idResult) {
        ensureSchema();
        String sql = "DELETE FROM results WHERE id_result = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idResult);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete result.", exception);
        }
    }

    public int deleteAll() {
        ensureSchema();
        String sql = "DELETE FROM results";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all results.", exception);
        }
    }

    public int deleteBySeason(int season) {
        ensureSchema();
        String sql = "DELETE r FROM results r INNER JOIN races ra ON ra.id_race = r.id_race WHERE ra.season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete results by season.", exception);
        }
    }

    public List<DriverStandingRow> findDriverStandingsBySeason(int season) {
        ensureSchema();
        String sql = "SELECT d.given_name, d.family_name, c.name AS constructor_name, "
            + "SUM(r.points) AS total_points, "
            + "SUM(CASE WHEN r.finishing_position = 1 THEN 1 ELSE 0 END) AS wins "
            + "FROM results r "
            + "INNER JOIN races ra ON ra.id_race = r.id_race "
            + "INNER JOIN drivers d ON d.id_driver = r.id_driver "
            + "INNER JOIN constructors c ON c.id_constructor = r.id_constructor "
            + "WHERE ra.season = ? "
            + "GROUP BY r.id_driver, d.given_name, d.family_name, c.name "
            + "ORDER BY total_points DESC, wins DESC, d.family_name ASC";

        List<DriverStandingRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                int pos = 1;
                while (resultSet.next()) {
                    DriverStandingRow row = new DriverStandingRow();
                    row.setPosition(pos++);
                    row.setDriverName(resultSet.getString("given_name") + " " + resultSet.getString("family_name"));
                    row.setConstructorName(resultSet.getString("constructor_name"));
                    row.setPoints(resultSet.getBigDecimal("total_points"));
                    row.setWins(resultSet.getInt("wins"));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver standings by season.", exception);
        }
    }

    public List<ConstructorStandingRow> findConstructorStandingsBySeason(int season) {
        ensureSchema();
        String sql = "SELECT c.name AS constructor_name, "
            + "SUM(r.points) AS total_points, "
            + "SUM(CASE WHEN r.finishing_position = 1 THEN 1 ELSE 0 END) AS wins "
            + "FROM results r "
            + "INNER JOIN races ra ON ra.id_race = r.id_race "
            + "INNER JOIN constructors c ON c.id_constructor = r.id_constructor "
            + "WHERE ra.season = ? "
            + "GROUP BY r.id_constructor, c.name "
            + "ORDER BY total_points DESC, wins DESC, c.name ASC";

        List<ConstructorStandingRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                int pos = 1;
                while (resultSet.next()) {
                    ConstructorStandingRow row = new ConstructorStandingRow();
                    row.setPosition(pos++);
                    row.setConstructorName(resultSet.getString("constructor_name"));
                    row.setPoints(resultSet.getBigDecimal("total_points"));
                    row.setWins(resultSet.getInt("wins"));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch constructor standings by season.", exception);
        }
    }

    private BigDecimal calculatePoints(int position, String sessionType) {
        if ("SPRINT".equalsIgnoreCase(normalizeSessionType(sessionType))) {
            switch (position) {
                case 1: return new BigDecimal("8.00");
                case 2: return new BigDecimal("7.00");
                case 3: return new BigDecimal("6.00");
                case 4: return new BigDecimal("5.00");
                case 5: return new BigDecimal("4.00");
                case 6: return new BigDecimal("3.00");
                case 7: return new BigDecimal("2.00");
                case 8: return new BigDecimal("1.00");
                default: return BigDecimal.ZERO;
            }
        }
        switch (position) {
            case 1:
                return new BigDecimal("25.00");
            case 2:
                return new BigDecimal("18.00");
            case 3:
                return new BigDecimal("15.00");
            case 4:
                return new BigDecimal("12.00");
            case 5:
                return new BigDecimal("10.00");
            case 6:
                return new BigDecimal("8.00");
            case 7:
                return new BigDecimal("6.00");
            case 8:
                return new BigDecimal("4.00");
            case 9:
                return new BigDecimal("2.00");
            case 10:
                return new BigDecimal("1.00");
            default:
                return BigDecimal.ZERO;
        }
    }

    private Result mapRow(ResultSet resultSet) throws SQLException {
        Result result = new Result();
        result.setIdResult(resultSet.getInt("id_result"));
        result.setIdRace(resultSet.getInt("id_race"));
        result.setIdDriver(resultSet.getInt("id_driver"));
        result.setIdConstructor(resultSet.getInt("id_constructor"));
        result.setSessionType(resultSet.getString("session_type"));
        result.setFinishingPosition(resultSet.getInt("finishing_position"));
        result.setPoints(resultSet.getBigDecimal("points"));
        return result;
    }

    private void validate(Result result) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null.");
        }
        if (result.getIdRace() == null || result.getIdRace() <= 0) {
            throw new IllegalArgumentException("Race id is required.");
        }
        if (result.getIdDriver() == null || result.getIdDriver() <= 0) {
            throw new IllegalArgumentException("Driver id is required.");
        }
        if (result.getIdConstructor() == null || result.getIdConstructor() <= 0) {
            throw new IllegalArgumentException("Constructor id is required.");
        }
        if (!"RACE".equalsIgnoreCase(normalizeSessionType(result.getSessionType()))
            && !"SPRINT".equalsIgnoreCase(normalizeSessionType(result.getSessionType()))) {
            throw new IllegalArgumentException("Session type must be RACE or SPRINT.");
        }
        if (result.getFinishingPosition() == null || result.getFinishingPosition() < 1 || result.getFinishingPosition() > 20) {
            throw new IllegalArgumentException("Finishing position must be between 1 and 20.");
        }
    }

    private void validateForUpdate(Result result) {
        validate(result);
        if (result.getIdResult() == null || result.getIdResult() <= 0) {
            throw new IllegalArgumentException("Result id is required for update.");
        }
    }

    private void ensureForeignKeysExist(Result result) {
        ensureExists("races", "id_race", result.getIdRace(), "Referenced race does not exist");
        ensureExists("drivers", "id_driver", result.getIdDriver(), "Referenced driver does not exist");
        ensureExists("constructors", "id_constructor", result.getIdConstructor(), "Referenced constructor does not exist");
    }

    private void ensureExists(String table, String idColumn, int idValue, String message) {
        String sql = "SELECT 1 FROM " + table + " WHERE " + idColumn + " = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(message + ": " + idColumn + "=" + idValue);
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to validate foreign key existence.", exception);
        }
    }

    private void ensureUniqueRaceDriver(int idRace, int idDriver, String sessionType, Integer idResultToExclude) {
        String sql = "SELECT id_result FROM results WHERE id_race = ? AND id_driver = ? AND session_type = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            statement.setInt(2, idDriver);
            statement.setString(3, normalizeSessionType(sessionType));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int existingId = resultSet.getInt("id_result");
                    if (idResultToExclude == null || existingId != idResultToExclude) {
                        throw new IllegalArgumentException("Duplicate result detected for race and driver.");
                    }
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to validate race-driver uniqueness.", exception);
        }
    }

    public Optional<Result> findByRaceAndDriverAndSession(int idRace, int idDriver, String sessionType) {
        ensureSchema();
        String sql = "SELECT id_result, id_race, id_driver, id_constructor, session_type, finishing_position, points FROM results WHERE id_race = ? AND id_driver = ? AND session_type = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            statement.setInt(2, idDriver);
            statement.setString(3, normalizeSessionType(sessionType));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch result by race, driver and session.", exception);
        }
    }

    private String normalizeSessionType(String sessionType) {
        if (sessionType == null || sessionType.isBlank()) {
            return "RACE";
        }
        return sessionType.trim().toUpperCase();
    }

    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }
        synchronized (ResultDAO.class) {
            if (schemaChecked) {
                return;
            }
            String sql = "SELECT id_result, id_race, id_driver, id_constructor, "
                + "session_type, finishing_position, points FROM results WHERE 1 = 0";
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet ignored = statement.executeQuery()) {
                // The restored schema is checked, never migrated during a query.
            } catch (SQLException exception) {
                throw new DataAccessException("La tabla results de BoxBox no está preparada.", exception);
            }
            schemaChecked = true;
        }
    }
}
