package controller;

import java.io.IOException;
import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import model.history.*;
import static model.history.RaceData.*;

public final class ComparisonViewController extends BorderPane {
    @FXML ComboBox<String> mode;
    @FXML ComboBox<Choice> left,right;
    @FXML TableView<Metric> metrics;
    @FXML TableView<HistoryAnalysis.RaceComparison> racesTable;
    @FXML LineChart<Number,Number> pointsChart;
    @FXML Label explanation,paceLabel;
    private final HistoryAnalysis analysis=new HistoryAnalysis();
    private List<RaceData> season=List.of();private RaceData race;private boolean updating;
    record Choice(String id,String name){public String toString(){return name;}}
    record Metric(String label,String a,String b){}
    public ComparisonViewController(){FXMLLoader f=new FXMLLoader(getClass().getResource("/view/ComparisonView.fxml"));f.setRoot(this);f.setController(this);try{f.load();}catch(IOException e){throw new IllegalStateException(e);}}
    @FXML void initialize(){
        mode.getItems().setAll("Pilotos","Escuderías");mode.setValue("Pilotos");mode.setOnAction(e->choices());left.setOnAction(e->render());right.setOnAction(e->render());
        col("Métrica",Metric::label);col("Selección A",Metric::a);col("Selección B",Metric::b);
        raceCol("Gran Premio",HistoryAnalysis.RaceComparison::race);raceCol("A salida",r->p(r.gridA()));raceCol("A final",r->p(r.finishA()));raceCol("A puntos",r->f(r.pointsA()));raceCol("B salida",r->p(r.gridB()));raceCol("B final",r->p(r.finishB()));raceCol("B puntos",r->f(r.pointsB()));racesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        metrics.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        pointsChart.setCreateSymbols(true);pointsChart.setAnimated(false);
    }
    private void col(String name,java.util.function.Function<Metric,String> value){TableColumn<Metric,String> c=new TableColumn<>(name);c.setCellValueFactory(x->new SimpleStringProperty(value.apply(x.getValue())));metrics.getColumns().add(c);}
    private void raceCol(String name,java.util.function.Function<HistoryAnalysis.RaceComparison,String> value){TableColumn<HistoryAnalysis.RaceComparison,String> c=new TableColumn<>(name);c.setCellValueFactory(x->new SimpleStringProperty(value.apply(x.getValue())));racesTable.getColumns().add(c);}
    private String p(Integer n){return n==null?"—":"P"+n;}
    public void setData(RaceData race,List<RaceData> season){this.race=race;this.season=season;choices();}
    private void choices(){boolean team=mode.getValue().equals("Escuderías");Map<String,String> options=new TreeMap<>();season.forEach(d->d.raceEntries().forEach(e->options.put(team?e.teamId():e.driverId(),team?e.team():e.driver())));
        List<Choice> choices=options.entrySet().stream().map(e->new Choice(e.getKey(),e.getValue())).sorted(Comparator.comparing(Choice::name)).toList();updating=true;left.getItems().setAll(choices);right.getItems().setAll(choices);if(!choices.isEmpty())left.setValue(choices.get(0));if(choices.size()>1)right.setValue(choices.get(1));updating=false;render();}
    private void render(){if(updating)return;metrics.getItems().clear();racesTable.getItems().clear();pointsChart.getData().clear();Choice a=left.getValue(),b=right.getValue();if(a==null||b==null||a.id().equals(b.id())){explanation.setText("Selecciona dos elementos distintos con datos locales.");paceLabel.setText("Sin comparación seleccionada.");return;}
        boolean team=mode.getValue().equals("Escuderías");var x=analysis.statistics(season,a.id(),team);var y=analysis.statistics(season,b.id(),team);
        metrics.setItems(FXCollections.observableArrayList(new Metric("Puntos (carrera + sprint)",f(x.points()),f(y.points())),new Metric("Victorias (carrera)",""+x.wins(),""+y.wins()),new Metric("Podios (carrera)",""+x.podiums(),""+y.podiums()),new Metric("GP con participación",x.races()+"",y.races()+""),new Metric("Posición de salida media",f(x.grid()),f(y.grid())),new Metric("Posición final media",f(x.finish()),f(y.finish())),new Metric("Clasificación cronometrada media",f(x.qualifying()),f(y.qualifying())),new Metric("Muestras de clasificación",x.qualifyingSamples()+"",y.qualifyingSamples()+"")));
        for(Choice c:List.of(a,b)){XYChart.Series<Number,Number> series=new XYChart.Series<>();series.setName(c.name());for(var p:analysis.progress(season,c.id(),team)){var point=new XYChart.Data<Number,Number>(p.round(),p.points());series.getData().add(point);tooltip(point,"Ronda "+p.round()+" · "+f(p.points())+" puntos acumulados");}pointsChart.getData().add(series);}
        explanation.setText(analysis.compareExplanation(season,a.id(),b.id(),a.name(),b.name(),team));
        racesTable.getItems().setAll(analysis.raceComparison(season,a.id(),b.id(),team));
        if(race!=null&&!team){var p=analysis.commonPace(race,a.id(),b.id());var q=analysis.commonPace(race,b.id(),a.id());paceLabel.setText(p.median()==null||q.median()==null?"Ritmo: no hay vueltas comunes comparables en la carrera seleccionada.":"Ritmo mediano en "+p.samples()+" vueltas comunes de "+race.race().name()+": "+Timing.format(p.median())+" vs "+Timing.format(q.median())+"; diferencia A−B = "+f(p.median()-q.median())+" s. Excluye primera vuelta, entrada/salida de pits y SC/VSC identificados; no ajusta tráfico.");}
        else if(race!=null){var p=analysis.teamPace(race,a.id());var q=analysis.teamPace(race,b.id());paceLabel.setText("Ritmo mediano por equipo en esta carrera: "+Timing.format(p.median())+" ("+p.samples()+" vueltas) vs "+Timing.format(q.median())+" ("+q.samples()+"). Combina coches y contextos diferentes; no identifica rendimiento causal. En la tabla por GP, salida/final de equipo corresponden al mejor coche y puntos a la suma.");}
    }
    static String f(Double n){return n==null?"—":String.format(Locale.ROOT,"%.2f",n);}
    static void tooltip(XYChart.Data<Number,Number> point,String text){point.nodeProperty().addListener((o,a,node)->{if(node!=null)Tooltip.install(node,new Tooltip(text));});if(point.getNode()!=null)Tooltip.install(point.getNode(),new Tooltip(text));}
}
