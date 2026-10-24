package view;

import javafx.scene.Scene;

public final class SceneTheme {
    private SceneTheme(){}
    public static Scene apply(Scene scene){String css=SceneTheme.class.getResource("/application/application.css").toExternalForm();if(!scene.getStylesheets().contains(css))scene.getStylesheets().add(css);return scene;}
}
