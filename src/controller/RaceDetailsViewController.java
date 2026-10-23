package controller;

import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import model.entity.Race;
import view.LocalImageService;
import view.SeasonContext;

public class RaceDetailsViewController {

    @FXML
    private HBox rootContainer;
    @FXML
    private ImageView avatarImage;
    @FXML
    private Label raceLabel;
    @FXML
    private Label seasonLabel;
    @FXML
    private Label roundLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label circuitIdLabel;
    @FXML
    private Label raceIdLabel;
    @FXML
    private Label entriesLabel;
    @FXML
    private Label winnersLabel;

    @FXML
    private void initialize() {
        for (Label label : Arrays.asList(
            raceLabel,
            seasonLabel,
            roundLabel,
            dateLabel,
            circuitIdLabel,
            raceIdLabel,
            entriesLabel,
            winnersLabel
        )) {
            label.setStyle("-fx-text-fill: white;");
        }
    }

    public void setData(Race race, int totalEntries, long winsInRace) {
        avatarImage.setImage(new Image(
            LocalImageService.resolveRaceImage(
                race.getName(),
                race.getSeason() == null ? SeasonContext.getActiveSeason() : race.getSeason(),
                race.getRoundNumber() == null ? 0 : race.getRoundNumber()
            ),
            true
        ));

        raceLabel.setText("Race: " + race.getName());
        seasonLabel.setText("Season: " + race.getSeason());
        roundLabel.setText("Round: " + race.getRoundNumber());
        dateLabel.setText("Date: " + race.getRaceDate());
        circuitIdLabel.setText("Circuit ID: " + race.getIdCircuit());
        raceIdLabel.setText("Race ID: " + race.getIdRace());
        entriesLabel.setText("Recorded entries: " + totalEntries);
        winnersLabel.setText("Winner records: " + winsInRace);

        rootContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #2d3040, #11131a);"
            + "-fx-background-radius: 12;"
        );
    }
}
