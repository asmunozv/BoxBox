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
import static model.history.StrategySimulator.*;

/** Configures experiments and presents results; all mathematical work remains in the model. */
public final class SimulationViewController extends BorderPane {
    @FXML ComboBox<Entry> driver;
    @FXML ComboBox<String> initial;
    @FXML Spinner<Integer> age;
    @FXML TextField stops,degradation,pitLoss,fuel,warmup,softEffect,mediumEffect,hardEffect;
    @FXML Button calculate,earlier,reset,addStop,noStops;
    @FXML Label historicalLabel,evidence,resultLabel,limitations,deltaValue,interpretation,diagnostics;
    @FXML VBox stopRows;
    @FXML VBox resultCard;
    @FXML LineChart<Number,Number> deltaChart;
    private RaceData data;
    private boolean updating;
    private Runnable onCompared=()->{};
    private final List<StopControls> editors=new ArrayList<>();
    private final StrategySimulator model=new StrategySimulator();
    private final HistoryAnalysis analysis=new HistoryAnalysis();

    private record StopControls(Spinner<Integer> lap,ComboBox<String> compound,Spinner<Integer> age) {
        Stop value(){return new Stop(lap.getValue(),compound.getValue(),age.getValue());}
    }

    public SimulationViewController() {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/view/SimulationView.fxml"));
        loader.setRoot(this);loader.setController(this);
        try{loader.load();}catch(IOException e){throw new IllegalStateException(e);}
    }

