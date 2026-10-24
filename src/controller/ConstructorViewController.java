package controller;

import java.io.IOException;
import java.util.List;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entity.Constructor;
import model.entity.ConstructorSeasonStats;
import view.SeasonContext;

public class ConstructorViewController extends BorderPane {

    private final ConstructorController constructorController;

    @FXML
    private TableView<Constructor> constructorTable;

    @FXML
    private VBox contentContainer;

    @FXML
    private Label titleLabel;

    public ConstructorViewController() {
        this.constructorController = new ConstructorController();
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ConstructorView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load ConstructorView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        applyBackgroundImage("assets/images/fondo/fondoconstructor.png", "src/assets/images/fondo/fondoconstructor.png");
        setPadding(new Insets(12));
        contentContainer.setStyle("-fx-background-color: rgba(0,0,0,0.42); -fx-background-radius: 10;");
        titleLabel.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 8, 0.45, 0, 1);");
        configureTable();

        constructorTable.setRowFactory(table -> {
            TableRow<Constructor> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !row.isEmpty()) {
                    showConstructorDetails(row.getItem());
                }
            });
            return row;
        });

        loadConstructors();
    }

    @FXML
    private void onViewProfile() {
        Constructor selected = constructorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Constructor profile", "Select a constructor first.");
            return;
        }
        showConstructorDetails(selected);
    }

    @FXML
    private void onRefresh() {
        loadConstructors();
    }

    private void configureTable() {
        TableColumn<Constructor, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idConstructor"));

        TableColumn<Constructor, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Constructor, String> countryColumn = new TableColumn<>("Country");
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));

        constructorTable.getColumns().addAll(idColumn, nameColumn, countryColumn);
        constructorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        constructorTable.setPrefHeight(320);
    }

    private void loadConstructors() {
        try {
            List<Constructor> constructors = constructorController.getAllConstructors();
            constructors.removeIf(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase("Sauber"));
            ObservableList<Constructor> items = FXCollections.observableArrayList(constructors);
            constructorTable.setItems(items);
        } catch (ControllerException exception) {
            showError("Failed to load constructors", exception.getMessage());
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

    private void showConstructorDetails(Constructor constructorEntity) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Constructor Profile - " + constructorEntity.getName());

        int season = SeasonContext.getActiveSeason();
        ConstructorSeasonStats stats = constructorController.getConstructorSeasonStats(constructorEntity.getIdConstructor(), season);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ConstructorDetailsView.fxml"));
            Parent root = loader.load();
            ConstructorDetailsViewController detailsController = loader.getController();
            detailsController.setData(constructorEntity, stats, season);
            stage.setScene(view.SceneTheme.apply(new Scene(root, 500, 220)));
            stage.showAndWait();
        } catch (IOException exception) {
            showError("Constructor profile", "Could not load constructor detail view.");
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
