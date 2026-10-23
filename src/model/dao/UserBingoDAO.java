package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.db.DatabaseConnection;

public class UserBingoDAO {

    public UserBingoDAO() {
        ensureSchema();
    }

    public List<BingoEntry> loadByUser(int userId) {
        String sql = "SELECT slot_index, prediction_text, is_completed FROM user_bingo_predictions WHERE id_user = ? ORDER BY slot_index";
        List<BingoEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new BingoEntry(
                        resultSet.getInt("slot_index"),
                        resultSet.getString("prediction_text"),
                        resultSet.getBoolean("is_completed")
                    ));
                }
            }
            return entries;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to load bingo entries.", exception);
        }
    }

    public void upsert(int userId, int slotIndex, String predictionText, boolean completed) {
        String sql = "INSERT INTO user_bingo_predictions (id_user, slot_index, prediction_text, is_completed) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE prediction_text = VALUES(prediction_text), is_completed = VALUES(is_completed)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, slotIndex);
            statement.setString(3, predictionText);
            statement.setBoolean(4, completed);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save bingo entry.", exception);
        }
    }

    private void ensureSchema() {
        String sql = "SELECT id_user, slot_index, prediction_text, is_completed "
            + "FROM user_bingo_predictions WHERE 1 = 0";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet ignored = statement.executeQuery()) {
            // Validate the restored schema without CREATE/ALTER privileges.
        } catch (SQLException exception) {
            throw new DataAccessException("La tabla de bingo de BoxBox no está preparada.", exception);
        }
    }

    public static class BingoEntry {
        private final int slotIndex;
        private final String predictionText;
        private final boolean completed;

        public BingoEntry(int slotIndex, String predictionText, boolean completed) {
            this.slotIndex = slotIndex;
            this.predictionText = predictionText;
            this.completed = completed;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public String getPredictionText() {
            return predictionText;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
