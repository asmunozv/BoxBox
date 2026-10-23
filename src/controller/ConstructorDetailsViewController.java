package controller;

import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import model.entity.Constructor;
import model.entity.ConstructorSeasonStats;
import view.LocalImageService;

public class ConstructorDetailsViewController {

    @FXML
    private HBox rootContainer;
    @FXML
    private ImageView avatarImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label countryLabel;
    @FXML
    private Label ergastIdLabel;
    @FXML
    private Label databaseIdLabel;
    @FXML
    private Label seasonLabel;
    @FXML
    private Label pointsLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label racesLabel;

    @FXML
    private void initialize() {
        for (Label label : Arrays.asList(
            nameLabel,
            countryLabel,
            ergastIdLabel,
            databaseIdLabel,
            seasonLabel,
            pointsLabel,
            winsLabel,
            racesLabel
        )) {
            label.setStyle("-fx-text-fill: white;");
        }
    }

    public void setData(Constructor constructorEntity, ConstructorSeasonStats stats, int season) {
        avatarImage.setImage(new Image(
            LocalImageService.resolveConstructorImage(
                constructorEntity.getErgastConstructorId(),
                constructorEntity.getName()
            ),
            true
        ));

        nameLabel.setText("Name: " + constructorEntity.getName());
        countryLabel.setText("Country: " + (constructorEntity.getCountry() == null ? "-" : constructorEntity.getCountry()));
        ergastIdLabel.setText("Ergast ID: " + constructorEntity.getErgastConstructorId());
        databaseIdLabel.setText("Database ID: " + constructorEntity.getIdConstructor());
        seasonLabel.setText("Season: " + season);
        pointsLabel.setText("Championship points: " + (stats.getPoints() == null ? "0" : stats.getPoints()));
        winsLabel.setText("Wins: " + stats.getWins());
        racesLabel.setText("Race entries: " + stats.getRaces());

        applyTeamGradient(constructorEntity.getName());
    }

    private void applyTeamGradient(String teamName) {
        String colorA = "#2a2f3b";
        String colorB = "#0c0e14";

        if (teamName != null) {
            String key = teamName.trim().toLowerCase();
            if (key.contains("ferrari")) {
                colorA = "#9f101a";
                colorB = "#21060a";
            } else if (key.contains("mercedes")) {
                colorA = "#008f82";
                colorB = "#071414";
            } else if (key.contains("mclaren")) {
                colorA = "#c15a00";
                colorB = "#1d0f07";
            } else if (key.contains("red bull")) {
                colorA = "#132c8a";
                colorB = "#090d22";
            } else if (key.contains("racing bulls") || key.equals("rb")) {
                colorA = "#1746b3";
                colorB = "#0a1230";
            } else if (key.contains("aston martin")) {
                colorA = "#006a57";
                colorB = "#071812";
            } else if (key.contains("alpine")) {
                colorA = "#005f9e";
                colorB = "#0a1020";
            } else if (key.contains("williams")) {
                colorA = "#1759b7";
                colorB = "#0a1226";
            } else if (key.contains("haas")) {
                colorA = "#5a5f68";
                colorB = "#141519";
            } else if (key.contains("sauber") || key.contains("audi")) {
                colorA = "#0f8a5a";
                colorB = "#08160f";
            }
        }

        rootContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " + colorA + ", " + colorB + ");"
            + "-fx-background-radius: 12;"
        );
    }
}
