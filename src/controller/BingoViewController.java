package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import model.dao.UserBingoDAO;

public class BingoViewController extends BorderPane {

    @FXML
    private GridPane predictionGrid;
    @FXML
    private Label progressLabel;
    @FXML
    private StackPane confettiLayer;
    @FXML
    private Label subtitleLabel;

    private final List<TextField> predictionFields;
    private final List<CheckBox> predictionChecks;
    private final Random random;
    private final UserBingoDAO userBingoDAO;
    private AnimationTimer confettiTimer;
    private Canvas confettiCanvas;
    private final List<Particle> particles;
    private boolean loadingState;

    public BingoViewController() {
        this.predictionFields = new ArrayList<>();
        this.predictionChecks = new ArrayList<>();
        this.random = new Random();
        this.userBingoDAO = new UserBingoDAO();
        this.particles = new ArrayList<>();
        this.loadingState = false;
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BingoView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load BingoView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        applyBackgroundImage("assets/images/fondo/fondodrivers.png", "src/assets/images/fondo/fondodrivers.png");
        setPadding(new Insets(12));
        confettiLayer.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 12;");
        subtitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 29px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0.4, 0, 1);");
        progressLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.45); -fx-padding: 4 8 4 8; -fx-background-radius: 6;");
        buildPredictionGrid();
        setupConfettiCanvas();
        loadUserBingo();
        updateProgress();
    }

    @FXML
    private void onReset() {
        for (TextField field : predictionFields) {
            field.clear();
        }
        for (CheckBox checkBox : predictionChecks) {
            checkBox.setSelected(false);
        }
        for (int i = 0; i < predictionFields.size(); i++) {
            saveSlot(i);
        }
        stopConfetti();
        updateProgress();
    }

    private void buildPredictionGrid() {
        predictionGrid.getChildren().clear();
        predictionFields.clear();
        predictionChecks.clear();

        for (int index = 0; index < 9; index++) {
            int predictionNumber = index + 1;

            Label title = new Label("Prediction " + predictionNumber);
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

            TextField textField = new TextField();
            textField.setPromptText("Write your prediction...");
            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                updateProgress();
                saveSlot(predictionNumber - 1);
            });

            CheckBox checkBox = new CheckBox("Completed");
            checkBox.setStyle("-fx-text-fill: white;");
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
                updateProgress();
                saveSlot(predictionNumber - 1);
            });

            VBox card = new VBox(8, title, textField, checkBox);
            card.setPadding(new Insets(10));
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2a3142, #131722);"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: #44506a;"
                    + "-fx-border-radius: 10;"
            );

            predictionFields.add(textField);
            predictionChecks.add(checkBox);

            predictionGrid.add(card, index % 3, index / 3);
        }
    }

    private void updateProgress() {
        int completed = 0;
        for (CheckBox checkBox : predictionChecks) {
            if (checkBox.isSelected()) {
                completed++;
            }
        }

        int filledPredictions = 0;
        for (TextField field : predictionFields) {
            if (field.getText() != null && !field.getText().trim().isEmpty()) {
                filledPredictions++;
            }
        }

        progressLabel.setText("Completed: " + completed + "/9  |  Filled predictions: " + filledPredictions + "/9");

        if (completed == 9 && filledPredictions == 9) {
            startConfetti();
            progressLabel.setText("BINGO COMPLETE! All 9 predictions are done!");
        } else {
            stopConfetti();
        }
    }

    private void startConfetti() {
        if (confettiTimer != null) {
            return;
        }

        particles.clear();
        seedParticles();

        confettiTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderConfetti();
            }
        };
        confettiTimer.start();
    }

    private void stopConfetti() {
        if (confettiTimer != null) {
            confettiTimer.stop();
            confettiTimer = null;
        }
        particles.clear();
        if (confettiCanvas != null) {
            GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());
        }
    }

    private void renderConfetti() {
        if (confettiCanvas == null) {
            return;
        }

        GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
        double width = confettiCanvas.getWidth();
        double height = confettiCanvas.getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }
        if (particles.isEmpty()) {
            seedParticles();
        }
        gc.clearRect(0, 0, width, height);

        for (Particle particle : particles) {
            particle.y += particle.speed;
            particle.x += particle.drift;
            particle.rotation += particle.rotationSpeed;

            if (particle.y > height + 20) {
                particle.y = -20;
                particle.x = random.nextDouble() * width;
            }

            gc.save();
            gc.translate(particle.x, particle.y);
            gc.rotate(particle.rotation);
            gc.setFill(particle.color);
            gc.fillRect(-particle.size / 2.0, -particle.size / 2.0, particle.size, particle.size);
            gc.restore();
        }
    }

    private Particle randomParticle(double width, double height) {
        Color[] palette = {
            Color.web("#f94144"),
            Color.web("#f3722c"),
            Color.web("#f9c74f"),
            Color.web("#43aa8b"),
            Color.web("#577590"),
            Color.web("#ffffff")
        };

        Particle particle = new Particle();
        particle.x = random.nextDouble() * Math.max(1.0, width);
        particle.y = random.nextDouble() * Math.max(1.0, height);
        particle.size = 5 + random.nextDouble() * 6;
        particle.speed = 1.2 + random.nextDouble() * 2.8;
        particle.drift = -0.8 + random.nextDouble() * 1.6;
        particle.rotation = random.nextDouble() * 360;
        particle.rotationSpeed = -4 + random.nextDouble() * 8;
        particle.color = palette[random.nextInt(palette.length)];
        return particle;
    }

    private void setupConfettiCanvas() {
        confettiCanvas = new Canvas();
        confettiCanvas.widthProperty().bind(confettiLayer.widthProperty());
        confettiCanvas.heightProperty().bind(confettiLayer.heightProperty());
        confettiCanvas.setMouseTransparent(true);
        confettiLayer.getChildren().add(confettiCanvas);
    }

    private void seedParticles() {
        if (confettiCanvas == null) {
            return;
        }
        double width = confettiCanvas.getWidth();
        double height = confettiCanvas.getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }
        particles.clear();
        for (int i = 0; i < 220; i++) {
            particles.add(randomParticle(width, height));
        }
    }

    private void applyBackgroundImage(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (!file.exists()) {
                continue;
            }
            Image image = new Image(file.toURI().toString(), true);
            BackgroundSize size = new BackgroundSize(100, 100, true, true, true, true);
            setBackground(new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size
            )));
            return;
        }
    }

    private void loadUserBingo() {
        Integer userId = UserSession.getCurrentUserId();
        if (userId == null) {
            return;
        }

        loadingState = true;
        try {
            List<UserBingoDAO.BingoEntry> entries = userBingoDAO.loadByUser(userId);
            for (UserBingoDAO.BingoEntry entry : entries) {
                int idx = entry.getSlotIndex();
                if (idx < 0 || idx >= predictionFields.size()) {
                    continue;
                }
                predictionFields.get(idx).setText(entry.getPredictionText() == null ? "" : entry.getPredictionText());
                predictionChecks.get(idx).setSelected(entry.isCompleted());
            }
        } finally {
            loadingState = false;
        }
    }

    private void saveSlot(int index) {
        if (loadingState) {
            return;
        }
        Integer userId = UserSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        if (index < 0 || index >= predictionFields.size()) {
            return;
        }

        String prediction = predictionFields.get(index).getText();
        boolean completed = predictionChecks.get(index).isSelected();
        userBingoDAO.upsert(userId, index, prediction, completed);
    }

    private static class Particle {
        private double x;
        private double y;
        private double size;
        private double speed;
        private double drift;
        private double rotation;
        private double rotationSpeed;
        private Color color;
    }
}
