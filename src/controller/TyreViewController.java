package controller;

import java.io.IOException;
import java.util.*;
import javafx.fxml.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.history.*;
import static model.history.RaceData.*;

public final class TyreViewController extends BorderPane {
    @FXML ComboBox<Entry> left,right;
    @FXML VBox timelines;
    @FXML LineChart<Number,Number> lapChart;
    @FXML Label detail;
    private RaceData data;private boolean updating;private final HistoryAnalysis analysis=new HistoryAnalysis();
    public TyreViewController(){FXMLLoader f=new FXMLLoader(getClass().getResource("/view/TyreView.fxml"));f.setRoot(this);f.setController(this);try{f.load();}catch(IOException e){throw new IllegalStateException(e);}}
    @FXML void initialize(){left.setOnAction(e->render());right.setOnAction(e->render());lapChart.setAnimated(false);}
    public void setData(RaceData d){data=d;updating=true;left.getItems().setAll(d.raceEntries());right.getItems().setAll(d.raceEntries());if(!d.raceEntries().isEmpty())left.setValue(d.raceEntries().get(0));if(d.raceEntries().size()>1)right.setValue(d.raceEntries().get(1));updating=false;render();}
    private void render(){if(updating||data==null)return;timelines.getChildren().clear();lapChart.getData().clear();boolean any=false;
        for(Entry e:Arrays.asList(left.getValue(),right.getValue()).stream().filter(Objects::nonNull).distinct().toList()){
            Label name=new Label(e.driver());name.getStyleClass().add("stat-name");timelines.getChildren().add(name);HBox line=new HBox(4);line.setMinHeight(60);timelines.getChildren().add(line);
            List<Stint> stints=data.stints().stream().filter(s->s.driverId().equals(e.driverId())).sorted(Comparator.comparingInt(Stint::firstLap)).toList();
            if(stints.isEmpty())line.getChildren().add(new Label("Compuestos no disponibles en la instantánea. Los tiempos se muestran si existen."));
            for(Stint s:stints){any=true;Button segment=new Button(s.compound()+"\n"+s.firstLap()+"–"+s.lastLap()+" · "+s.length()+" v");segment.setMaxWidth(Double.MAX_VALUE);segment.prefWidthProperty().bind(line.widthProperty().multiply((double)s.length()/Math.max(1,data.lapCount())));String color=color(s.compound());segment.setStyle("-fx-background-color:"+color+";-fx-text-fill:"+(s.compound().equals("HARD")||s.compound().equals("MEDIUM")?"#111827":"white")+";-fx-padding:8 5;-fx-font-size:11;");
                String text="Stint "+s.number()+": "+s.length()+" vueltas, "+s.compound()+", edad inicial "+(s.initialAge()==null?"desconocida":s.initialAge()+" vueltas")+". ";Double slope=analysis.stintTrend(data,s);text+=slope==null?"Muestra insuficiente para estimar una tendencia.":String.format(Locale.ROOT,"Pendiente observada: %+.4f s/vuelta (OLS). No separa degradación de combustible, tráfico o evolución de pista.",slope);String explanation=text;Tooltip.install(segment,new Tooltip(text));segment.setOnAction(x->detail.setText(explanation));line.getChildren().add(segment);
            }
            XYChart.Series<Number,Number> series=new XYChart.Series<>();series.setName(e.driver());
            for(Lap lap:data.laps().stream().filter(l->l.driverId().equals(e.driverId())&&l.seconds()!=null).sorted(Comparator.comparingInt(Lap::number)).toList()){var point=new XYChart.Data<Number,Number>(lap.number(),lap.seconds());series.getData().add(point);ComparisonViewController.tooltip(point,e.driver()+" · V"+lap.number()+" · "+Timing.format(lap.seconds()));}
            lapChart.getData().add(series);
        }
        detail.setText(any?"Un stint es una tanda con un juego de neumáticos. Selecciona una banda para ver edad, duración y tendencia. Los picos incluyen paradas; una pendiente de tiempos no demuestra degradación causal.":"La fuente no proporciona stints para esta carrera. Jolpica ofrece tiempos/posiciones según cobertura; OpenF1 ofrece neumáticos desde 2023. No se infieren compuestos faltantes.");
    }
    public static String color(String compound){return switch(compound){case "SOFT"->"#e94d63";case "MEDIUM"->"#f4c95d";case "HARD"->"#e5eaf2";case "INTERMEDIATE"->"#36b883";case "WET"->"#3285db";default->"#68758d";};}
}
