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

public final class PitViewController extends BorderPane {
    @FXML ComboBox<Entry> left,right;
    @FXML TableView<PitRow> pitTable;
    @FXML BarChart<String,Number> pitChart;
    @FXML Label detail;
    private RaceData data;private boolean updating;private final HistoryAnalysis analysis=new HistoryAnalysis();
    record PitRow(String driver,Pit pit,Integer before,Integer after){}
    public PitViewController(){FXMLLoader f=new FXMLLoader(getClass().getResource("/view/PitView.fxml"));f.setRoot(this);f.setController(this);try{f.load();}catch(IOException e){throw new IllegalStateException(e);}}
    @FXML void initialize(){left.setOnAction(e->render());right.setOnAction(e->render());col("Piloto",PitRow::driver);col("Vuelta",r->""+r.pit().lap());col("Pit lane / fuente (s)",r->Timing.number(r.pit().laneSeconds()));col("Estacionario (s)",r->Timing.number(r.pit().stationarySeconds()));col("P antes (v−1)",r->r.before()==null?"—":"P"+r.before());col("P después (v+1)",r->r.after()==null?"—":"P"+r.after());col("Qué mide",r->r.pit().durationMeaning());pitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);pitTable.setPlaceholder(new Label("Sin paradas disponibles para esta selección."));pitChart.setAnimated(false);pitChart.setLegendVisible(false);}
    private void col(String title,java.util.function.Function<PitRow,String> value){TableColumn<PitRow,String> c=new TableColumn<>(title);c.setCellValueFactory(x->new SimpleStringProperty(value.apply(x.getValue())));c.setPrefWidth(title.equals("Qué mide")?250:120);pitTable.getColumns().add(c);}
    public void setData(RaceData d){data=d;updating=true;left.getItems().setAll(d.raceEntries());right.getItems().setAll(d.raceEntries());if(!d.raceEntries().isEmpty())left.setValue(d.raceEntries().get(0));if(d.raceEntries().size()>1)right.setValue(d.raceEntries().get(1));updating=false;render();}
    private void render(){if(updating||data==null)return;List<PitRow> rows=new ArrayList<>();pitChart.getData().clear();XYChart.Series<String,Number> bars=new XYChart.Series<>();
        boolean available=data.sources().stream().anyMatch(s->Set.of("PITS","OPEN_PITS").contains(s.kind()));
        if(!available){pitTable.getItems().clear();detail.setText("No hay un documento de paradas preparado para esta carrera. Pulsa Preparar vueltas y estrategia. La ausencia de registros no significa que un piloto hiciera cero paradas.");return;}
        for(Entry e:Arrays.asList(left.getValue(),right.getValue()).stream().filter(Objects::nonNull).distinct().toList()){
            var stops=data.pits().stream().filter(p->p.driverId().equals(e.driverId())).sorted(Comparator.comparingInt(Pit::lap)).toList();for(Pit p:stops)rows.add(new PitRow(e.driver(),p,analysis.position(data,e.driverId(),p.lap()-1),analysis.position(data,e.driverId(),p.lap()+1)));bars.getData().add(new XYChart.Data<>(e.driver(),stops.size()));
        }
        pitTable.setItems(FXCollections.observableArrayList(rows));pitChart.getData().add(bars);
        detail.setText((left.getValue()!=null&&right.getValue()!=null?analysis.pitExplanation(data,left.getValue().driverId(),right.getValue().driverId()):"Selecciona dos pilotos.")+"\nTiempo estacionario, tránsito por pit lane y pérdida neta son magnitudes distintas. Los campos — son desconocidos; no se sustituyen por cero. El tiempo estacionario de OpenF1 solo está disponible desde EE. UU. 2024.");
    }
}
