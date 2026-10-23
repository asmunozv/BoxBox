package model.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.db.DatabaseConnection;
import model.entity.Driver;
import model.entity.DriverSeasonStats;

public class DriverDAO {

    public int create(Driver driver) {
        validateDriver(driver);

        String sql = "INSERT INTO drivers "
            + "(ergast_driver_id, permanent_number, code, given_name, family_name, date_of_birth, nationality, audio_file_path) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            mapDriverForInsertOrUpdate(statement, driver);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    driver.setIdDriver(id);
                    return id;
                }
            }
            throw new SQLException("Failed to create driver: no generated key returned.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create driver.", exception);
        }
    }

    public Optional<Driver> findById(int idDriver) {
        String sql = "SELECT id_driver, ergast_driver_id, permanent_number, code, given_name, family_name, date_of_birth, nationality, audio_file_path "
            + "FROM drivers WHERE id_driver = ?";

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
            throw new DataAccessException("Failed to fetch driver by id.", exception);
        }
    }

    public Optional<Driver> findByErgastDriverId(String ergastDriverId) {
        String sql = "SELECT id_driver, ergast_driver_id, permanent_number, code, given_name, family_name, date_of_birth, nationality, audio_file_path "
            + "FROM drivers WHERE ergast_driver_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, ergastDriverId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver by Ergast id.", exception);
        }
    }

    public Optional<Driver> findByCode(String code) {
        if (isBlank(code)) {
            return Optional.empty();
        }

        String sql = "SELECT id_driver, ergast_driver_id, permanent_number, code, given_name, family_name, date_of_birth, nationality, audio_file_path "
            + "FROM drivers WHERE code = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, code);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver by code.", exception);
        }
    }

    public List<Driver> findAll() {
        String sql = "SELECT id_driver, ergast_driver_id, permanent_number, code, given_name, family_name, date_of_birth, nationality, audio_file_path "
            + "FROM drivers ORDER BY family_name, given_name";

        List<Driver> drivers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                drivers.add(mapRow(resultSet));
            }
            return drivers;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch drivers.", exception);
        }
    }

    public boolean update(Driver driver) {
        validateDriverForUpdate(driver);

        String sql = "UPDATE drivers SET "
            + "ergast_driver_id = ?, permanent_number = ?, code = ?, given_name = ?, family_name = ?, "
            + "date_of_birth = ?, nationality = ?, audio_file_path = ? "
            + "WHERE id_driver = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            mapDriverForInsertOrUpdate(statement, driver);
            statement.setInt(9, driver.getIdDriver());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update driver.", exception);
        }
    }

    public boolean delete(int idDriver) {
        String sql = "DELETE FROM drivers WHERE id_driver = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idDriver);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete driver.", exception);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM drivers";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all drivers.", exception);
        }
    }

    public int deleteByErgastDriverId(String ergastDriverId) {
        String sql = "DELETE FROM drivers WHERE ergast_driver_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ergastDriverId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete driver by Ergast id.", exception);
        }
    }

    public int deleteDriversWithoutResultsInSeason(int season) {
        String sql = "DELETE FROM drivers WHERE id_driver NOT IN ("
            + "SELECT DISTINCT r.id_driver FROM results r "
            + "INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE ra.season = ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete drivers not used in season.", exception);
        }
    }

    public int createIfNotExists(Driver driver) {
        validateDriver(driver);

        Optional<Driver> existingDriver = findByErgastDriverId(driver.getErgastDriverId());
        if (existingDriver.isPresent()) {
            return existingDriver.get().getIdDriver();
        }
        return create(driver);
    }

    public DriverSeasonStats findSeasonStats(int idDriver, int season) {
        String sql = "SELECT COUNT(*) AS races, "
            + "COALESCE(SUM(CASE WHEN r.finishing_position = 1 THEN 1 ELSE 0 END), 0) AS wins, "
            + "COALESCE(SUM(CASE WHEN r.finishing_position <= 3 THEN 1 ELSE 0 END), 0) AS podiums, "
            + "COALESCE(SUM(r.points), 0) AS points "
            + "FROM results r INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE r.id_driver = ? AND ra.season = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idDriver);
            statement.setInt(2, season);
            try (ResultSet resultSet = statement.executeQuery()) {
                DriverSeasonStats stats = new DriverSeasonStats();
                if (resultSet.next()) {
                    stats.setRaces(resultSet.getInt("races"));
                    stats.setWins(resultSet.getInt("wins"));
                    stats.setPodiums(resultSet.getInt("podiums"));
                    stats.setPoints(resultSet.getBigDecimal("points"));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver season stats.", exception);
        }
    }

    public Optional<Integer> findDebutSeason(int idDriver) {
        String sql = "SELECT MIN(ra.season) AS debut_season "
            + "FROM results r INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE r.id_driver = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idDriver);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int value = resultSet.getInt("debut_season");
                    if (!resultSet.wasNull()) {
                        return Optional.of(value);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch driver debut season.", exception);
        }
    }

    private void mapDriverForInsertOrUpdate(PreparedStatement statement, Driver driver) throws SQLException {
        statement.setString(1, driver.getErgastDriverId());
        if (driver.getPermanentNumber() == null) {
            statement.setNull(2, java.sql.Types.SMALLINT);
        } else {
            statement.setInt(2, driver.getPermanentNumber());
        }
        statement.setString(3, driver.getCode());
        statement.setString(4, driver.getGivenName());
        statement.setString(5, driver.getFamilyName());
        if (driver.getDateOfBirth() == null) {
            statement.setNull(6, java.sql.Types.DATE);
        } else {
            statement.setDate(6, Date.valueOf(driver.getDateOfBirth()));
        }
        statement.setString(7, driver.getNationality());
        statement.setString(8, driver.getAudioFilePath());
    }

    private Driver mapRow(ResultSet resultSet) throws SQLException {
        Driver driver = new Driver();
        driver.setIdDriver(resultSet.getInt("id_driver"));
        driver.setErgastDriverId(resultSet.getString("ergast_driver_id"));

        int permanentNumber = resultSet.getInt("permanent_number");
        if (resultSet.wasNull()) {
            driver.setPermanentNumber(null);
        } else {
            driver.setPermanentNumber(permanentNumber);
        }

        driver.setCode(resultSet.getString("code"));
        driver.setGivenName(resultSet.getString("given_name"));
        driver.setFamilyName(resultSet.getString("family_name"));

        Date dateOfBirth = resultSet.getDate("date_of_birth");
        if (dateOfBirth != null) {
            driver.setDateOfBirth(dateOfBirth.toLocalDate());
        }

        driver.setNationality(resultSet.getString("nationality"));
        driver.setAudioFilePath(resultSet.getString("audio_file_path"));
        return driver;
    }

    private void validateDriver(Driver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver cannot be null.");
        }
        if (isBlank(driver.getErgastDriverId())) {
            throw new IllegalArgumentException("Ergast driver id is required.");
        }
        if (isBlank(driver.getGivenName())) {
            throw new IllegalArgumentException("Given name is required.");
        }
        if (isBlank(driver.getFamilyName())) {
            throw new IllegalArgumentException("Family name is required.");
        }
    }

    private void validateDriverForUpdate(Driver driver) {
        validateDriver(driver);
        if (driver.getIdDriver() == null || driver.getIdDriver() <= 0) {
            throw new IllegalArgumentException("Driver id is required for update.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public int deleteDriversNotInSeasonResults(int season) {
        String sql = "DELETE FROM drivers WHERE id_driver NOT IN ("
            + "SELECT DISTINCT r.id_driver FROM results r "
            + "INNER JOIN races ra ON ra.id_race = r.id_race "
            + "WHERE ra.season = ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete non-current drivers.", exception);
        }
    }
}
