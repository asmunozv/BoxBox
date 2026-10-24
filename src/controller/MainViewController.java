package controller;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import view.SeasonContext;

public class MainViewController extends BorderPane implements view.DisposableView {

    private final DataImportController dataImportController;
    private final RaceViewController raceView;
    private final ResultViewController resultView;
    private final MiniGameViewController miniGameView;
    private final BingoViewController bingoView;
    private final Runnable onLogout;
    private static final int ACTIVE_SEASON = 2026;

    @FXML
    private Label seasonLabel;

    @FXML
    private Label userLabel;

    private HistoricalExplorerController explorer;
    private GuideViewController guide;
    private final java.util.Map<Integer,Button> analysisButtons=new java.util.HashMap<>();
    @FXML private VBox analysisNavigation,legacyNavigation;
    @FXML private StackPane contentHost;

    @FXML
    private Button logoutButton;

    private final TabPane tabPane = new TabPane();

    public MainViewController(Runnable onLogout) {
        this.dataImportController = new DataImportController();
        this.raceView = new RaceViewController();
        this.resultView = new ResultViewController();
        this.miniGameView = new MiniGameViewController();
        this.bingoView = new BingoViewController();
        this.onLogout = onLogout;
        SeasonContext.setActiveSeason(ACTIVE_SEASON);
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load MainView.fxml", exception);
        }
    }

    @FXML
    private void initialize() {
        getStyleClass().add("main-view");
        seasonLabel.setText("MySQL local · boxbox_db");
        String username = UserSession.getCurrentUsername();
        userLabel.setText("Sesión: " + (username == null ? "-" : username));
        logoutButton.setOnAction(event -> {
            dispose();
            UserSession.logout();
            if (onLogout != null) {
                onLogout.run();
            }
        });

        tabPane.getTabs().add(createTab("Drivers", new DriverViewController()));
        tabPane.getTabs().add(createTab("Constructors", new ConstructorViewController()));
        tabPane.getTabs().add(createTab("Races & Circuits", raceView));
        tabPane.getTabs().add(createTab("Results", resultView));
        tabPane.getTabs().add(createTab("Bingo", bingoView));
        Tab miniGameTab = createTab("Mini Game", miniGameView);
        tabPane.getTabs().add(miniGameTab);
        tabPane.getStyleClass().add("legacy-tabs");
        explorer = new RaceLabController();
        Button explore = nav("Explorador histórico", () -> showAnalysis(0));
        explore.getStyleClass().add("nav-active");
        analysisNavigation.getChildren().add(explore);
        analysisButtons.put(0,explore);
        String[] analysisTitles={"Comparador","Neumáticos","Paradas en pits","Evolución de posiciones","Simulador","Historia de carrera"};
        for(int i=0;i<analysisTitles.length;i++){final int index=i+1;Button button=nav(analysisTitles[i],()->showAnalysis(index));analysisNavigation.getChildren().add(button);analysisButtons.put(index,button);}
        guide=new GuideViewController(this::showAnalysis,()->{showAnalysis(0);explorer.loadDemo();});
        Button guideButton=nav("Cómo usar BoxBox",()->showContent(guide));analysisNavigation.getChildren().add(guideButton);
        explorer.onGuide(()->{showContent(guide);markActive(guideButton);});
        explorer.selectedTabIndexProperty().addListener((o,a,b)->{if(contentHost.getChildren().contains(explorer))markActive(analysisButtons.get(b.intValue()));});
        String[] titles={"Pilotos","Escuderías","Mapa y circuitos","Gestión de resultados","Bingo","Arcade"};
        for(int i=0;i<titles.length;i++){
            final int index=i;
            legacyNavigation.getChildren().add(nav(titles[i],()->{showContent(tabPane);tabPane.getSelectionModel().select(index);if(index==5)miniGameView.onTabSelected();}));
        }
        showContent(explorer);

        miniGameTab.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (selected) {
                miniGameView.onTabSelected();
            } else {
                miniGameView.onTabUnselected();
            }
        });

        raceView.setSeason(ACTIVE_SEASON);
        resultView.setSeason(ACTIVE_SEASON);
    }

    private Button nav(String text,Runnable action){Button button=new Button(text);button.getStyleClass().add("nav-item");button.setMaxWidth(Double.MAX_VALUE);button.setOnAction(e->{for(var node:analysisNavigation.getChildren())node.getStyleClass().remove("nav-active");for(var node:legacyNavigation.getChildren())node.getStyleClass().remove("nav-active");button.getStyleClass().add("nav-active");action.run();});return button;}
    private void markActive(Button button){for(var node:analysisNavigation.getChildren())node.getStyleClass().remove("nav-active");for(var node:legacyNavigation.getChildren())node.getStyleClass().remove("nav-active");if(button!=null)button.getStyleClass().add("nav-active");}
    private void showAnalysis(int index){showContent(explorer);explorer.openTab(index);markActive(analysisButtons.get(explorer.selectedTabIndexProperty().get()));}
    private void showContent(javafx.scene.Node content){miniGameView.onTabUnselected();if(explorer!=null)explorer.stopAudio();for(Tab tab:tabPane.getTabs())if(tab.getContent() instanceof DriverViewController drivers)drivers.dispose();contentHost.getChildren().setAll(content);FadeTransition fade=new FadeTransition(Duration.millis(160),content);fade.setFromValue(0.6);fade.setToValue(1);fade.play();}
    @Override public void dispose(){miniGameView.onTabUnselected();bingoView.dispose();for(Tab tab:tabPane.getTabs())if(tab.getContent() instanceof DriverViewController drivers)drivers.dispose();if(explorer!=null)explorer.dispose();}

    private void importApiData() {
        try {
            String message = dataImportController.importAllFromApi(ACTIVE_SEASON);
            raceView.setSeason(ACTIVE_SEASON);
            resultView.setSeason(ACTIVE_SEASON);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("API Import");
            alert.setHeaderText("Jolpica data import finished for season " + ACTIVE_SEASON);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (ControllerException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("API Import Error");
            alert.setHeaderText("Could not import API data");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private Tab createTab(String title, BorderPane content) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setTooltip(new Tooltip(title + " Module"));
        tab.setContent(content);
        return tab;
    }
}
