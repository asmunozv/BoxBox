package controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entity.Circuit;
import model.entity.Race;
import view.SeasonAwareView;

public class RaceViewController extends BorderPane implements SeasonAwareView {

    private static final double MAP_LON_MIN = -180.0;
    private static final double MAP_LON_MAX = 180.0;
    private static final double MAP_LAT_MIN = -85.05112878;
    private static final double MAP_LAT_MAX = 85.05112878;
    private static final boolean USE_MERCATOR_Y = true;

    private final RaceController raceController;
    private final CircuitController circuitController;
    private final ResultController resultController;

    @FXML
    private ImageView mapImageView;
    @FXML
    private Pane mapOverlayPane;
    @FXML
    private StackPane mapContainer;

    private int currentSeason;
    private final Map<Integer, Circuit> circuitById;
    private final List<Race> currentSeasonRaces;
    private final Map<String, double[]> manualPointByCircuitErgastId;

    public RaceViewController() {
        this.raceController = new RaceController();
        this.circuitController = new CircuitController();
        this.resultController = new ResultController();
        this.currentSeason = java.time.LocalDate.now().getYear();
        this.circuitById = new HashMap<>();
        this.currentSeasonRaces = new ArrayList<>();
        this.manualPointByCircuitErgastId = buildManualPointMap();
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RaceView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load RaceView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        setStyle("-fx-padding: 12;");
        loadMapImage();
        mapContainer.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> renderCircuitPoints());
        refreshAll();
    }

    @FXML
    private void onRefresh() {
        refreshAll();
    }

    private void refreshAll() {
        loadSeasonRaces();
        loadCircuits();
        renderCircuitPoints();
    }

    private void loadSeasonRaces() {
        try {
            currentSeasonRaces.clear();
            currentSeasonRaces.addAll(raceController.getRacesBySeason(currentSeason));
        } catch (ControllerException exception) {
            showError("Failed to load races", exception.getMessage());
        }
    }

    private void loadCircuits() {
        try {
            circuitById.clear();
            for (Circuit circuit : circuitController.getAllCircuits()) {
                circuitById.put(circuit.getIdCircuit(), circuit);
            }
        } catch (ControllerException exception) {
            showError("Failed to load circuits", exception.getMessage());
        }
    }

    private void loadMapImage() {
        File[] candidates = {
            new File("assets/images/maps/world_map_light.png"),
            new File("src/assets/images/maps/world_map_light.png")
        };
        for (File file : candidates) {
            if (!file.exists()) {
                continue;
            }
            mapImageView.setImage(new Image(file.toURI().toString(), true));
            return;
        }
        showInfo("World map", "Map image not found at assets/images/maps/world_map_light.png");
    }

    private void renderCircuitPoints() {
        mapOverlayPane.getChildren().clear();
        if (mapImageView.getImage() == null) {
            return;
        }

        Image image = mapImageView.getImage();
        double containerWidth = mapContainer.getWidth() > 0 ? mapContainer.getWidth() : mapContainer.getPrefWidth();
        double containerHeight = mapContainer.getHeight() > 0 ? mapContainer.getHeight() : mapContainer.getPrefHeight();
        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        double scale = Math.min(containerWidth / imageWidth, containerHeight / imageHeight);
        double renderedWidth = imageWidth * scale;
        double renderedHeight = imageHeight * scale;
        double offsetX = (containerWidth - renderedWidth) / 2.0;
        double offsetY = (containerHeight - renderedHeight) / 2.0;

        mapOverlayPane.setPrefSize(containerWidth, containerHeight);

        for (Race race : currentSeasonRaces) {
            Circuit circuit = circuitById.get(race.getIdCircuit());
            if (circuit == null || circuit.getLatitude() == null || circuit.getLongitude() == null) {
                continue;
            }

            double x;
            double y;
            double[] manual = manualPointByCircuitErgastId.get(normalizeKey(circuit.getErgastCircuitId()));
            if (manual != null) {
                x = offsetX + (manual[0] * renderedWidth);
                y = offsetY + (manual[1] * renderedHeight);
            } else {
                x = offsetX + longitudeToX(circuit.getLongitude(), renderedWidth);
                y = offsetY + latitudeToY(circuit.getLatitude(), renderedHeight);
            }

            Circle point = new Circle(x, y, 5.5, Color.web("#e11d2e"));
            point.setStroke(Color.WHITE);
            point.setStrokeWidth(1.2);

            String locality = circuit.getLocality() == null ? "" : circuit.getLocality();
            String country = circuit.getCountry() == null ? "" : circuit.getCountry();
            Tooltip tooltip = new Tooltip(race.getName() + "\n" + circuit.getName() + " - " + locality + ", " + country);
            Tooltip.install(point, tooltip);

            point.setOnMouseEntered(event -> point.setRadius(7.5));
            point.setOnMouseExited(event -> point.setRadius(5.5));
            point.setOnMouseClicked(event -> showRaceDetails(race));

            mapOverlayPane.getChildren().add(point);
        }
    }

    private double longitudeToX(BigDecimal longitude, double width) {
        double lon = clamp(longitude.doubleValue(), MAP_LON_MIN, MAP_LON_MAX);
        double ratio = (lon - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN);
        return ratio * width;
    }

    private double latitudeToY(BigDecimal latitude, double height) {
        double lat = clamp(latitude.doubleValue(), MAP_LAT_MIN, MAP_LAT_MAX);
        if (!USE_MERCATOR_Y) {
            double ratio = (MAP_LAT_MAX - lat) / (MAP_LAT_MAX - MAP_LAT_MIN);
            return ratio * height;
        }

        double latRad = Math.toRadians(lat);
        double mercN = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));

        double maxRad = Math.toRadians(MAP_LAT_MAX);
        double minRad = Math.toRadians(MAP_LAT_MIN);
        double mercMax = Math.log(Math.tan(Math.PI / 4.0 + maxRad / 2.0));
        double mercMin = Math.log(Math.tan(Math.PI / 4.0 + minRad / 2.0));

        double ratio = (mercMax - mercN) / (mercMax - mercMin);
        return ratio * height;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, double[]> buildManualPointMap() {
        Map<String, double[]> map = new HashMap<>();
        map.put("albert_park", point(0.875, 0.804));
        map.put("shanghai", point(0.790, 0.455));
        map.put("suzuka", point(0.835, 0.368));
        map.put("bahrain", point(0.612, 0.392));
        map.put("jeddah", point(0.582, 0.462));
        map.put("miami", point(0.252, 0.417));
        map.put("imola", point(0.511, 0.342));
        map.put("catalunya", point(0.480, 0.365));
        map.put("monaco", point(0.490, 0.355));
        map.put("gilles_villeneuve", point(0.268, 0.292));
        map.put("red_bull_ring", point(0.521, 0.329));
        map.put("silverstone", point(0.463, 0.305));
        map.put("spa", point(0.479, 0.298));
        map.put("hungaroring", point(0.531, 0.322));
        map.put("zandvoort", point(0.482, 0.285));
        map.put("monza", point(0.511, 0.339));
        map.put("baku", point(0.580, 0.349));
        map.put("marina_bay", point(0.745, 0.613));
        map.put("americas", point(0.198, 0.472));
        map.put("rodriguez", point(0.179, 0.505));
        map.put("interlagos", point(0.319, 0.722));
        map.put("las_vegas", point(0.142, 0.392));
        map.put("losail", point(0.607, 0.433));
        map.put("yas_marina", point(0.625, 0.445));
        return Collections.unmodifiableMap(map);
    }

    private double[] point(double xRatio, double yRatio) {
        return new double[] {xRatio, yRatio};
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void showRaceDetails(Race race) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Race - " + race.getName());

        int totalEntries = resultController.getResultsByRace(race.getIdRace()).size();
        long winsInRace = resultController.getResultsByRace(race.getIdRace())
            .stream().filter(r -> r.getPosition() != null && r.getPosition() == 1).count();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RaceDetailsView.fxml"));
            Parent root = loader.load();
            RaceDetailsViewController detailsController = loader.getController();
            detailsController.setData(race, totalEntries, winsInRace);
            stage.setScene(view.SceneTheme.apply(new Scene(root, 520, 250)));
            stage.showAndWait();
        } catch (IOException exception) {
            showError("Race details", "Could not load race detail view.");
        }
    }

    @Override
    public void setSeason(int season) {
        this.currentSeason = season;
        refreshAll();
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
}
