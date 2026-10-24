package controller;

import java.io.IOException;
import java.util.*;
import javafx.fxml.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import model.history.*;
import static model.history.RaceData.*;

public final class PositionsViewController extends BorderPane {
    @FXML FlowPane driverChoices;
    @FXML LineChart<Number,Number> positions;
    @FXML NumberAxis positionAxis;
    @FXML Label pointDetail;
    private RaceData data;private final Map<String,Boolean> visible=new LinkedHashMap<>();
    private final String[] colors={"#ff667c","#6bc5ff","#f4cc69","#61d5ae","#bb9cff","#f89c65","#85dce1","#f493c2"};
    public PositionsViewController(){FXMLLoader f=new FXMLLoader(getClass().getResource("/view/PositionsView.fxml"));f.setRoot(this);f.setController(this);try{f.load();}catch(IOException e){throw new IllegalStateException(e);}}
    @FXML void initialize(){positions.setAnimated(false);positions.setLegendVisible(false);positionAxis.setAutoRanging(false);positionAxis.setTickUnit(1);positionAxis.setTickLabelFormatter(new StringConverter<Number>(){public String toString(Number n){return n.intValue()>=0?"":"P"+(-n.intValue());}public Number fromString(String s){return -Integer.parseInt(s.replace("P",""));}});}
    public void setData(RaceData d){data=d;driverChoices.getChildren().clear();visible.clear();int index=0;
        for(Entry e:d.raceEntries()){CheckBox choice=new CheckBox(e.code());choice.setTooltip(new Tooltip(e.driver()));choice.setSelected(index++<4);visible.put(e.driverId(),choice.isSelected());choice.setOnAction(x->{visible.put(e.driverId(),choice.isSelected());render();});driverChoices.getChildren().add(choice);}
        positionAxis.setLowerBound(-Math.max(2,d.raceEntries().size()));positionAxis.setUpperBound(-1);render();
    }
    private void render(){if(data==null)return;positions.getData().clear();int driverIndex=0;for(Entry e:data.raceEntries()){
        String color=colors[driverIndex++%colors.length];if(!visible.getOrDefault(e.driverId(),false))continue;
        List<Lap> laps=data.laps().stream().filter(l->l.driverId().equals(e.driverId())&&l.position()!=null).sorted(Comparator.comparingInt(Lap::number)).toList();
        XYChart.Series<Number,Number> series=null;int previous=-1;
        for(Lap l:laps){if(series==null||l.number()!=previous+1){series=new XYChart.Series<>();series.setName(e.code());positions.getData().add(series);styleSeries(series,color,driverIndex>8);}previous=l.number();var point=new XYChart.Data<Number,Number>(l.number(),-l.position());series.getData().add(point);
            boolean pit=data.pits().stream().anyMatch(p->p.driverId().equals(e.driverId())&&p.lap()==l.number());String text=e.driver()+" · V"+l.number()+" · P"+l.position()+(pit?" · PIT REGISTRADO":"")+" · "+Timing.format(l.seconds());
            java.util.function.Consumer<javafx.scene.Node> configure=node->{if(node!=null){Tooltip.install(node,new Tooltip(text));node.setStyle("-fx-background-color:"+(pit?"white":color)+";-fx-padding:"+(pit?"5":"3")+";");node.setOnMouseClicked(x->pointDetail.setText(text+". Posición observada al final de la vuelta del piloto; no implica un adelantamiento en pista. Un marcador blanco señala el paso por pits."));}};
            point.nodeProperty().addListener((o,a,node)->configure.accept(node));configure.accept(point.getNode());
        }
    }
        pointDetail.setText(data.laps().isEmpty()?"Sin posiciones por vuelta en esta instantánea. Prepara vueltas y estrategia; Jolpica ofrece esta cobertura desde 1996.":"P1 está arriba. Activa u oculta pilotos y selecciona puntos para leer la carrera. Marcadores blancos = pits. Los huecos y finales de serie se conservan; no se inventan posiciones tras un abandono. Las vueltas corresponden al contador de cada piloto, no a una animación sincronizada por tiempo.");
    }
    private void styleSeries(XYChart.Series<Number,Number> s,String color,boolean dashed){s.nodeProperty().addListener((o,a,n)->{if(n!=null)n.setStyle("-fx-stroke:"+color+";"+(dashed?"-fx-stroke-dash-array:8 4;":""));});if(s.getNode()!=null)s.getNode().setStyle("-fx-stroke:"+color+";");}
}
