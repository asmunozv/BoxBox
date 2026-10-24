package controller;

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
import view.DisposableView;
import javafx.concurrent.Task;

public class LoginViewController extends BorderPane implements DisposableView {

    private AuthController authController;
    private final Runnable onLoginSuccess;
    private MediaPlayer loginMusic;
    private MediaPlayer backgroundPlayer;
    private Task<Boolean> activeTask;

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

        Runnable loginAction = () -> authenticate(false);
        Runnable registerAction = () -> authenticate(true);

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
        var file = model.util.LocalResources.find("assets/video/login.mp4","src/assets/video/login.mp4");
        if(file.isPresent()) {
            try {
                Media media = new Media(file.get().toUri().toString());
                MediaPlayer player = new MediaPlayer(media);
                backgroundPlayer = player;
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

    private void authenticate(boolean register){
        if(activeTask!=null&&activeTask.isRunning())return;
        String username=usernameField.getText(),password=passwordField.getText();
        loginButton.setDisable(true);registerButton.setDisable(true);statusLabel.setText(register?"Creando acceso protegido…":"Verificando acceso…");
        activeTask=new Task<>(){protected Boolean call(){if(authController==null)authController=new AuthController();return register?authController.register(username,password):authController.authenticate(username,password);}};
        activeTask.setOnSucceeded(e->{loginButton.setDisable(false);registerButton.setDisable(false);if(activeTask.getValue()){passwordField.clear();dispose();onLoginSuccess.run();}else statusLabel.setText(register?"Usuario no disponible. Usa hasta 80 caracteres y una contraseña de 8 a 1024 caracteres.":"Credenciales no válidas.");});
        activeTask.setOnFailed(e->{loginButton.setDisable(false);registerButton.setDisable(false);statusLabel.setText("No se pudo verificar el acceso. Revisa MySQL y la configuración privada de BoxBox.");});
        Thread thread=new Thread(activeTask,"boxbox-authentication");thread.setDaemon(true);thread.start();
    }

    @Override public void dispose(){AppAudio.stop(loginMusic);loginMusic=null;if(backgroundPlayer!=null){backgroundPlayer.stop();backgroundPlayer.dispose();backgroundPlayer=null;}if(activeTask!=null&&activeTask.isRunning())activeTask.cancel(true);}
}
