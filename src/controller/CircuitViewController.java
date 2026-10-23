package controller;

import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entity.Circuit;
import view.SeasonContext;

public class CircuitViewController extends BorderPane {

    private final CircuitController circuitController;
    private final RaceController raceController;

    @FXML
    private TableView<Circuit> circuitTable;

    public CircuitViewController() {
        this.circuitController = new CircuitController();
        this.raceController = new RaceController();
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CircuitView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load CircuitView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        setPadding(new Insets(12));
        configureTable();

        circuitTable.setRowFactory(table -> {
            TableRow<Circuit> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !row.isEmpty()) {
                    showCircuitDetails(row.getItem());
                }
            });
            return row;
        });

        loadCircuits();
    }

    @FXML
    private void onViewDetails() {
        Circuit selected = circuitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Circuit details", "Select a circuit first.");
            return;
        }
        showCircuitDetails(selected);
    }

    @FXML
    private void onRefresh() {
        loadCircuits();
    }

    private void configureTable() {
        TableColumn<Circuit, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idCircuit"));

        TableColumn<Circuit, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Circuit, String> countryColumn = new TableColumn<>("Country");
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));

        TableColumn<Circuit, String> localityColumn = new TableColumn<>("Locality");
        localityColumn.setCellValueFactory(new PropertyValueFactory<>("locality"));

        TableColumn<Circuit, Object> latitudeColumn = new TableColumn<>("Latitude");
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("latitude"));

        TableColumn<Circuit, Object> longitudeColumn = new TableColumn<>("Longitude");
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("longitude"));

        circuitTable.getColumns().addAll(idColumn, nameColumn, countryColumn, localityColumn, latitudeColumn, longitudeColumn);
        circuitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        circuitTable.setPrefHeight(320);
    }

    private void loadCircuits() {
        try {
            List<Circuit> circuits = circuitController.getAllCircuits();
            ObservableList<Circuit> items = FXCollections.observableArrayList(circuits);
            circuitTable.setItems(items);
        } catch (ControllerException exception) {
            showError("Failed to load circuits", exception.getMessage());
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

    private void showCircuitDetails(Circuit circuit) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Circuit - " + circuit.getName());

        int season = SeasonContext.getActiveSeason();
        int racesThisSeason = (int) raceController.getRacesBySeason(season)
            .stream()
            .filter(r -> r.getIdCircuit() != null && r.getIdCircuit().equals(circuit.getIdCircuit()))
            .count();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CircuitDetailsView.fxml"));
            Parent root = loader.load();
            CircuitDetailsViewController detailsController = loader.getController();
            detailsController.setData(circuit, season, racesThisSeason);
            stage.setScene(new Scene(root, 560, 260));
            stage.showAndWait();
        } catch (IOException exception) {
            showError("Circuit details", "Could not load circuit detail view.");
        }
    }
}
