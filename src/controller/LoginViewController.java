package controller;

import java.io.File;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import view.AppAudio;

public class LoginViewController extends BorderPane {

    private final AuthController authController;
    private final Runnable onLoginSuccess;
    private MediaPlayer loginMusic;

    @FXML
    private StackPane centerLayer;

    @FXML
    private VBox loginCard;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label statusLabel;

    public LoginViewController(Runnable onLoginSuccess) {
        this.authController = new AuthController();
        this.onLoginSuccess = onLoginSuccess;
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load LoginView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        getStyleClass().add("login-view");

        loginMusic = AppAudio.playLoop(
            "assets/audio/log_in/log.mp3",
            "src/assets/audio/log_in/log.mp3"
        );
        loginButton.setDefaultButton(true);

        Runnable loginAction = () -> {
            boolean authenticated = authController.authenticate(usernameField.getText(), passwordField.getText());
            if (authenticated) {
                statusLabel.setText("");
                AppAudio.stop(loginMusic);
                loginMusic = null;
                onLoginSuccess.run();
            } else {
                statusLabel.setText("Invalid credentials. Try again.");
            }
        };

        Runnable registerAction = () -> {
            boolean created = authController.register(usernameField.getText(), passwordField.getText());
            if (created) {
                statusLabel.setText("");
                AppAudio.stop(loginMusic);
                loginMusic = null;
                onLoginSuccess.run();
            } else {
                statusLabel.setText("Could not create account (username may exist). ");
            }
        };

        loginButton.setOnAction(event -> loginAction.run());
        registerButton.setOnAction(event -> registerAction.run());
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                loginAction.run();
            }
        });

        MediaView backgroundVideo = createBackgroundVideo();
        if (backgroundVideo != null) {
            backgroundVideo.fitWidthProperty().bind(widthProperty());
            backgroundVideo.fitHeightProperty().bind(heightProperty());
            backgroundVideo.setPreserveRatio(false);
            centerLayer.getChildren().add(0, backgroundVideo);
        }

        BorderPane.setMargin(centerLayer, new Insets(0));
    }

    private MediaView createBackgroundVideo() {
        File[] candidates = {
            new File("assets/video/login.mp4"),
            new File("src/assets/video/login.mp4")
        };

        for (File file : candidates) {
            if (!file.exists()) {
                continue;
            }
            try {
                Media media = new Media(file.toURI().toString());
                MediaPlayer player = new MediaPlayer(media);
                player.setMute(true);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.play();
                MediaView mediaView = new MediaView(player);
                mediaView.setMouseTransparent(true);
                return mediaView;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }
}
