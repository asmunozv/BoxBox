package controller;

import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import model.entity.Driver;
import model.entity.DriverSeasonStats;
import view.LocalImageService;

public class DriverDetailsViewController {

    @FXML
    private HBox rootContainer;
    @FXML
    private ImageView avatarImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label codeLabel;
    @FXML
    private Label numberLabel;
    @FXML
    private Label nationalityLabel;
    @FXML
    private Label dobLabel;
    @FXML
    private Label ergastIdLabel;
    @FXML
    private Label audioPathLabel;
    @FXML
    private Label teamLabel;
    @FXML
    private Label debutLabel;
    @FXML
    private Label quoteLabel;
    @FXML
    private Label seasonLabel;
    @FXML
    private Label pointsLabel;
    @FXML
    private Label racesLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label podiumsLabel;

    @FXML
    private void initialize() {
        for (Label label : Arrays.asList(
            nameLabel,
            codeLabel,
            numberLabel,
            nationalityLabel,
            dobLabel,
            ergastIdLabel,
            audioPathLabel,
            teamLabel,
            debutLabel,
            quoteLabel,
            seasonLabel,
            pointsLabel,
            racesLabel,
            winsLabel,
            podiumsLabel
        )) {
            label.setStyle("-fx-text-fill: white;");
        }
    }

    public void setData(Driver driver, DriverSeasonStats stats, int season, String teamName, Integer debutSeason) {
        DriverProfile profile = getDriverProfile(driver.getErgastDriverId());
        String resolvedTeam = profile.teamName == null ? teamName : profile.teamName;
        Integer resolvedDebut = profile.debutYear == null ? debutSeason : profile.debutYear;
        String resolvedQuote = profile.quote == null ? getDefaultQuote() : profile.quote;

        avatarImage.setImage(new Image(
            LocalImageService.resolveDriverImage(
                driver.getErgastDriverId(),
                driver.getGivenName() + " " + driver.getFamilyName()
            ),
            true
        ));

        nameLabel.setText("Name: " + driver.getGivenName() + " " + driver.getFamilyName());
        codeLabel.setText("Code: " + (driver.getCode() == null ? "-" : driver.getCode()));
        numberLabel.setText("Number: " + (driver.getPermanentNumber() == null ? "-" : driver.getPermanentNumber()));
        nationalityLabel.setText("Nationality: " + (driver.getNationality() == null ? "-" : driver.getNationality()));
        dobLabel.setText("Date of birth: " + (driver.getDateOfBirth() == null ? "-" : driver.getDateOfBirth()));
        ergastIdLabel.setText("Ergast ID: " + driver.getErgastDriverId());
        audioPathLabel.setText("Audio path: " + (driver.getAudioFilePath() == null ? "-" : driver.getAudioFilePath()));
        teamLabel.setText("Current team: " + (resolvedTeam == null || resolvedTeam.isBlank() ? "-" : resolvedTeam));
        debutLabel.setText("F1 debut season: " + (resolvedDebut == null ? "-" : resolvedDebut));
        quoteLabel.setText("Iconic quote: " + resolvedQuote);
        seasonLabel.setText("Season: " + season);
        pointsLabel.setText("Championship points: " + (stats.getPoints() == null ? "0" : stats.getPoints()));
        racesLabel.setText("Races: " + stats.getRaces());
        winsLabel.setText("Wins: " + stats.getWins());
        podiumsLabel.setText("Podiums: " + stats.getPodiums());

        applyTeamGradient(resolvedTeam);
    }

    private void applyTeamGradient(String teamName) {
        String colorA = "#1b1f2a";
        String colorB = "#0a0b10";

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

    private DriverProfile getDriverProfile(String ergastId) {
        if (ergastId == null) {
            return new DriverProfile(null, null, null);
        }
        String key = ergastId.trim().toLowerCase();
        switch (key) {
            case "max_verstappen":
                return new DriverProfile("Red Bull Racing", 2015, "Simply lovely.");
            case "hadjar":
                return new DriverProfile("Red Bull Racing", 2025, "Let's go!");
            case "russell":
                return new DriverProfile("Mercedes", 2019, "We go again.");
            case "antonelli":
                return new DriverProfile("Mercedes", 2025, "This is crazy!");
            case "leclerc":
                return new DriverProfile("Ferrari", 2018, "Nothing, just an inCHident on the race");
            case "hamilton":
                return new DriverProfile("Ferrari", 2007, "Still I rise.");
            case "norris":
                return new DriverProfile("McLaren", 2019, "Soy Lago");
            case "oscar_piastri":
            case "piastri":
                return new DriverProfile("McLaren", 2023, "That was unexpected.");
            case "alonso":
                return new DriverProfile("Aston Martin", 2001, "Karma.");
            case "stroll":
                return new DriverProfile("Aston Martin", 2017, "We'll see.");
            case "gasly":
                return new DriverProfile("Alpine", 2017, "What did we just do?!");
            case "colapinto":
                return new DriverProfile("Alpine", 2025, "Vamos carajo!");
            case "ocon":
                return new DriverProfile("Haas", 2016, "We fight.");
            case "bearman":
                return new DriverProfile("Haas", 2024, "That was mega.");
            case "sainz":
                return new DriverProfile("Williams", 2015, "Smoooooth operatooooor");
            case "albon":
                return new DriverProfile("Williams", 2019, "Mate...");
            case "hulkenberg":
            case "nico_hulkenberg":
                return new DriverProfile("Audi", 2010, "Finally!");
            case "bortoleto":
                return new DriverProfile("Audi", 2026, "Dream come true.");
            case "perez":
                return new DriverProfile("Cadillac", 2011, "Vamos!");
            case "bottas":
                return new DriverProfile("Cadillac", 2013, "To whom it may concern...");
            case "lawson":
                return new DriverProfile("Racing Bulls", 2023, "I'm ready.");
            case "arvid_lindblad":
                return new DriverProfile("Racing Bulls", 2026, "Let's prove it.");
            default:
                return new DriverProfile(null, null, null);
        }
    }

    private String getDefaultQuote() {
        return "Full send into Turn 1.";
    }

    private static class DriverProfile {
        private final String teamName;
        private final Integer debutYear;
        private final String quote;

        private DriverProfile(String teamName, Integer debutYear, String quote) {
            this.teamName = teamName;
            this.debutYear = debutYear;
            this.quote = quote;
        }
    }

}
