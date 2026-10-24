package controller;

import javafx.scene.control.Tab;

/** Coordinates a single immutable race/season context across analytical controllers. */
public final class RaceLabController extends HistoricalExplorerController {
    private final ComparisonViewController comparison=new ComparisonViewController();
    private final TyreViewController tyres=new TyreViewController();
    private final PitViewController pits=new PitViewController();
    private final PositionsViewController positions=new PositionsViewController();
    private final SimulationViewController simulation=new SimulationViewController();
    public RaceLabController(){
        analysisTabs.getTabs().add(1,new Tab("Comparador",comparison));
        analysisTabs.getTabs().add(2,new Tab("Neumáticos",tyres));
        analysisTabs.getTabs().add(3,new Tab("Pits",pits));
        analysisTabs.getTabs().add(4,new Tab("Posiciones",positions));
        javafx.scene.control.ScrollPane simulationScroll=new javafx.scene.control.ScrollPane(simulation);simulationScroll.setFitToWidth(true);simulation.setPrefHeight(900);simulation.setMinHeight(800);
        analysisTabs.getTabs().add(5,new Tab("Simulador",simulationScroll));
    }
    @Override protected void onRaceChanged(){if(data==null)return;comparison.setData(data,seasonData);tyres.setData(data);pits.setData(data);positions.setData(data);simulation.setData(data);}
}
