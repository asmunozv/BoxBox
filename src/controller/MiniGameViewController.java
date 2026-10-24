package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import view.AppAudio;

public class MiniGameViewController extends BorderPane {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 560;
    private static final double CAR_WIDTH = 36;
    private static final double CAR_HEIGHT = 60;
    private static final double PLAYER_SPEED = 5.0;
    private static final double OBSTACLE_SPEED = 3.5;

    @FXML
    private Canvas canvas;

    @FXML
    private ComboBox<TeamColor> teamCombo;

    @FXML
    private Label statusLabel;

    private GraphicsContext gc;
    private final Random random;
    private final List<Obstacle> obstacles;
    private AnimationTimer timer;
    private MediaPlayer gameMusic;

    private double playerX;
    private double playerY;
    private boolean leftPressed;
    private boolean rightPressed;
    private long frames;
    private int score;
    private boolean running;

    public MiniGameViewController() {
        this.random = new Random();
        this.obstacles = new ArrayList<>();
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MiniGameView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load MiniGameView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        gc = canvas.getGraphicsContext2D();

        teamCombo.getItems().addAll(
            new TeamColor("Ferrari", Color.web("#dc0000")),
            new TeamColor("Mercedes", Color.web("#00d2be")),
            new TeamColor("McLaren", Color.web("#ff8700")),
            new TeamColor("Red Bull", Color.web("#1e41ff")),
            new TeamColor("Aston Martin", Color.web("#006f62"))
        );
        teamCombo.setValue(teamCombo.getItems().get(0));
        teamCombo.setFocusTraversable(false);

        setFocusTraversable(true);
        canvas.setFocusTraversable(true);
        canvas.setOnMouseClicked(event -> canvas.requestFocus());
        setOnKeyPressed(event -> handleKeyPressed(event.getCode()));
        setOnKeyReleased(event -> handleKeyReleased(event.getCode()));
        canvas.setOnKeyPressed(event -> handleKeyPressed(event.getCode()));
        canvas.setOnKeyReleased(event -> handleKeyReleased(event.getCode()));

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (!isVisible()) {
                        return;
                    }
                    if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT) {
                        handleKeyPressed(event.getCode());
                        event.consume();
                    }
                });
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                    if (!isVisible()) {
                        return;
                    }
                    if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT) {
                        handleKeyReleased(event.getCode());
                        event.consume();
                    }
                });
            }
        });

        drawInitial();
    }

    @FXML
    private void onStartGame() {
        startGame();
    }

    private void handleKeyPressed(KeyCode keyCode) {
        if (keyCode == KeyCode.LEFT) {
            leftPressed = true;
        } else if (keyCode == KeyCode.RIGHT) {
            rightPressed = true;
        }
    }

    private void handleKeyReleased(KeyCode keyCode) {
        if (keyCode == KeyCode.LEFT) {
            leftPressed = false;
        } else if (keyCode == KeyCode.RIGHT) {
            rightPressed = false;
        }
    }

    private void startGame() {
        requestFocus();
        canvas.requestFocus();
        playerX = (WIDTH - CAR_WIDTH) / 2.0;
        playerY = HEIGHT - CAR_HEIGHT - 20;
        leftPressed = false;
        rightPressed = false;
        frames = 0;
        score = 0;
        running = true;
        obstacles.clear();
        statusLabel.setText("Use LEFT/RIGHT arrows. Avoid crashes!");

        if (timer != null) {
            timer.stop();
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        timer.start();
    }

    private void update() {
        if (!running) {
            return;
        }

        frames++;

        if (leftPressed) {
            playerX -= PLAYER_SPEED;
        }
        if (rightPressed) {
            playerX += PLAYER_SPEED;
        }

        if (playerX < 120) {
            playerX = 120;
        }
        if (playerX > WIDTH - 120 - CAR_WIDTH) {
            playerX = WIDTH - 120 - CAR_WIDTH;
        }

        if (frames % 40 == 0) {
            spawnObstacle();
        }

        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.y += OBSTACLE_SPEED;

            if (intersects(playerX, playerY, CAR_WIDTH, CAR_HEIGHT, obstacle.x, obstacle.y, CAR_WIDTH, CAR_HEIGHT)) {
                running = false;
                timer.stop();
                statusLabel.setText("Crashed! Final score: " + score + " - Press Start to retry.");
            }

            if (obstacle.y > HEIGHT) {
                iterator.remove();
                score += 10;
            }
        }
    }

    private void render() {
        gc.setFill(Color.web("#0f0f0f"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#2b2b2b"));
        gc.fillRect(110, 0, WIDTH - 220, HEIGHT);

        gc.setStroke(Color.web("#d9d9d9"));
        gc.setLineWidth(4);
        gc.strokeLine(110, 0, 110, HEIGHT);
        gc.strokeLine(WIDTH - 110, 0, WIDTH - 110, HEIGHT);

        gc.setStroke(Color.web("#7f7f7f"));
        gc.setLineWidth(2);
        for (int y = -40; y < HEIGHT; y += 40) {
            int offset = (int) (frames % 40);
            gc.strokeLine(WIDTH / 2.0, y + offset, WIDTH / 2.0, y + 18 + offset);
        }

        TeamColor selectedTeam = teamCombo.getValue() == null ? new TeamColor("Ferrari", Color.web("#dc0000")) : teamCombo.getValue();
        drawCar(playerX, playerY, selectedTeam.color, Color.WHITE);

        for (Obstacle obstacle : obstacles) {
            drawCar(obstacle.x, obstacle.y, Color.web("#ff4d4d"), Color.web("#ffe8e8"));
        }

        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + score, 20, 24);
    }

    private void drawInitial() {
        render();
    }

    public void onTabSelected() {
        if(timer!=null&&running)timer.start();
        if (gameMusic == null) {
            gameMusic = AppAudio.playLoop(
                "assets/audio/game/game.mp3",
                "assets/audio/game/game",
                "src/assets/audio/game/game.mp3",
                "src/assets/audio/game/game"
            );
        }
    }

    public void onTabUnselected() {
        if(timer!=null)timer.stop();
        AppAudio.stop(gameMusic);
        gameMusic = null;
    }

    private void spawnObstacle() {
        double minX = 120;
        double maxX = WIDTH - 120 - CAR_WIDTH;
        double x = minX + random.nextDouble() * (maxX - minX);
        obstacles.add(new Obstacle(x, -CAR_HEIGHT));
    }

    private boolean intersects(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void drawCar(double x, double y, Color bodyColor, Color accentColor) {
        gc.setFill(bodyColor);
        gc.fillRoundRect(x, y, CAR_WIDTH, CAR_HEIGHT, 10, 10);

        gc.setFill(accentColor);
        gc.fillRoundRect(x + 8, y + 8, CAR_WIDTH - 16, CAR_HEIGHT - 28, 6, 6);

        gc.setFill(Color.BLACK);
        gc.fillOval(x - 4, y + 10, 8, 16);
        gc.fillOval(x + CAR_WIDTH - 4, y + 10, 8, 16);
        gc.fillOval(x - 4, y + CAR_HEIGHT - 26, 8, 16);
        gc.fillOval(x + CAR_WIDTH - 4, y + CAR_HEIGHT - 26, 8, 16);
    }

    private static class Obstacle {
        private final double x;
        private double y;

        private Obstacle(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class TeamColor {
        private final String name;
        private final Color color;

        private TeamColor(String name, Color color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
