package controller;

import java.io.IOException;
import java.nio.file.Path;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.media.*;
import model.history.RaceData.Entry;
import model.util.LocalResources;
import view.LocalImageService;

/** The local multimedia collection alongside the selected historical result. */
public final class DriverMediaCard extends HBox implements view.DisposableView {
    @FXML private ImageView photo;
    @FXML private Label nameLabel,detailLabel,mediaStatus;
    @FXML private Button playButton,stopButton;
    private Path audio;
    private MediaPlayer player;

    public DriverMediaCard() {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/view/DriverMediaCard.fxml"));
        loader.setRoot(this);loader.setController(this);
        try{loader.load();}catch(IOException e){throw new IllegalStateException("No se pudo cargar la ficha multimedia.",e);}
        playButton.setOnAction(e->play());stopButton.setOnAction(e->dispose());
        sceneProperty().addListener((o,a,b)->{if(b==null)dispose();});
        setEntry(null);
    }

    public void setEntry(Entry entry) {
        dispose();
        String image=entry==null?null:LocalImageService.resolveDriverImage(entry.driverId(),entry.driver());
        photo.setImage(LocalImageService.load(image));
        nameLabel.setText(entry==null?"Selecciona un piloto en la tabla":entry.driver());
        detailLabel.setText(entry==null?"Aquí verás su foto y podrás escuchar tu audio local.":entry.team()+" · "+(entry.position()==null?"Sin posición":"P"+entry.position())
            +" · "+(image==null?"Sin foto en tu colección":"Foto de tu colección local"));
        audio=entry==null?null:LocalResources.driverAudio(entry.driverId(),null).orElse(null);
        playButton.setDisable(audio==null);
        mediaStatus.setText(audio==null?"Sin audio local para esta selección.":"Audio local disponible · pulsa Escuchar.");
    }

    private void play() {
        dispose();if(audio==null)return;
        try {
            MediaPlayer current=new MediaPlayer(new Media(audio.toUri().toString()));player=current;
            mediaStatus.setText("Abriendo audio local…");stopButton.setDisable(false);
            current.setOnPlaying(()->{if(player==current)mediaStatus.setText("Reproduciendo tu audio local.");});
            current.setOnEndOfMedia(()->{if(player==current){dispose();mediaStatus.setText("Audio finalizado · puedes volver a escucharlo.");}});
            current.setOnError(()->{if(player==current){dispose();mediaStatus.setText("No se pudo reproducir este archivo. Revisa el dispositivo de audio.");}});
            current.play();
        } catch(RuntimeException e) {
            dispose();mediaStatus.setText("No se pudo abrir el audio local.");
        }
    }

    @Override public void dispose() {
        if(player!=null){MediaPlayer old=player;player=null;old.stop();old.dispose();}
        stopButton.setDisable(true);
        if(audio!=null)mediaStatus.setText("Audio detenido · pulsa Escuchar para reproducirlo.");
    }
}
