package view;

import java.io.File;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public final class AppAudio {

    private AppAudio() {
    }

    public static MediaPlayer playLoop(String... candidatePaths) {
        stop(null);
        File file = resolveFile(candidatePaths);
        if (file == null) {
            return null;
        }

        try {
            Media media = new Media(file.toURI().toString());
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

    private static File resolveFile(String... paths) {
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
}
