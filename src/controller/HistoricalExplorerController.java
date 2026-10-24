package controller;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.*;
import model.history.*;
import static model.history.RaceData.*;

/** JavaFX interaction only; all source access, persistence and calculations live in model. */
public class HistoricalExplorerController extends BorderPane implements view.DisposableView {
    final HistoryService service=new HistoryService();
    final HistoryImportService importer=new HistoryImportService();
    private final RaceScoring scoring=new RaceScoring();
    List<RaceInfo> calendar=List.of();
    List<RaceData> seasonData=List.of();
    RaceData data;
    private boolean selecting;
    private Task<?> activeTask;
    final BooleanProperty busy=new SimpleBooleanProperty(false);
    @FXML ComboBox<Integer> seasonCombo;
    @FXML ComboBox<RaceInfo> raceCombo;
    @FXML ComboBox<String> driverFilter,teamFilter,sessionFilter;
    @FXML TextField searchField;
    @FXML Button seasonImportButton,raceImportButton,demoButton;
    @FXML ProgressIndicator loading;
    @FXML Label statusLabel,titleLabel,circuitLabel,coverageLabel,winnerValue,entriesValue,lapsValue,qualifyingValue,summaryLabel,pointsLabel;
    @FXML TableView<Entry> resultsTable;
    @FXML ListView<String> sourcesList;
    @FXML TabPane analysisTabs;

