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
import model.entity.Circuit;

public class CircuitDAO {

    public int create(Circuit circuit) {
        validate(circuit);
        String sql = "INSERT INTO circuits (ergast_circuit_id, name, country, locality, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, circuit.getErgastCircuitId());
            statement.setString(2, circuit.getName());
            statement.setString(3, circuit.getCountry());
            statement.setString(4, circuit.getLocality());
            statement.setBigDecimal(5, circuit.getLatitude());
            statement.setBigDecimal(6, circuit.getLongitude());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    circuit.setIdCircuit(id);
                    return id;
                }
            }
            throw new SQLException("No key returned when creating circuit.");
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create circuit.", exception);
        }
    }

    public Optional<Circuit> findById(int idCircuit) {
        String sql = "SELECT id_circuit, ergast_circuit_id, name, country, locality, latitude, longitude FROM circuits WHERE id_circuit = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCircuit);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch circuit by id.", exception);
        }
    }

    public Optional<Circuit> findByErgastCircuitId(String ergastCircuitId) {
        String sql = "SELECT id_circuit, ergast_circuit_id, name, country, locality, latitude, longitude FROM circuits WHERE ergast_circuit_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ergastCircuitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch circuit by Ergast id.", exception);
        }
    }

    public List<Circuit> findAll() {
        String sql = "SELECT id_circuit, ergast_circuit_id, name, country, locality, latitude, longitude FROM circuits ORDER BY name";
        List<Circuit> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapRow(resultSet));
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch circuits.", exception);
        }
    }

    public boolean update(Circuit circuit) {
        validateForUpdate(circuit);
        String sql = "UPDATE circuits SET ergast_circuit_id = ?, name = ?, country = ?, locality = ?, latitude = ?, longitude = ? WHERE id_circuit = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, circuit.getErgastCircuitId());
            statement.setString(2, circuit.getName());
            statement.setString(3, circuit.getCountry());
            statement.setString(4, circuit.getLocality());
            statement.setBigDecimal(5, circuit.getLatitude());
            statement.setBigDecimal(6, circuit.getLongitude());
            statement.setInt(7, circuit.getIdCircuit());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update circuit.", exception);
        }
    }

    public boolean delete(int idCircuit) {
        String sql = "DELETE FROM circuits WHERE id_circuit = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCircuit);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete circuit.", exception);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM circuits";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete all circuits.", exception);
        }
    }

    public int deleteCircuitsWithoutRacesInSeason(int season) {
        String sql = "DELETE FROM circuits WHERE id_circuit NOT IN ("
            + "SELECT DISTINCT id_circuit FROM races WHERE season = ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete circuits not used in season.", exception);
        }
    }

    public int createIfNotExists(Circuit circuit) {
        validate(circuit);
        Optional<Circuit> existing = findByErgastCircuitId(circuit.getErgastCircuitId());
        if (existing.isPresent()) {
            return existing.get().getIdCircuit();
        }
        return create(circuit);
    }

    private Circuit mapRow(ResultSet resultSet) throws SQLException {
        Circuit circuit = new Circuit();
        circuit.setIdCircuit(resultSet.getInt("id_circuit"));
        circuit.setErgastCircuitId(resultSet.getString("ergast_circuit_id"));
        circuit.setName(resultSet.getString("name"));
        circuit.setCountry(resultSet.getString("country"));
        circuit.setLocality(resultSet.getString("locality"));
        circuit.setLatitude(resultSet.getBigDecimal("latitude"));
        circuit.setLongitude(resultSet.getBigDecimal("longitude"));
        return circuit;
    }

    private void validate(Circuit circuit) {
        if (circuit == null) {
            throw new IllegalArgumentException("Circuit cannot be null.");
        }
        if (isBlank(circuit.getErgastCircuitId())) {
            throw new IllegalArgumentException("Ergast circuit id is required.");
        }
        if (isBlank(circuit.getName())) {
            throw new IllegalArgumentException("Circuit name is required.");
        }
    }

    private void validateForUpdate(Circuit circuit) {
        validate(circuit);
        if (circuit.getIdCircuit() == null || circuit.getIdCircuit() <= 0) {
            throw new IllegalArgumentException("Circuit id is required for update.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
