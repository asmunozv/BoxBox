package controller;

import javafx.scene.control.Tab;

/** Coordinates a single immutable race/season context across analytical controllers. */
public final class RaceLabController extends HistoricalExplorerController {
    private final ComparisonViewController comparison=new ComparisonViewController();
    private final TyreViewController tyres=new TyreViewController();
    private final PitViewController pits=new PitViewController();
    private final PositionsViewController positions=new PositionsViewController();
    private final SimulationViewController simulation=new SimulationViewController();
    private final javafx.scene.control.ListView<model.history.RaceInsights.Insight> narrative=new javafx.scene.control.ListView<>();
    public RaceLabController(){
        analysisTabs.getTabs().add(1,new Tab("Comparador",scroll(comparison)));
        analysisTabs.getTabs().add(2,new Tab("Neumáticos",scroll(tyres)));
        analysisTabs.getTabs().add(3,new Tab("Paradas",scroll(pits)));
        analysisTabs.getTabs().add(4,new Tab("Posiciones",scroll(positions)));
        javafx.scene.control.ScrollPane simulationScroll=scroll(simulation);
        simulation.onCompared(()->javafx.application.Platform.runLater(()->{
            simulation.applyCss();simulation.layout();
            double range=simulation.getHeight()-simulationScroll.getViewportBounds().getHeight();
            if(range>0){double y=simulation.sceneToLocal(simulation.resultCard.localToScene(simulation.resultCard.getBoundsInLocal())).getMinY();simulationScroll.setVvalue(Math.max(0,Math.min(1,y/range)));}
        }));
        analysisTabs.getTabs().add(5,new Tab("Simulador",simulationScroll));
        javafx.scene.control.Label intro=new javafx.scene.control.Label("Historia de carrera · lee cada evento junto a su evidencia. Los cambios de posición pueden incluir pits, abandonos o incidentes; no se atribuye automáticamente una causa.");intro.setWrapText(true);intro.getStyleClass().add("explanation-card");
        javafx.scene.layout.BorderPane narrativePane=new javafx.scene.layout.BorderPane(narrative,intro,null,null,null);analysisTabs.getTabs().add(6,new Tab("Historia",narrativePane));
        narrative.setCellFactory(v->new javafx.scene.control.ListCell<>(){protected void updateItem(model.history.RaceInsights.Insight i,boolean empty){super.updateItem(i,empty);setText(empty||i==null?null:i.toString());setWrapText(true);setPrefWidth(0);setStyle("-fx-padding: 13; -fx-border-color: transparent transparent #26344a transparent;");}});
    }
    private javafx.scene.control.ScrollPane scroll(javafx.scene.layout.Region content){var scroll=new javafx.scene.control.ScrollPane(content);scroll.setFitToWidth(true);content.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);return scroll;}
    @Override protected void onRaceChanged(){for(int i=1;i<=6;i++)analysisTabs.getTabs().get(i).setDisable(data==null);if(data==null){if(analysisTabs.getSelectionModel().getSelectedIndex()>0&&analysisTabs.getSelectionModel().getSelectedIndex()<=6)openTab(0);narrative.getItems().clear();return;}comparison.setData(data,seasonData);tyres.setData(data);pits.setData(data);positions.setData(data);simulation.setData(data);narrative.getItems().setAll(new model.history.RaceInsights().narrate(data));}
}
