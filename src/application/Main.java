package application;

import controller.LoginViewController;
import controller.MainViewController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        try {
            scene = new Scene(new StackPane(), 1280, 800);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            showLogin();

            primaryStage.setScene(scene);
            primaryStage.setTitle("BoxBox · Racing understood");
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.show();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void showLogin() {
        if(scene.getRoot() instanceof view.DisposableView disposable)disposable.dispose();
        scene.setRoot(new LoginViewController(this::showMain));
    }

    private void showMain() {
        if(scene.getRoot() instanceof view.DisposableView disposable)disposable.dispose();
        scene.setRoot(new MainViewController(this::showLogin));
    }

    public static void main(String[] args) {
        launch(args);
    }
    @Override public void stop(){if(scene!=null&&scene.getRoot() instanceof view.DisposableView disposable)disposable.dispose();}
}
