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
        analysisTabs.getTabs().add(1,new Tab("Comparador",comparison));
        analysisTabs.getTabs().add(2,new Tab("Neumáticos",tyres));
        analysisTabs.getTabs().add(3,new Tab("Pits",pits));
        analysisTabs.getTabs().add(4,new Tab("Posiciones",positions));
        javafx.scene.control.ScrollPane simulationScroll=new javafx.scene.control.ScrollPane(simulation);simulationScroll.setFitToWidth(true);simulation.setPrefHeight(720);simulation.setMinHeight(650);
        analysisTabs.getTabs().add(5,new Tab("Simulador",simulationScroll));
        javafx.scene.control.Label intro=new javafx.scene.control.Label("Historia de carrera · observaciones y reglas verificables. Selecciona un evento para leer su evidencia; no se atribuye causalidad automática a una parada.");intro.setWrapText(true);intro.getStyleClass().add("explanation-card");
        javafx.scene.layout.BorderPane narrativePane=new javafx.scene.layout.BorderPane(narrative,intro,null,null,null);analysisTabs.getTabs().add(6,new Tab("Narrativa",narrativePane));
        narrative.setCellFactory(v->new javafx.scene.control.ListCell<>(){protected void updateItem(model.history.RaceInsights.Insight i,boolean empty){super.updateItem(i,empty);setText(empty||i==null?null:i.toString());setWrapText(true);setPrefWidth(0);setStyle("-fx-padding: 13; -fx-border-color: transparent transparent #26344a transparent;");}});
    }
    @Override protected void onRaceChanged(){if(data==null)return;comparison.setData(data,seasonData);tyres.setData(data);pits.setData(data);positions.setData(data);simulation.setData(data);narrative.getItems().setAll(new model.history.RaceInsights().narrate(data));}
}
