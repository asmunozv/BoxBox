package controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import model.entity.Driver;
import model.entity.DriverSeasonStats;
import view.SeasonContext;

public class DriverViewController extends BorderPane {

    private final DriverController driverController;
    private final DriverConstructorSeasonController driverConstructorSeasonController;
    private final ConstructorController constructorController;
    private final ResultController resultController;

    @FXML
    private TableView<Driver> driverTable;

    private MediaPlayer currentMediaPlayer;

    public DriverViewController() {
        this.driverController = new DriverController();
        this.driverConstructorSeasonController = new DriverConstructorSeasonController();
        this.constructorController = new ConstructorController();
        this.resultController = new ResultController();
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load DriverView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        setStyle("-fx-background-color: #05070d;");
        setPadding(new Insets(12));

        configureTable();

        driverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                playDriverAudio(selected, selected.getAudioFilePath());
            }
        });

        driverTable.setRowFactory(table -> {
            TableRow<Driver> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !row.isEmpty()) {
                    showDriverDetails(row.getItem());
                }
            });
            return row;
        });

        loadDrivers();
    }

    @FXML
    private void onViewProfile() {
        Driver selected = driverTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Driver profile", "Select a driver first.");
            return;
        }
        showDriverDetails(selected);
    }

    @FXML
    private void onRefresh() {
        loadDrivers();
    }

    private void configureTable() {
        TableColumn<Driver, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idDriver"));

        TableColumn<Driver, String> ergastIdColumn = new TableColumn<>("Ergast ID");
        ergastIdColumn.setCellValueFactory(new PropertyValueFactory<>("ergastDriverId"));

        TableColumn<Driver, String> givenNameColumn = new TableColumn<>("Given Name");
        givenNameColumn.setCellValueFactory(new PropertyValueFactory<>("givenName"));

        TableColumn<Driver, String> familyNameColumn = new TableColumn<>("Family Name");
        familyNameColumn.setCellValueFactory(new PropertyValueFactory<>("familyName"));

        TableColumn<Driver, String> nationalityColumn = new TableColumn<>("Nationality");
        nationalityColumn.setCellValueFactory(new PropertyValueFactory<>("nationality"));

        TableColumn<Driver, Integer> numberColumn = new TableColumn<>("Number");
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("permanentNumber"));

        driverTable.getColumns().addAll(idColumn, ergastIdColumn, givenNameColumn, familyNameColumn, nationalityColumn, numberColumn);
        driverTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        driverTable.setPrefHeight(280);
    }

    private void loadDrivers() {
        try {
            List<Driver> drivers = driverController.getAllDrivers();
            drivers.removeIf(driver -> "jak_crawford".equalsIgnoreCase(driver.getErgastDriverId()));
            ObservableList<Driver> items = FXCollections.observableArrayList(drivers);
            driverTable.setItems(items);
        } catch (ControllerException exception) {
            showError("Failed to load drivers", exception.getMessage());
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void playDriverAudio(Driver driver, String audioFilePath) {
        stopCurrentAudio();

        try {
            File file = resolveAudioFile(driver, audioFilePath);
            if (file == null || !file.exists()) {
                return;
            }

            Media media = new Media(file.toURI().toString());
            currentMediaPlayer = new MediaPlayer(media);
            currentMediaPlayer.play();
        } catch (RuntimeException exception) {
            currentMediaPlayer = null;
        }
    }

    private File resolveAudioFile(Driver driver, String audioFilePath) {
        if (audioFilePath != null && !audioFilePath.isBlank()) {
            File explicit = new File(audioFilePath);
            if (explicit.exists()) {
                return explicit;
            }
        }

        if (driver == null || driver.getErgastDriverId() == null || driver.getErgastDriverId().isBlank()) {
            return null;
        }

        String base = driver.getErgastDriverId().trim().toLowerCase();
        String[] extensions = {".mp3", ".wav", ".m4a"};
        for (String ext : extensions) {
            File file = new File("assets/audio/drivers/" + base + ext);
            if (file.exists()) {
                return file;
            }
            File fileSrc = new File("src/assets/audio/drivers/" + base + ext);
            if (fileSrc.exists()) {
                return fileSrc;
            }
        }
        return null;
    }

    private void stopCurrentAudio() {
        if (currentMediaPlayer != null) {
            currentMediaPlayer.stop();
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }
    }

    private void showDriverDetails(Driver driver) {
        playDriverAudio(driver, driver.getAudioFilePath());

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Driver Profile - " + driver.getGivenName() + " " + driver.getFamilyName());
        stage.setOnHidden(event -> stopCurrentAudio());

        int season = SeasonContext.getActiveSeason();
        DriverSeasonStats stats = driverController.getDriverSeasonStats(driver.getIdDriver(), season);
        String teamName = resolveTeamName(driver.getIdDriver(), season);
        Integer debutSeason = null;
        try {
            debutSeason = driverController.getDriverDebutSeason(driver.getIdDriver()).orElse(null);
        } catch (ControllerException exception) {
            debutSeason = null;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDetailsView.fxml"));
            Parent root = loader.load();
            DriverDetailsViewController detailsController = loader.getController();
            detailsController.setData(driver, stats, season, teamName, debutSeason);
            stage.setScene(view.SceneTheme.apply(new Scene(root, 560, 280)));
            stage.showAndWait();
        } catch (IOException exception) {
            showError("Driver profile", "Could not load driver detail view.");
        }
    }

    private String resolveTeamName(Integer idDriver, int season) {
        if (idDriver == null) {
            return null;
        }
        try {
            java.util.Optional<model.entity.DriverConstructorSeason> mapping =
                driverConstructorSeasonController.getByDriverAndSeason(idDriver, season);
            if (mapping.isEmpty()) {
                mapping = driverConstructorSeasonController.getLatestByDriver(idDriver);
            }
            if (mapping.isPresent()) {
                return constructorController.getConstructorById(mapping.get().getIdConstructor())
                    .map(model.entity.Constructor::getName)
                    .orElse(null);
            }

            java.util.Optional<Driver> driverOptional = driverController.getDriverById(idDriver);
            if (driverOptional.isPresent()) {
                String ergastId = driverOptional.get().getErgastDriverId();
                if (ergastId != null) {
                    String key = ergastId.trim().toLowerCase();
                    if ("oscar_piastri".equals(key) || "piastri".equals(key)) {
                        return "McLaren";
                    }
                    if ("hulkenberg".equals(key) || "nico_hulkenberg".equals(key)) {
                        return "Audi";
                    }
                }
            }

            Integer constructorIdFromSeasonResults = inferConstructorFromSeasonResults(idDriver, season);
            if (constructorIdFromSeasonResults != null) {
                return constructorController.getConstructorById(constructorIdFromSeasonResults)
                    .map(model.entity.Constructor::getName)
                    .orElse(null);
            }

            Integer constructorIdFromAnyResult = inferConstructorFromAnyResult(idDriver);
            if (constructorIdFromAnyResult != null) {
                return constructorController.getConstructorById(constructorIdFromAnyResult)
                    .map(model.entity.Constructor::getName)
                    .orElse(null);
            }
        } catch (ControllerException exception) {
            return null;
        }
        return null;
    }

    private Integer inferConstructorFromSeasonResults(Integer idDriver, int season) {
        try {
            List<model.entity.Result> results = resultController.getResultsBySeason(season);
            for (model.entity.Result result : results) {
                if (idDriver.equals(result.getIdDriver()) && result.getIdConstructor() != null) {
                    return result.getIdConstructor();
                }
            }
        } catch (ControllerException exception) {
            return null;
        }
        return null;
    }

    private Integer inferConstructorFromAnyResult(Integer idDriver) {
        try {
            List<model.entity.Result> results = resultController.getAllResults();
            Integer found = null;
            for (model.entity.Result result : results) {
                if (idDriver.equals(result.getIdDriver()) && result.getIdConstructor() != null) {
                    found = result.getIdConstructor();
                }
            }
            return found;
        } catch (ControllerException exception) {
            return null;
        }
    }
}