    @FXML void initialize() {
        initial.getItems().setAll("SOFT","MEDIUM","HARD");initial.setConverter(compoundNames());
        age.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,200,0));
        driver.setOnAction(e->defaults());calculate.setOnAction(e->{run();onCompared.run();});reset.setOnAction(e->defaults());
        earlier.setOnAction(e->edit(()->{
            Strategy current=alternative();
            if(current.stops().isEmpty())throw new IllegalArgumentException("Añade una parada antes de adelantarla.");
            List<Stop> changed=new ArrayList<>(current.stops());Stop first=changed.get(0);
            changed.set(0,new Stop(Math.max(1,first.afterLap()-3),first.compound(),first.age()));
            apply(new Strategy(current.initialCompound(),current.initialAge(),changed));run();onCompared.run();
        }));
        noStops.setOnAction(e->{apply(new Strategy(initial.getValue(),age.getValue(),List.of()));run();onCompared.run();});
        addStop.setOnAction(e->edit(()->{
            Strategy current=alternative();int last=current.stops().isEmpty()?0:current.stops().getLast().afterLap();
            int end=driver.getValue().completedLaps();
            if(last>=end-1)throw new IllegalArgumentException("No queda una vuelta posterior para añadir otra parada.");
            List<Stop> changed=new ArrayList<>(current.stops());
            changed.add(new Stop(last+Math.max(1,(end-last)/2),"HARD",0));
            apply(new Strategy(current.initialCompound(),current.initialAge(),changed));
        }));
        for(TextField field:List.of(stops,degradation,pitLoss,fuel,warmup,softEffect,mediumEffect,hardEffect))field.textProperty().addListener((o,a,b)->dirty());
        initial.valueProperty().addListener((o,a,b)->dirty());age.valueProperty().addListener((o,a,b)->dirty());
        deltaChart.setAnimated(false);enabled(false);
    }

    public void setData(RaceData race) {
        data=race;updating=true;driver.getItems().setAll(race.raceEntries());
        driver.setValue(race.raceEntries().isEmpty()?null:race.raceEntries().getFirst());
        updating=false;defaults();
    }
    public void onCompared(Runnable action){onCompared=action;}

    private void defaults() {
        if(updating||data==null)return;
        deltaChart.getData().clear();deltaValue.setText("—");diagnostics.setText("");enabled(false);
        try {
            Entry selected=driver.getValue();
            if(selected==null)throw new IllegalArgumentException("No hay un piloto con resultados locales.");
            Strategy history=model.historical(data,selected.driverId());
            if(!Set.of("SOFT","MEDIUM","HARD").contains(history.initialCompound())
                ||history.stops().stream().anyMatch(s->!Set.of("SOFT","MEDIUM","HARD").contains(s.compound())))
                throw new IllegalArgumentException("Este modelo usa compuestos de seco. Los stints de lluvia se conservan en Neumáticos.");
            updating=true;apply(history);
            var estimates=analysis.estimates(data,selected.driverId());
            degradation.setText(n(estimates.degradation()));pitLoss.setText(n(estimates.pitLoss()));
            fuel.setText("0");warmup.setText("0");softEffect.setText(n(estimates.effects().get("SOFT")));
            mediumEffect.setText(n(estimates.effects().get("MEDIUM")));hardEffect.setText(n(estimates.effects().get("HARD")));
            historicalLabel.setText("HISTÓRICA OBSERVADA · "+describe(history)+" · "+selected.completedLaps()+" vueltas. Tiempo oficial: "+Timing.format(selected.totalSeconds()));
            evidence.setText(estimates.evidence());enabled(true);updating=false;run();
        } catch(IllegalArgumentException e) {
            stopRows.getChildren().clear();editors.clear();
            historicalLabel.setText("Falta detalle para reconstruir la estrategia de este piloto.");
            resultLabel.setText("Sin simulación disponible para esta selección.");
            interpretation.setText("Prepara vueltas y estrategia si la fuente las ofrece. La demo detallada es Baréin 2024.");
            evidence.setText(e.getMessage());
        } finally {updating=false;}
        limitations.setText("SIMULACIÓN EDUCATIVA · Ambas estrategias usan los mismos parámetros. No modela tráfico, adelantamientos, respuestas de rivales, lluvia, SC/VSC ni fallos. La distancia es la recorrida realmente por el piloto; no supone que un abandonado termine. No se estima posición final. En seco, un solo compuesto puede incumplir el reglamento; se permite como experimento etiquetado. Fórmula: t(v)=base+efecto del compuesto+degradación·edad−combustible·(v−1)+calentamiento; se suma la pérdida neta por parada.");
    }

    private Strategy alternative(){return model.parseAlternative(initial.getValue(),age.getValue(),stops.getText());}

    private void apply(Strategy strategy) {
        initial.setValue(strategy.initialCompound());age.getValueFactory().setValue(strategy.initialAge());
        stops.setText(String.join(";",strategy.stops().stream().map(s->s.afterLap()+":"+s.compound()+":"+s.age()).toList()));
        rows(strategy.stops());
    }

    private void rows(List<Stop> stops) {
        editors.clear();stopRows.getChildren().clear();
        if(stops.isEmpty())stopRows.getChildren().add(new Label("Alternativa sin paradas. Puedes añadir una con el botón de abajo."));
        for(Stop stop:stops) {
            Spinner<Integer> lap=new Spinner<>(1,Math.max(1,driver.getValue().completedLaps()-1),stop.afterLap());lap.setPrefWidth(82);
            ComboBox<String> compound=new ComboBox<>();compound.getItems().setAll("SOFT","MEDIUM","HARD");
            compound.setConverter(compoundNames());compound.setValue(stop.compound());compound.setPrefWidth(165);
            Spinner<Integer> tyreAge=new Spinner<>(0,Math.max(200,stop.age()),stop.age());tyreAge.setPrefWidth(78);
            StopControls controls=new StopControls(lap,compound,tyreAge);editors.add(controls);
            lap.setAccessibleText("Vuelta de la parada "+editors.size());compound.setAccessibleText("Compuesto de la parada "+editors.size());tyreAge.setAccessibleText("Edad del neumático de la parada "+editors.size());
            Button remove=new Button("Quitar");remove.setOnAction(e->apply(new Strategy(initial.getValue(),age.getValue(),editors.stream().filter(r->r!=controls).map(StopControls::value).toList())));
            FlowPane row=new FlowPane(8,6,new Label("Parada "+editors.size()+" · tras vuelta"),lap,compound,new Label("Edad del juego"),tyreAge,remove);
            row.getStyleClass().add("stop-editor-row");stopRows.getChildren().add(row);
            lap.valueProperty().addListener((o,a,b)->storeRows());compound.valueProperty().addListener((o,a,b)->storeRows());tyreAge.valueProperty().addListener((o,a,b)->storeRows());
        }
    }

    private void storeRows() {
        stops.setText(String.join(";",editors.stream().map(StopControls::value).map(s->s.afterLap()+":"+s.compound()+":"+s.age()).toList()));
    }

    private void run() {
        if(data==null||driver.getValue()==null||calculate.isDisable())return;
        edit(()->{
            Entry selected=driver.getValue();Strategy history=model.historical(data,selected.driverId()),alternative=alternative();
            var effects=Map.of("SOFT",value(softEffect),"MEDIUM",value(mediumEffect),"HARD",value(hardEffect));
            double d=value(degradation),f=value(fuel),loss=value(pitLoss),warm=value(warmup);
            if(f<0)throw new IllegalArgumentException("La tendencia de combustible debe ser no negativa.");
            var assumptions=new Parameters(1,effects,d,f,loss,warm,0);
            double base=model.calibratedBase(data,selected.driverId(),assumptions);
            var comparison=model.compare(selected.completedLaps(),history,alternative,new Parameters(base,effects,d,f,loss,warm,0));
            rows(alternative.stops());deltaChart.getData().clear();
            XYChart.Series<Number,Number> reference=new XYChart.Series<>(),scenario=new XYChart.Series<>();
            reference.setName("Histórica modelada · referencia");scenario.setName("Alternativa − histórica modelada");
            for(int i=0;i<comparison.alternative().cumulative().size();i++) {
                reference.getData().add(new XYChart.Data<>(i+1,0));
                double delta=comparison.alternative().cumulative().get(i)-comparison.historical().cumulative().get(i);
                var point=new XYChart.Data<Number,Number>(i+1,delta);scenario.getData().add(point);
                ComparisonViewController.tooltip(point,"V"+(i+1)+" · diferencia estimada "+n(delta)+" s");
            }
            deltaChart.getData().addAll(reference,scenario);
            deltaValue.setText(String.format(Locale.ROOT,"%+.3f s",comparison.delta()));
            String meaning=Math.abs(comparison.delta())<0.0005?"Las estrategias tienen el mismo tiempo en el modelo. Cambia una parada para experimentar.":"En este modelo, la alternativa "+(comparison.delta()<0?"ahorraría ":"perdería ")+n(Math.abs(comparison.delta()))+" segundos frente a la histórica modelada.";
            boolean uncertain=comparison.sensitivityLow()<0&&comparison.sensitivityHigh()>0;
            Set<String> compounds=new HashSet<>();compounds.add(alternative.initialCompound());alternative.stops().forEach(s->compounds.add(s.compound()));
            interpretation.setText(meaning+(uncertain?" El signo cambia al variar los supuestos: la conclusión es sensible.":"")+(compounds.size()<2?" Experimento con un solo compuesto: puede incumplir el reglamento de seco.":""));
            resultLabel.setText("ESTIMADO · histórica modelada "+Timing.format(comparison.historical().total())+" · alternativa "+Timing.format(comparison.alternative().total())+". Negativo = alternativa más rápida; positivo = más lenta.");
            var error=analysis.modelError(data,selected.driverId(),comparison.historical().lapSeconds());
            diagnostics.setText("Base calibrada: "+n(base)+" s. Error absoluto medio descriptivo: "+(error.meanAbsolute()==null?"—":n(error.meanAbsolute()))+" s en "+error.samples()+" vueltas de calibración; no es validación independiente. Sensibilidad al variar ±20% degradación y pérdida de pits: ["+n(comparison.sensitivityLow())+", "+n(comparison.sensitivityHigh())+"] s; no es un intervalo de confianza.");
        });
    }

    private void dirty() {
        if(updating||calculate.isDisable())return;
        deltaValue.setText("—");deltaChart.getData().clear();diagnostics.setText("");
        resultLabel.setText("Cambios pendientes · pulsa Comparar escenarios.");interpretation.setText("La comparación se actualizará con tus nuevas decisiones.");
    }

    private void edit(Runnable action) {
        try{action.run();}catch(IllegalArgumentException e){deltaValue.setText("—");resultLabel.setText("Escenario no calculado: "+e.getMessage());interpretation.setText("Corrige los parámetros y vuelve a comparar.");diagnostics.setText("");deltaChart.getData().clear();}
    }

    private void enabled(boolean enabled) {
        for(Control control:List.of(calculate,earlier,reset,addStop,noStops,initial,age,degradation,pitLoss))control.setDisable(!enabled);
    }

    private String describe(Strategy strategy) {
        return compoundNames().toString(strategy.initialCompound())+" (edad "+strategy.initialAge()+")"
            +String.join("",strategy.stops().stream().map(s->" → V"+s.afterLap()+": "+compoundNames().toString(s.compound())+" (edad "+s.age()+")").toList());
    }

    private StringConverter<String> compoundNames() {
        return new StringConverter<>() {
            public String toString(String value){return value==null?"":switch(value){case "SOFT"->"Blandos · rojo";case "MEDIUM"->"Medios · amarillo";case "HARD"->"Duros · blanco";default->value;};}
            public String fromString(String value){return value;}
        };
    }

    private double value(TextField field) {
        try{double value=Double.parseDouble(field.getText().trim().replace(',','.'));if(!Double.isFinite(value))throw new NumberFormatException();return value;}
        catch(NumberFormatException e){throw new IllegalArgumentException("Introduce números finitos. Puedes usar punto o coma decimal.");}
    }
    private String n(double value){return String.format(Locale.ROOT,"%.3f",value);}
}
