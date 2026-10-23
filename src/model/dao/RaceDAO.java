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
import model.entity.Race;

public class RaceDAO {

    public int create(Race race) {
        validate(race);
        ensureCircuitExists(race.getIdCircuit());
        String sql = "INSERT INTO races (season, round_number, name, race_date, id_circuit) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, race.getSeason());
            statement.setInt(2, race.getRoundNumber());
            statement.setString(3, race.getName());
            statement.setDate(4, Date.valueOf(race.getRaceDate()));
            statement.setInt(5, race.getIdCircuit());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    race.setIdRace(id);
                    return id;
                }
            }
            throw new SQLException("No key returned when creating race.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create race.", exception);
        }
    }

    public Optional<Race> findById(int idRace) {
        String sql = "SELECT id_race, season, round_number, name, race_date, id_circuit FROM races WHERE id_race = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch race by id.", exception);
        }
    }

    public List<Race> findAll() {
        String sql = "SELECT id_race, season, round_number, name, race_date, id_circuit FROM races ORDER BY season DESC, round_number";
        List<Race> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapRow(resultSet));
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch races.", exception);
        }
    }

    public List<Race> findBySeason(int season) {
        String sql = "SELECT id_race, season, round_number, name, race_date, id_circuit FROM races WHERE season = ? ORDER BY round_number";
        List<Race> items = new ArrayList<>();
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
            throw new DataAccessException("Failed to fetch races by season.", exception);
        }
    }

    public Optional<Race> findBySeasonAndRound(int season, int roundNumber) {
        String sql = "SELECT id_race, season, round_number, name, race_date, id_circuit FROM races WHERE season = ? AND round_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            statement.setInt(2, roundNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch race by season and round.", exception);
        }
    }

    public boolean update(Race race) {
        validateForUpdate(race);
        ensureCircuitExists(race.getIdCircuit());
        String sql = "UPDATE races SET season = ?, round_number = ?, name = ?, race_date = ?, id_circuit = ? WHERE id_race = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, race.getSeason());
            statement.setInt(2, race.getRoundNumber());
            statement.setString(3, race.getName());
            statement.setDate(4, Date.valueOf(race.getRaceDate()));
            statement.setInt(5, race.getIdCircuit());
            statement.setInt(6, race.getIdRace());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update race.", exception);
        }
    }

    public boolean delete(int idRace) {
        String sql = "DELETE FROM races WHERE id_race = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRace);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete race.", exception);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM races";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all races.", exception);
        }
    }

    public int deleteBySeason(int season) {
        String sql = "DELETE FROM races WHERE season = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete races by season.", exception);
        }
    }

    public int createIfNotExists(Race race) {
        validate(race);
        Optional<Race> existing = findBySeasonAndRound(race.getSeason(), race.getRoundNumber());
        if (existing.isPresent()) {
            return existing.get().getIdRace();
        }
        return create(race);
    }

    private Race mapRow(ResultSet resultSet) throws SQLException {
        Race race = new Race();
        race.setIdRace(resultSet.getInt("id_race"));
        race.setSeason(resultSet.getInt("season"));
        race.setRoundNumber(resultSet.getInt("round_number"));
        race.setName(resultSet.getString("name"));
        race.setRaceDate(resultSet.getDate("race_date").toLocalDate());
        race.setIdCircuit(resultSet.getInt("id_circuit"));
        return race;
    }

    private void validate(Race race) {
        if (race == null) {
            throw new IllegalArgumentException("Race cannot be null.");
        }
        if (race.getSeason() == null || race.getSeason() < 1950) {
            throw new IllegalArgumentException("Valid season is required.");
        }
        if (race.getRoundNumber() == null || race.getRoundNumber() < 1 || race.getRoundNumber() > 30) {
            throw new IllegalArgumentException("Round number must be between 1 and 30.");
        }
        if (race.getName() == null || race.getName().isBlank()) {
            throw new IllegalArgumentException("Race name is required.");
        }
        if (race.getRaceDate() == null) {
            throw new IllegalArgumentException("Race date is required.");
        }
        if (race.getIdCircuit() == null || race.getIdCircuit() <= 0) {
            throw new IllegalArgumentException("Circuit id is required.");
        }
    }

    private void validateForUpdate(Race race) {
        validate(race);
        if (race.getIdRace() == null || race.getIdRace() <= 0) {
            throw new IllegalArgumentException("Race id is required for update.");
        }
    }

    private void ensureCircuitExists(int idCircuit) {
        String sql = "SELECT 1 FROM circuits WHERE id_circuit = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCircuit);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Referenced circuit does not exist: id_circuit=" + idCircuit);
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to validate circuit foreign key.", exception);
        }
    }
}
