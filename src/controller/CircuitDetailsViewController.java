package controller;

import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import model.entity.Circuit;
import view.LocalImageService;

public class CircuitDetailsViewController {

    @FXML
    private HBox rootContainer;
    @FXML
    private ImageView avatarImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label countryLabel;
    @FXML
    private Label localityLabel;
    @FXML
    private Label latitudeLabel;
    @FXML
    private Label longitudeLabel;
    @FXML
    private Label ergastIdLabel;
    @FXML
    private Label seasonLabel;
    @FXML
    private Label racesHostedLabel;

    @FXML
    private void initialize() {
        for (Label label : Arrays.asList(
            nameLabel,
            countryLabel,
            localityLabel,
            latitudeLabel,
            longitudeLabel,
            ergastIdLabel,
            seasonLabel,
            racesHostedLabel
        )) {
            label.setStyle("-fx-text-fill: white;");
        }
    }

    public void setData(Circuit circuit, int season, int racesThisSeason) {
        avatarImage.setImage(new Image(
            LocalImageService.resolveCircuitImage(
                circuit.getErgastCircuitId(),
                circuit.getName()
            ),
            true
        ));

        nameLabel.setText("Name: " + circuit.getName());
        countryLabel.setText("Country: " + (circuit.getCountry() == null ? "-" : circuit.getCountry()));
        localityLabel.setText("Locality: " + (circuit.getLocality() == null ? "-" : circuit.getLocality()));
        latitudeLabel.setText("Latitude: " + (circuit.getLatitude() == null ? "-" : circuit.getLatitude()));
        longitudeLabel.setText("Longitude: " + (circuit.getLongitude() == null ? "-" : circuit.getLongitude()));
        ergastIdLabel.setText("Ergast ID: " + circuit.getErgastCircuitId());
        seasonLabel.setText("Season: " + season);
        racesHostedLabel.setText("Races hosted this season: " + racesThisSeason);

        rootContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #283241, #10141d);"
            + "-fx-background-radius: 12;"
        );
    }
}
