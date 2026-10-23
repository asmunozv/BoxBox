package controller;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entity.ConstructorStandingRow;
import model.entity.Constructor;
import model.entity.DriverStandingRow;
import model.entity.DriverConstructorSeason;
import model.entity.Driver;
import model.entity.Race;
import model.entity.Result;
import view.SeasonAwareView;

public class ResultViewController extends BorderPane implements SeasonAwareView {

    private final ResultController resultController;
    private final DriverController driverController;
    private final DriverConstructorSeasonController driverConstructorSeasonController;
    private final ConstructorController constructorController;
    private final RaceController raceController;

    @FXML
    private TableView<Result> resultTable;

    @FXML
    private ComboBox<DriverOption> driverComboBox;

    @FXML
    private ComboBox<ConstructorOption> constructorComboBox;

    @FXML
    private ComboBox<RaceOption> raceComboBox;

    @FXML
    private ComboBox<String> sessionTypeComboBox;

    @FXML
    private Spinner<Integer> positionSpinner;

    private int currentSeason;
    private final Set<String> sprintRaceNames;
    private final List<Race> seasonRaces;
    private final Map<Integer, String> driverNameById;
    private final Map<Integer, String> constructorNameById;
    private final Map<Integer, String> raceNameById;

    public ResultViewController() {
        this.resultController = new ResultController();
        this.driverController = new DriverController();
        this.driverConstructorSeasonController = new DriverConstructorSeasonController();
        this.constructorController = new ConstructorController();
        this.raceController = new RaceController();
        this.currentSeason = java.time.LocalDate.now().getYear();
        this.sprintRaceNames = new HashSet<>();
        this.seasonRaces = new java.util.ArrayList<>();
        this.driverNameById = new HashMap<>();
        this.constructorNameById = new HashMap<>();
        this.raceNameById = new HashMap<>();

        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ResultView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load ResultView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        applyBackgroundImage("assets/images/fondo/fondoresults.png", "src/assets/images/fondo/fondoresults.png");
        setPadding(new Insets(12));
        configureTable();
        configureForm();

        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });
        driverComboBox.valueProperty().addListener((obs, oldValue, selected) -> autoSelectConstructorForDriver(selected));

        constructorComboBox.setDisable(true);

        refreshAll();
    }

    private void configureTable() {
        TableColumn<Result, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idResult"));

        TableColumn<Result, String> raceColumn = new TableColumn<>("Race");
        raceColumn.setCellValueFactory(cellData -> {
            Integer id = cellData.getValue().getIdRace();
            String name = raceNameById.get(id);
            return new javafx.beans.property.SimpleStringProperty(name == null ? String.valueOf(id) : name);
        });

        TableColumn<Result, String> driverColumn = new TableColumn<>("Driver");
        driverColumn.setCellValueFactory(cellData -> {
            Integer id = cellData.getValue().getIdDriver();
            String name = driverNameById.get(id);
            return new javafx.beans.property.SimpleStringProperty(name == null ? String.valueOf(id) : name);
        });

        TableColumn<Result, String> constructorColumn = new TableColumn<>("Constructor");
        constructorColumn.setCellValueFactory(cellData -> {
            Integer id = cellData.getValue().getIdConstructor();
            String name = constructorNameById.get(id);
            return new javafx.beans.property.SimpleStringProperty(name == null ? String.valueOf(id) : name);
        });

        TableColumn<Result, String> sessionColumn = new TableColumn<>("Session");
        sessionColumn.setCellValueFactory(new PropertyValueFactory<>("sessionType"));

        TableColumn<Result, Integer> positionColumn = new TableColumn<>("Position");
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));

        TableColumn<Result, Object> pointsColumn = new TableColumn<>("Points");
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));

        resultTable.getColumns().addAll(idColumn, raceColumn, driverColumn, constructorColumn, sessionColumn, positionColumn, pointsColumn);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.setPrefHeight(320);
    }

    private void configureForm() {
        driverComboBox.setPromptText("Select driver");
        constructorComboBox.setPromptText("Select constructor");
        raceComboBox.setPromptText("Select race");
        sessionTypeComboBox.setPromptText("Session");
        sessionTypeComboBox.setItems(FXCollections.observableArrayList("RACE", "SPRINT"));
        sessionTypeComboBox.setValue("RACE");

        positionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        positionSpinner.setEditable(true);
    }

    @FXML
    private void onCreateResult() {
        createResult();
    }

    @FXML
    private void onUpdateResult() {
        updateResult();
    }

    @FXML
    private void onDeleteResult() {
        deleteResult();
    }

    @FXML
    private void onRefreshData() {
        refreshAll();
    }

    @FXML
    private void onShowDriverStandings() {
        showDriverStandings();
    }

    @FXML
    private void onShowConstructorStandings() {
        showConstructorStandings();
    }

    @FXML
    private void onSessionTypeChanged() {
        loadRaces();
    }

    private void refreshAll() {
        loadDrivers();
        loadConstructors();
        loadRaces();
        loadResults();
    }

    private void loadResults() {
        try {
            List<Result> results = resultController.getResultsBySeason(currentSeason);
            ObservableList<Result> items = FXCollections.observableArrayList(results);
            resultTable.setItems(items);
        } catch (ControllerException exception) {
            showError("Failed to load results", exception.getMessage());
        }
    }

    private void loadDrivers() {
        try {
            Integer selectedDriverId = driverComboBox.getValue() == null ? null : driverComboBox.getValue().getIdDriver();
            List<Driver> drivers = driverController.getAllDrivers();
            ObservableList<DriverOption> items = FXCollections.observableArrayList();
            driverNameById.clear();
            for (Driver driver : drivers) {
                if ("jak_crawford".equalsIgnoreCase(driver.getErgastDriverId())) {
                    continue;
                }
                items.add(new DriverOption(driver.getIdDriver(), driver.getGivenName(), driver.getFamilyName()));
                driverNameById.put(driver.getIdDriver(), driver.getGivenName() + " " + driver.getFamilyName());
            }
            driverComboBox.setItems(items);
            if (selectedDriverId != null) {
                selectDriverOptionById(selectedDriverId);
            }
            resultTable.refresh();
        } catch (ControllerException exception) {
            showError("Failed to load drivers", exception.getMessage());
        }
    }

    private void loadConstructors() {
        try {
            List<Constructor> constructors = constructorController.getAllConstructors();
            ObservableList<ConstructorOption> items = FXCollections.observableArrayList();
            constructorNameById.clear();
            for (Constructor constructorEntity : constructors) {
                if (constructorEntity.getName() != null && constructorEntity.getName().trim().equalsIgnoreCase("Sauber")) {
                    continue;
                }
                items.add(new ConstructorOption(constructorEntity.getIdConstructor(), constructorEntity.getName()));
                constructorNameById.put(constructorEntity.getIdConstructor(), constructorEntity.getName());
            }
            constructorComboBox.setItems(items);
            autoSelectConstructorForDriver(driverComboBox.getValue());
            resultTable.refresh();
        } catch (ControllerException exception) {
            showError("Failed to load constructors", exception.getMessage());
        }
    }

    private void loadRaces() {
        try {
            seasonRaces.clear();
            seasonRaces.addAll(raceController.getRacesBySeason(currentSeason));
            sprintRaceNames.clear();
            sprintRaceNames.addAll(readSprintRaceNames(currentSeason));

            String sessionType = sessionTypeComboBox.getValue() == null ? "RACE" : sessionTypeComboBox.getValue();
            ObservableList<RaceOption> items = FXCollections.observableArrayList();
            raceNameById.clear();
            for (Race race : seasonRaces) {
                boolean sprintWeekend = race.getName() != null && sprintRaceNames.contains(race.getName().trim().toLowerCase());
                raceNameById.put(race.getIdRace(), race.getName() + " [R" + race.getRoundNumber() + "]");
                if ("SPRINT".equalsIgnoreCase(sessionType)) {
                    if (sprintWeekend) {
                        items.add(new RaceOption(race.getIdRace(), race.getName(), race.getSeason(), race.getRoundNumber()));
                    }
                } else {
                    items.add(new RaceOption(race.getIdRace(), race.getName(), race.getSeason(), race.getRoundNumber()));
                }
            }
            raceComboBox.setItems(items);
            resultTable.refresh();
        } catch (ControllerException exception) {
            showError("Failed to load races", exception.getMessage());
        }
    }

    private void createResult() {
        try {
            Result result = buildResultFromForm();
            resultController.createResult(result);
            clearForm();
            loadResults();
        } catch (RuntimeException exception) {
            showError("Failed to create result", exception.getMessage());
        }
    }

    private void updateResult() {
        Result selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Update result", "Select a result from the table first.");
            return;
        }

        try {
            Result updated = buildResultFromForm();
            updated.setIdResult(selected.getIdResult());
            resultController.updateResult(updated);
            clearForm();
            loadResults();
        } catch (RuntimeException exception) {
            showError("Failed to update result", exception.getMessage());
        }
    }

    private void deleteResult() {
        Result selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Delete result", "Select a result from the table first.");
            return;
        }

        try {
            resultController.deleteResult(selected.getIdResult());
            clearForm();
            loadResults();
        } catch (RuntimeException exception) {
            showError("Failed to delete result", exception.getMessage());
        }
    }

    private Result buildResultFromForm() {
        DriverOption driverOption = driverComboBox.getValue();
        ConstructorOption constructorOption = constructorComboBox.getValue();
        RaceOption raceOption = raceComboBox.getValue();
        String sessionType = sessionTypeComboBox.getValue();
        Integer position = positionSpinner.getValue();

        if (driverOption == null) {
            throw new IllegalArgumentException("Please select a valid driver.");
        }
        if (constructorOption == null) {
            throw new IllegalArgumentException("Driver has no constructor assigned for season " + currentSeason + ".");
        }
        if (raceOption == null) {
            throw new IllegalArgumentException("Please select a valid race.");
        }
        if (position == null || position < 1 || position > 20) {
            throw new IllegalArgumentException("Position must be between 1 and 20.");
        }
        if (sessionType == null || sessionType.isBlank()) {
            throw new IllegalArgumentException("Select session type.");
        }

        boolean driverExists = driverController.getDriverById(driverOption.getIdDriver()).isPresent();
        boolean constructorExists = constructorController.getConstructorById(constructorOption.getIdConstructor()).isPresent();
        boolean raceExists = raceController.getRaceById(raceOption.getIdRace()).isPresent();
        if (!driverExists || !constructorExists || !raceExists) {
            refreshAll();
            throw new IllegalArgumentException(
                "Selected references are outdated. Click Refresh and select Driver/Constructor/Race again."
            );
        }

        Result result = new Result();
        result.setIdDriver(driverOption.getIdDriver());
        result.setIdConstructor(constructorOption.getIdConstructor());
        result.setIdRace(raceOption.getIdRace());
        result.setSessionType(sessionType);
        result.setPosition(position);
        return result;
    }

    private void fillForm(Result result) {
        selectDriverOptionById(result.getIdDriver());
        autoSelectConstructorForDriver(driverComboBox.getValue());
        sessionTypeComboBox.setValue(result.getSessionType() == null ? "RACE" : result.getSessionType());
        loadRaces();
        selectRaceOptionById(result.getIdRace());
        positionSpinner.getValueFactory().setValue(result.getPosition());
    }

    private void autoSelectConstructorForDriver(DriverOption driverOption) {
        if (driverOption == null) {
            constructorComboBox.getSelectionModel().clearSelection();
            return;
        }
        try {
            java.util.Optional<DriverConstructorSeason> mapping = driverConstructorSeasonController
                .getByDriverAndSeason(driverOption.getIdDriver(), currentSeason);
            if (mapping.isPresent()) {
                selectConstructorOptionById(mapping.get().getIdConstructor());
                return;
            }

            java.util.Optional<DriverConstructorSeason> latestMapping = driverConstructorSeasonController
                .getLatestByDriver(driverOption.getIdDriver());
            if (latestMapping.isPresent()) {
                selectConstructorOptionById(latestMapping.get().getIdConstructor());
                return;
            }

            Integer constructorFromResults = inferConstructorFromSeasonResults(driverOption.getIdDriver());
            if (constructorFromResults != null) {
                selectConstructorOptionById(constructorFromResults);
                return;
            }

            Integer constructorFromKnownDriverMapping = inferConstructorFromKnownDriverMapping(driverOption.getIdDriver());
            if (constructorFromKnownDriverMapping != null) {
                selectConstructorOptionById(constructorFromKnownDriverMapping);
                return;
            }

            constructorComboBox.getSelectionModel().clearSelection();
        } catch (ControllerException exception) {
            showError("Failed to resolve constructor", exception.getMessage());
            constructorComboBox.getSelectionModel().clearSelection();
        }
    }

    private Integer inferConstructorFromSeasonResults(Integer idDriver) {
        if (idDriver == null) {
            return null;
        }
        try {
            List<Result> results = resultController.getResultsBySeason(currentSeason);
            for (Result result : results) {
                if (idDriver.equals(result.getIdDriver()) && result.getIdConstructor() != null) {
                    return result.getIdConstructor();
                }
            }
            return null;
        } catch (ControllerException exception) {
            return null;
        }
    }

    private Integer inferConstructorFromKnownDriverMapping(Integer idDriver) {
        if (idDriver == null) {
            return null;
        }
        try {
            java.util.Optional<Driver> driverOptional = driverController.getDriverById(idDriver);
            if (driverOptional.isEmpty()) {
                return null;
            }
            String ergastId = driverOptional.get().getErgastDriverId();
            if (ergastId == null) {
                return null;
            }
            String key = ergastId.trim().toLowerCase();
            if ("oscar_piastri".equals(key)) {
                return findConstructorIdByNameKeyword("mclaren");
            }
            if ("hulkenberg".equals(key) || "nico_hulkenberg".equals(key)) {
                Integer audiId = findConstructorIdByNameKeyword("audi");
                if (audiId != null) {
                    return audiId;
                }
                return findConstructorIdByNameKeyword("sauber");
            }
            return null;
        } catch (ControllerException exception) {
            return null;
        }
    }

    private Integer findConstructorIdByNameKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String target = keyword.toLowerCase();
        for (ConstructorOption option : constructorComboBox.getItems()) {
            if (option.name != null && option.name.toLowerCase().contains(target)) {
                return option.getIdConstructor();
            }
        }
        return null;
    }

    private void selectDriverOptionById(Integer idDriver) {
        for (DriverOption option : driverComboBox.getItems()) {
            if (option.getIdDriver().equals(idDriver)) {
                driverComboBox.setValue(option);
                return;
            }
        }
        driverComboBox.getSelectionModel().clearSelection();
    }

    private void selectConstructorOptionById(Integer idConstructor) {
        for (ConstructorOption option : constructorComboBox.getItems()) {
            if (option.getIdConstructor().equals(idConstructor)) {
                constructorComboBox.setValue(option);
                return;
            }
        }
        constructorComboBox.getSelectionModel().clearSelection();
    }

    private void selectRaceOptionById(Integer idRace) {
        for (RaceOption option : raceComboBox.getItems()) {
            if (option.getIdRace().equals(idRace)) {
                raceComboBox.setValue(option);
                return;
            }
        }
        raceComboBox.getSelectionModel().clearSelection();
    }

    private void clearForm() {
        driverComboBox.getSelectionModel().clearSelection();
        constructorComboBox.getSelectionModel().clearSelection();
        raceComboBox.getSelectionModel().clearSelection();
        sessionTypeComboBox.setValue("RACE");
        positionSpinner.getValueFactory().setValue(1);
        resultTable.getSelectionModel().clearSelection();
    }

    @Override
    public void setSeason(int season) {
        this.currentSeason = season;
        refreshAll();
    }

    private Set<String> readSprintRaceNames(int season) {
        String env = System.getenv("F1_SPRINT_RACES_" + season);
        if (env == null || env.isBlank()) {
            env = System.getenv("F1_SPRINT_RACES");
        }

        Set<String> names = new HashSet<>();
        if (env != null && !env.isBlank()) {
            String[] tokens = env.split(",");
            for (String token : tokens) {
                String name = token.trim().toLowerCase();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        }

        if (names.isEmpty() && season == 2026) {
            names.add("chinese grand prix");
            names.add("miami grand prix");
            names.add("canadian grand prix");
            names.add("british grand prix");
            names.add("dutch grand prix");
            names.add("singapore grand prix");
        }
        return names;
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

    private void showDriverStandings() {
        List<DriverStandingRow> rows = resultController.getDriverStandingsBySeason(currentSeason);

        TableView<DriverStandingRow> table = new TableView<>();
        TableColumn<DriverStandingRow, Integer> posCol = new TableColumn<>("Pos");
        posCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        TableColumn<DriverStandingRow, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        TableColumn<DriverStandingRow, String> constructorCol = new TableColumn<>("Constructor");
        constructorCol.setCellValueFactory(new PropertyValueFactory<>("constructorName"));
        TableColumn<DriverStandingRow, Object> pointsCol = new TableColumn<>("Points");
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
        TableColumn<DriverStandingRow, Integer> winsCol = new TableColumn<>("Wins");
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        table.getColumns().addAll(posCol, driverCol, constructorCol, pointsCol, winsCol);
        table.setItems(FXCollections.observableArrayList(rows));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Drivers Championship - " + currentSeason);
        stage.setScene(new Scene(new BorderPane(table), 760, 520));
        stage.showAndWait();
    }

    private void showConstructorStandings() {
        List<ConstructorStandingRow> rows = resultController.getConstructorStandingsBySeason(currentSeason);

        TableView<ConstructorStandingRow> table = new TableView<>();
        TableColumn<ConstructorStandingRow, Integer> posCol = new TableColumn<>("Pos");
        posCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        TableColumn<ConstructorStandingRow, String> constructorCol = new TableColumn<>("Constructor");
        constructorCol.setCellValueFactory(new PropertyValueFactory<>("constructorName"));
        TableColumn<ConstructorStandingRow, Object> pointsCol = new TableColumn<>("Points");
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
        TableColumn<ConstructorStandingRow, Integer> winsCol = new TableColumn<>("Wins");
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        table.getColumns().addAll(posCol, constructorCol, pointsCol, winsCol);
        table.setItems(FXCollections.observableArrayList(rows));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Constructors Championship - " + currentSeason);
        stage.setScene(new Scene(new BorderPane(table), 640, 520));
        stage.showAndWait();
    }

    private static class DriverOption {
        private final Integer idDriver;
        private final String givenName;
        private final String familyName;

        private DriverOption(Integer idDriver, String givenName, String familyName) {
            this.idDriver = idDriver;
            this.givenName = givenName;
            this.familyName = familyName;
        }

        private Integer getIdDriver() {
            return idDriver;
        }

        @Override
        public String toString() {
            return givenName + " " + familyName + " (ID: " + idDriver + ")";
        }
    }

    private static class ConstructorOption {
        private final Integer idConstructor;
        private final String name;

        private ConstructorOption(Integer idConstructor, String name) {
            this.idConstructor = idConstructor;
            this.name = name;
        }

        private Integer getIdConstructor() {
            return idConstructor;
        }

        @Override
        public String toString() {
            return name + " (ID: " + idConstructor + ")";
        }
    }

    private static class RaceOption {
        private final Integer idRace;
        private final String raceName;
        private final Integer season;
        private final Integer round;

        private RaceOption(Integer idRace, String raceName, Integer season, Integer round) {
            this.idRace = idRace;
            this.raceName = raceName;
            this.season = season;
            this.round = round;
        }

        private Integer getIdRace() {
            return idRace;
        }

        @Override
        public String toString() {
            return raceName + " [" + season + " R" + round + "] (ID: " + idRace + ")";
        }
    }

    private void applyBackgroundImage(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (!file.exists()) {
                continue;
            }
            Image image = new Image(file.toURI().toString(), true);
            BackgroundSize size = new BackgroundSize(100, 100, true, true, true, true);
            setBackground(new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size
            )));
            return;
        }
    }
}