    public HistoricalExplorerController(){
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/view/HistoricalExplorer.fxml"));loader.setRoot(this);loader.setController(this);
        try{loader.load();}catch(IOException e){throw new IllegalStateException("No se pudo abrir el explorador histórico.",e);}
    }
    @FXML private void initialize(){
        loading.visibleProperty().bind(busy);loading.managedProperty().bind(busy);
        for(Control c:List.of(seasonCombo,raceCombo,seasonImportButton,raceImportButton,demoButton))c.disableProperty().bind(busy);
        for(int year=LocalDate.now().getYear();year>=1950;year--)seasonCombo.getItems().add(year);
        sessionFilter.getItems().setAll("Carrera","Sprint");sessionFilter.setValue("Carrera");
        driverFilter.getItems().setAll("Todos los pilotos");driverFilter.setValue("Todos los pilotos");
        teamFilter.getItems().setAll("Todas las escuderías");teamFilter.setValue("Todas las escuderías");
        column("Pos.",e->e.position()==null?"—":"P"+e.position(),55);
        column("Piloto",Entry::driver,170);column("Escudería",Entry::team,150);
        column("Salida",e->e.grid()==null?"—":e.grid()==0?"Pit lane":"P"+e.grid(),75);
        column("Puntos · VR = extra",e->scoring.explain(data.race(),e).label(),135);
        column("Clasif.",e->e.qualifying()==null?"—":"P"+e.qualifying(),65);
        column("Q1",e->Timing.format(e.q1()),90);column("Q2",e->Timing.format(e.q2()),90);column("Q3",e->Timing.format(e.q3()),90);
        column("Vueltas",e->""+e.completedLaps(),60);column("Estado",Entry::status,130);
        resultsTable.setPlaceholder(new Label("Sin resultados locales para esta selección. Prepara la carrera para comenzar."));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resultsTable.getSelectionModel().selectedItemProperty().addListener((o,a,b)->showPoints(b));
        for(ComboBox<?> box:List.of(driverFilter,teamFilter,sessionFilter))box.setOnAction(e->filter());
        searchField.textProperty().addListener((o,a,b)->filter());
        seasonCombo.setOnAction(e->{if(!selecting)selectSeason();});raceCombo.setOnAction(e->{if(!busy.get()&&!selecting)loadRace();});
        seasonImportButton.setOnAction(e->prepareSeason());raceImportButton.setOnAction(e->prepareRace());
        demoButton.setOnAction(e->job("Cargando demostración local, sin HTTP…",()->new DemoArchive().importFile(java.nio.file.Path.of("datasets/demo-2024.json")),count->{service.invalidate();selecting=true;seasonCombo.setValue(2024);raceCombo.setValue(null);selecting=false;refresh();}));
        refresh();
    }
    void column(String title,Function<Entry,String> value,double width){TableColumn<Entry,String> c=new TableColumn<>(title);c.setPrefWidth(width);c.setCellValueFactory(x->new SimpleStringProperty(value.apply(x.getValue())));resultsTable.getColumns().add(c);}
    void refresh(){
        job("Consultando archivo local…",()->service.races(),rows->{calendar=rows;Integer previous=seasonCombo.getValue();selecting=true;seasonCombo.setValue(previous!=null?previous:rows.isEmpty()?LocalDate.now().getYear():rows.get(0).season());selecting=false;selectSeason();});
    }
    private void selectSeason(){if(busy.get()||seasonCombo.getValue()==null)return;int year=seasonCombo.getValue();RaceInfo previous=raceCombo.getValue();List<RaceInfo> races=calendar.stream().filter(r->r.season()==year).sorted(Comparator.comparingInt(RaceInfo::round)).toList();selecting=true;raceCombo.getItems().setAll(races);if(!races.isEmpty())raceCombo.setValue(races.stream().filter(r->previous!=null&&r.key().equals(previous.key())).findFirst().orElse(races.get(0)));selecting=false;if(!races.isEmpty())loadRace();else{data=null;resultsTable.getItems().clear();titleLabel.setText("Prepara una temporada histórica");circuitLabel.setText("Selecciona un año y pulsa Preparar temporada. Después, la navegación funciona con MySQL local.");coverageLabel.setText("Sin calendario local para "+year);resetMetrics();}}
    private void loadRace(){RaceInfo race=raceCombo.getValue();if(race==null)return;job("Leyendo carrera y temporada desde MySQL…",()->new Loaded(service.race(race),service.season(race.season())),loaded->{data=loaded.race;seasonData=loaded.season;render();});}
    private record Loaded(RaceData race,List<RaceData> season){}
    void render(){
        titleLabel.setText(data.race().name()+" · "+data.race().season());
        circuitLabel.setText(data.race().circuit()+"  /  "+Timing.or(data.race().locality(),"—")+", "+Timing.or(data.race().country(),"—")+"  /  "+data.race().date());
        driverFilter.getItems().setAll("Todos los pilotos");driverFilter.getItems().addAll(data.raceEntries().stream().map(Entry::driver).distinct().sorted().toList());driverFilter.setValue("Todos los pilotos");
        teamFilter.getItems().setAll("Todas las escuderías");teamFilter.getItems().addAll(data.raceEntries().stream().map(Entry::team).distinct().sorted().toList());teamFilter.setValue("Todas las escuderías");
        entriesValue.setText(""+data.raceEntries().size());lapsValue.setText(""+data.lapCount());
        winnerValue.setText(data.raceEntries().stream().filter(e->Objects.equals(e.position(),1)).map(Entry::driver).findFirst().orElse("Sin resultados"));
        qualifyingValue.setText(data.raceEntries().stream().filter(e->e.qualifying()!=null).count()+" / "+data.raceEntries().size());
        long prepared=seasonData.stream().filter(d->!d.raceEntries().isEmpty()).count();
        coverageLabel.setText("Archivo local · "+prepared+"/"+seasonData.size()+" carreras con resultados en la temporada · "+data.laps().size()+" tiempos/posiciones por vuelta · "+data.stints().size()+" stints");
        sourcesList.getItems().setAll(data.sources().stream().map(s->s.kind()+" · "+s.provider()+" · "+s.fetchedAt()+"\n"+s.url()+"\nSHA-256 "+s.sha256()).toList());
        filter();onRaceChanged();
    }
    void resetMetrics(){winnerValue.setText("—");entriesValue.setText("—");lapsValue.setText("—");qualifyingValue.setText("—");summaryLabel.setText("Cada resultado procede de una instantánea verificable. Los datos faltantes se muestran como —.");pointsLabel.setText("Selecciona una carrera para consultar sus puntos oficiales.");}
    void filter(){
        if(data==null)return;String driver=driverFilter.getValue(),team=teamFilter.getValue(),search=searchField.getText()==null?"":searchField.getText().toLowerCase(Locale.ROOT).trim();String session="Sprint".equals(sessionFilter.getValue())?"SPRINT":"RACE";
        List<Entry> rows=data.entries().stream().filter(e->e.session().equals(session)).filter(e->driver==null||driver.startsWith("Todos")||e.driver().equals(driver)).filter(e->team==null||team.startsWith("Todas")||e.team().equals(team)).filter(e->(e.driver()+" "+e.team()+" "+e.code()+" "+e.status()).toLowerCase(Locale.ROOT).contains(search)).sorted(Comparator.comparing(e->e.position()==null?999:e.position())).toList();
        resultsTable.setItems(FXCollections.observableArrayList(rows));
        resultsTable.getSelectionModel().selectFirst();
        if(rows.isEmpty())showPoints(null);
        summaryLabel.setText(rows.size()+" resultados en esta selección. Parrilla y clasificación cronometrada son diferentes: las sanciones pueden cambiar la salida. Puntos conservados de la fuente; los abandonos mantienen su estado.");
    }
    private void showPoints(Entry entry){pointsLabel.setText(data==null||entry==null?"Selecciona una fila para entender el desglose de puntos.":scoring.explain(data.race(),entry).explanation());}
    protected void onRaceChanged(){}
    public void openTab(int index){analysisTabs.getSelectionModel().select(index);}
    public void prepareSeason(){Integer year=seasonCombo.getValue();if(year==null)return;importJob("Preparando temporada…",p->importer.season(year,p));}
    public void prepareRace(){RaceInfo race=raceCombo.getValue();if(race==null){statusLabel.setText("Prepara primero el calendario de la temporada.");return;}importJob("Preparando carrera…",p->importer.detail(race,p));}
    private void importJob(String message,Importer operation){
        if(busy.get())return;RaceInfo selected=raceCombo.getValue();
        Task<HistoryImportService.Report> task=new Task<>(){protected HistoryImportService.Report call()throws Exception{return operation.run(this::updateMessage);}};
        activeTask=task;
        busy.set(true);statusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e->{statusLabel.textProperty().unbind();busy.set(false);statusLabel.setText(task.getValue().message());service.invalidate();job("Actualizando archivo local…",()->service.races(),rows->{calendar=rows;selectSeason();});});
        task.setOnFailed(e->{statusLabel.textProperty().unbind();busy.set(false);statusLabel.setText("No se completó la preparación: "+safe(task.getException()));});
        Thread thread=new Thread(task,"boxbox-preparation");thread.setDaemon(true);thread.start();
    }
    @FunctionalInterface private interface Importer{HistoryImportService.Report run(Consumer<String> progress)throws Exception;}
    <T> void job(String message,Work<T> work,Consumer<T> success){
        if(busy.get())return;busy.set(true);statusLabel.setText(message);Task<T> task=new Task<>(){protected T call()throws Exception{return work.run();}};
        activeTask=task;
        task.setOnSucceeded(e->{busy.set(false);statusLabel.setText("Datos locales cargados · observado / calculado / simulado se identifican por separado.");success.accept(task.getValue());});
        task.setOnFailed(e->{busy.set(false);statusLabel.setText("No se pudo completar la operación: "+safe(task.getException()));});Thread thread=new Thread(task,"boxbox-local-read");thread.setDaemon(true);thread.start();
    }
    @FunctionalInterface interface Work<T>{T run()throws Exception;}
    private String safe(Throwable e){if(e instanceof java.sql.SQLException)return "verifica la conexión y la preparación del esquema de BoxBox.";return Timing.or(e.getMessage(),e.getClass().getSimpleName());}
    @Override public void dispose(){if(activeTask!=null&&activeTask.isRunning())activeTask.cancel(true);}
}
