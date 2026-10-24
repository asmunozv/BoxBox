package controller;

import java.io.IOException;
import java.util.*;
import javafx.fxml.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import model.history.*;
import static model.history.RaceData.*;

public final class SimulationViewController extends BorderPane {
    @FXML ComboBox<Entry> driver;
    @FXML ComboBox<String> initial;
    @FXML Spinner<Integer> age;
    @FXML TextField stops,degradation,pitLoss,fuel,warmup,softEffect,mediumEffect,hardEffect;
    @FXML Button calculate;
    @FXML Label historicalLabel,evidence,resultLabel,limitations;
    @FXML LineChart<Number,Number> deltaChart;
    private RaceData data;private boolean updating;
    private final StrategySimulator model=new StrategySimulator();private final HistoryAnalysis analysis=new HistoryAnalysis();
    public SimulationViewController(){FXMLLoader f=new FXMLLoader(getClass().getResource("/view/SimulationView.fxml"));f.setRoot(this);f.setController(this);try{f.load();}catch(IOException e){throw new IllegalStateException(e);}}
    @FXML void initialize(){initial.getItems().setAll("SOFT","MEDIUM","HARD");age.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,200,0));driver.setOnAction(e->defaults());calculate.setOnAction(e->run());deltaChart.setAnimated(false);}
    public void setData(RaceData d){data=d;updating=true;driver.getItems().setAll(d.raceEntries());if(!d.raceEntries().isEmpty())driver.setValue(d.raceEntries().get(0));updating=false;defaults();}
    private void defaults(){if(updating||data==null)return;deltaChart.getData().clear();resultLabel.setText("Configura un escenario para comparar estimaciones.");Entry selected=driver.getValue();
        try{
            if(selected==null)throw new IllegalArgumentException("No hay un piloto con resultados locales.");
            var history=model.historical(data,selected.driverId());
            if(!Set.of("SOFT","MEDIUM","HARD").contains(history.initialCompound())||history.stops().stream().anyMatch(s->!Set.of("SOFT","MEDIUM","HARD").contains(s.compound())))throw new IllegalArgumentException("El modelo inicial se limita a compuestos de seco. Los stints observados se conservan en Neumáticos.");
            initial.setValue(history.initialCompound());age.getValueFactory().setValue(history.initialAge());stops.setText(String.join(";",history.stops().stream().map(s->s.afterLap()+":"+s.compound()+":"+s.age()).toList()));
            var estimates=analysis.estimates(data,selected.driverId());degradation.setText(n(estimates.degradation()));pitLoss.setText(n(estimates.pitLoss()));fuel.setText("0");warmup.setText("0");softEffect.setText(n(estimates.effects().get("SOFT")));mediumEffect.setText(n(estimates.effects().get("MEDIUM")));hardEffect.setText(n(estimates.effects().get("HARD")));
            historicalLabel.setText("Observado: "+history.initialCompound()+" (edad "+history.initialAge()+") → "+stops.getText()+" · "+selected.completedLaps()+" vueltas del piloto. Tiempo total oficial disponible: "+Timing.format(selected.totalSeconds()));
            evidence.setText(estimates.evidence());calculate.setDisable(false);run();
        }catch(IllegalArgumentException e){calculate.setDisable(true);historicalLabel.setText("La estrategia histórica no está reconstruida completamente.");evidence.setText(e.getMessage()+" Prepara vueltas y estrategia si la fuente ofrece estos datos.");}
        limitations.setText("SIMULACIÓN EDUCATIVA · t(v)=base+efecto_compuesto+d·edad−f·(v−1)+calentamiento; se añade pérdida neta por parada. Se comparan ambas estrategias con los mismos parámetros. No modela tráfico, adelantamientos, reacciones de rivales, lluvia, SC/VSC ni fallos. La distancia es la realmente recorrida por el piloto; no se predice que un abandonado termine la carrera. No se estima posición final. Una estrategia de un solo compuesto puede incumplir reglas de seco; aquí se permite como experimento etiquetado.");
    }
    private void run(){if(data==null||driver.getValue()==null||calculate.isDisable())return;
        try{
            Entry selected=driver.getValue();var history=model.historical(data,selected.driverId());var alternative=model.parseAlternative(initial.getValue(),age.getValue(),stops.getText());
            var effects=Map.of("SOFT",value(softEffect),"MEDIUM",value(mediumEffect),"HARD",value(hardEffect));
            double d=value(degradation),f=value(fuel),loss=value(pitLoss),warm=value(warmup);if(f<0)throw new IllegalArgumentException("La tendencia de combustible debe ser no negativa.");
            var assumptions=new StrategySimulator.Parameters(1,effects,d,f,loss,warm,0);double base=model.calibratedBase(data,selected.driverId(),assumptions);
            var parameters=new StrategySimulator.Parameters(base,effects,d,f,loss,warm,0);
            var comparison=model.compare(selected.completedLaps(),history,alternative,parameters);
            deltaChart.getData().clear();XYChart.Series<Number,Number> reference=new XYChart.Series<>(),scenario=new XYChart.Series<>();reference.setName("Histórica modelada · referencia");scenario.setName("Alternativa − histórica modelada");
            for(int i=0;i<comparison.alternative().cumulative().size();i++){reference.getData().add(new XYChart.Data<>(i+1,0));double delta=comparison.alternative().cumulative().get(i)-comparison.historical().cumulative().get(i);var point=new XYChart.Data<Number,Number>(i+1,delta);scenario.getData().add(point);ComparisonViewController.tooltip(point,"V"+(i+1)+" · diferencia estimada "+n(delta)+" s");}deltaChart.getData().addAll(reference,scenario);
            String sensitivity="Rango de sensibilidad al variar ±20% degradación y pérdida de pits: ["+n(comparison.sensitivityLow())+", "+n(comparison.sensitivityHigh())+"] s. No es un intervalo de confianza.";
            double error=0;int samples=0;for(Lap lap:analysis.cleanLaps(data,selected.driverId()))if(lap.number()<=comparison.historical().lapSeconds().size()){error+=Math.abs(lap.seconds()-comparison.historical().lapSeconds().get(lap.number()-1));samples++;}
            Set<String> compounds=new HashSet<>();compounds.add(alternative.initialCompound());alternative.stops().forEach(s->compounds.add(s.compound()));
            resultLabel.setText("ESTIMADO · histórica "+Timing.format(comparison.historical().total())+"; alternativa "+Timing.format(comparison.alternative().total())+"; Δ = "+n(comparison.delta())+" s (negativo = alternativa más rápida en el modelo).\nBase calibrada "+n(base)+" s con vueltas filtradas. Error absoluto medio descriptivo: "+(samples==0?"—":n(error/samples))+" s en "+samples+" vueltas usadas para calibración; no es validación independiente.\n"+sensitivity+(comparison.sensitivityLow()<0&&comparison.sensitivityHigh()>0?" El signo cambia: la conclusión no es robusta a estos supuestos.":"")+(compounds.size()<2?" Advertencia reglamentaria: alternativa con un único compuesto.":""));
        }catch(IllegalArgumentException e){resultLabel.setText("Escenario no calculado: "+e.getMessage());deltaChart.getData().clear();}
    }
    private double value(TextField field){try{double x=Double.parseDouble(field.getText().trim());if(!Double.isFinite(x))throw new NumberFormatException();return x;}catch(NumberFormatException e){throw new IllegalArgumentException("Introduce parámetros numéricos finitos, usando punto decimal.");}}
    private String n(double x){return String.format(Locale.ROOT,"%.3f",x);}
}
