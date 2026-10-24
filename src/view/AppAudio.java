package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import model.util.LocalResources;

public final class AppAudio {

    private AppAudio() {
    }

    public static MediaPlayer playLoop(String... candidatePaths) {
        var file = LocalResources.find(candidatePaths);
        if (file.isEmpty()) {
            return null;
        }

        try {
            Media media = new Media(file.get().toUri().toString());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.play();
            return player;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static void stop(MediaPlayer player) {
        if (player != null) {
            player.stop();
            player.dispose();
        }
    }

}
