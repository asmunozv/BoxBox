package controller;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import model.game.*;
import static model.game.GameRules.*;
import view.AppAudio;
import view.game.GrandPrixRenderer;

/** Game-only coordinator: keyboard/lifecycle, snapshots and presentation. Race rules live in model.game. */
public class MiniGameViewController extends BorderPane implements view.DisposableView {
    private enum Mode {
        GRAND_PRIX("Gran Premio"),CHAMPIONSHIP("Campeonato · 3 GP"),ARCADE("Arcade clásico");
        private final String label;Mode(String label){this.label=label;}public String toString(){return label;}
    }
    private record TeamColor(String name,Color color){public String toString(){return name;}}
    @FXML private Canvas canvas;
    @FXML private StackPane canvasHost;
    @FXML private ComboBox<TeamColor> teamCombo;
    @FXML private ComboBox<Mode> modeCombo;
    @FXML private ComboBox<Difficulty> difficultyCombo;
    @FXML private ComboBox<Integer> lapsCombo,speedCombo;
    @FXML private ComboBox<Track> trackCombo;
    @FXML private ComboBox<WeatherPlan> weatherCombo;
    @FXML private ComboBox<TyreCompound> startingTyreCombo,pitTyreCombo;
    @FXML private ComboBox<DrivingStyle> styleCombo;
    @FXML private ComboBox<EnergyMode> energyCombo;
    @FXML private CheckBox assistCheck;
    @FXML private ToggleButton drsButton;
    @FXML private Button startButton,pauseButton,nextButton,pitButton,nextResultButton,setupButton;
    @FXML private Label statusLabel,sessionLabel,lapLabel,positionLabel,clockLabel,lapTimeLabel,flagLabel,weatherLabel;
    @FXML private Label tyreLabel,temperatureLabel,fuelLabel,energyLabel,speedLabel,drsLabel,pitLabel,penaltyLabel;
    @FXML private Label resultSummary,progressLabel,championshipTitle,controlsHint;
    @FXML private ProgressBar tyreBar,fuelBar,energyBar;
    @FXML private TableView<RaceSnapshot.Car> timingTable,resultsTable;
    @FXML private TableView<GameProgress.Standing> championshipTable;
    @FXML private ListView<String> eventsList;
    @FXML private VBox resultsPane,towerPane;
    @FXML private ScrollPane gameScroll;
    @FXML private FlowPane liveHud,telemetryPane,strategyPane;

    private final Random seeds=new Random();
    private final GameProgress progress=new GameProgress();
    private final Set<KeyCode> pressedHotkeys=EnumSet.noneOf(KeyCode.class);
    private RaceEngine race;
    private ArcadeSession arcade;
    private RaceSnapshot snapshot;
    private GrandPrixRenderer renderer;
    private AnimationTimer timer;
    private MediaPlayer gameMusic;
    private long lastFrame,frames,lastRadioRevision=-1;
    private double dashboardClock,playerX;
    private boolean running,leftPressed,rightPressed,viewActive,arcadePaused,championship;
    private Window listenedWindow;
    private final ChangeListener<Boolean> windowFocus=(o,a,b)->{if(!b)pauseForDeparture();};
    private final ChangeListener<Window> windowChanged=(o,a,b)->listenWindow(b);

    public MiniGameViewController() {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/view/MiniGameView.fxml"));loader.setRoot(this);loader.setController(this);
        try{loader.load();}catch(IOException e){throw new IllegalStateException("No se pudo abrir el juego.",e);}
    }

    @FXML private void initialize() {
        getStylesheets().add(getClass().getResource("/view/game/game.css").toExternalForm());
        renderer=new GrandPrixRenderer(canvas);canvas.widthProperty().bind(canvasHost.widthProperty());canvas.heightProperty().bind(canvasHost.heightProperty());
        canvas.widthProperty().addListener((o,a,b)->render());canvas.heightProperty().addListener((o,a,b)->render());
        teamCombo.getItems().setAll(new TeamColor("Ferrari",Color.web("#dc0000")),new TeamColor("Mercedes",Color.web("#00d2be")),new TeamColor("McLaren",Color.web("#ff8700")),new TeamColor("Red Bull",Color.web("#1e41ff")),new TeamColor("Aston Martin",Color.web("#006f62")));
        teamCombo.setValue(teamCombo.getItems().getFirst());teamCombo.setOnAction(e->render());
        modeCombo.getItems().setAll(Mode.values());modeCombo.setValue(Mode.GRAND_PRIX);
        difficultyCombo.getItems().setAll(Difficulty.values());difficultyCombo.setValue(Difficulty.ROOKIE);
        lapsCombo.getItems().setAll(6,10,16);lapsCombo.setValue(6);
        trackCombo.getItems().setAll(Track.values());trackCombo.setValue(Track.COAST);
        weatherCombo.getItems().setAll(WeatherPlan.values());weatherCombo.setValue(WeatherPlan.VARIABLE);
        startingTyreCombo.getItems().setAll(TyreCompound.values());startingTyreCombo.setValue(TyreCompound.MEDIUM);
        pitTyreCombo.getItems().setAll(TyreCompound.values());pitTyreCombo.setValue(TyreCompound.HARD);
        styleCombo.getItems().setAll(DrivingStyle.values());styleCombo.setValue(DrivingStyle.BALANCED);
        energyCombo.getItems().setAll(EnergyMode.values());energyCombo.setValue(EnergyMode.BALANCED);
        speedCombo.getItems().setAll(1,2,4);speedCombo.setValue(1);
        modeCombo.setOnAction(e->changeMode());
        styleCombo.setOnAction(e->{if(race!=null)race.setStyle(styleCombo.getValue());});
        energyCombo.setOnAction(e->{if(race!=null)race.setEnergyMode(energyCombo.getValue());});
        assistCheck.setOnAction(e->{if(race!=null)race.setAutoSteer(assistCheck.isSelected());});
        drsButton.setOnAction(e->{if(race!=null)race.setDrsArmed(drsButton.isSelected());});
        pitButton.setOnAction(e->requestPit());pauseButton.setOnAction(e->togglePause());nextButton.setOnAction(e->startRace(true));
        nextResultButton.disableProperty().bind(nextButton.disableProperty());nextResultButton.setOnAction(e->startRace(true));setupButton.setOnAction(e->gameScroll.setVvalue(0));
        setFocusTraversable(true);canvas.setFocusTraversable(true);canvas.setOnMouseClicked(e->canvas.requestFocus());
        addEventHandler(KeyEvent.KEY_PRESSED,e->key(e,true));addEventHandler(KeyEvent.KEY_RELEASED,e->key(e,false));
        sceneProperty().addListener((o,a,b)->sceneChanged(a,b));
        columns();
        timer=new AnimationTimer(){public void handle(long now){
            if(lastFrame==0){lastFrame=now;return;}
            double dt=Math.min(0.1,(now-lastFrame)/1_000_000_000d);lastFrame=now;
            if(!viewActive)return;
            frames++;advanceGame(dt);render();dashboardClock+=dt;
            if(dashboardClock>=0.1){dashboardClock=0;refreshDashboard();}
        }};
        render();
    }

    private void columns() {
        column(timingTable,"P",c->""+c.position(),32);column(timingTable,"Piloto",c->c.player()?"TÚ":c.name(),83);
        column(timingTable,"Gap",this::gap,65);column(timingTable,"Goma",c->c.tyre().code+(c.pitPhase()!=PitPhase.TRACK?" BOX":""),50);
        timingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        timingTable.setRowFactory(t->{TableRow<RaceSnapshot.Car> row=new TableRow<>();row.itemProperty().addListener((o,a,b)->{row.setStyle(b!=null&&b.player()?"-fx-background-color: #493047;":"");row.setTooltip(b==null?null:new Tooltip(b.name()+" · "+b.personality()+" · "+b.status()));});return row;});
        column(resultsTable,"P",c->""+c.position(),40);column(resultsTable,"Piloto",RaceSnapshot.Car::name,120);
        column(resultsTable,"Tiempo / estado",c->c.retired()?c.status():time(c.finishTime()),140);
        column(resultsTable,"Penalización",c->String.format(Locale.ROOT,"+%.0f s",c.penalties()),100);
        column(resultsTable,"Mejor vuelta",c->time(c.bestLap()),95);column(resultsTable,"Paradas",c->""+c.stops(),70);
        column(resultsTable,"Puntos",c->""+(c.retired()?0:GameRules.points(c.position())),65);
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        column(championshipTable,"Piloto",GameProgress.Standing::driver,160);column(championshipTable,"Puntos",s->""+s.points(),100);column(championshipTable,"Victorias",s->""+s.wins(),100);
        championshipTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventsList.setCellFactory(v->new ListCell<>(){protected void updateItem(String value,boolean empty){super.updateItem(value,empty);setText(empty?null:value);setWrapText(true);setPrefWidth(0);}});
        timingTable.setPlaceholder(new Label("Parrilla de 8 pilotos al iniciar"));
    }
    private <T> void column(TableView<T> table,String title,Function<T,String> text,double width) {
        TableColumn<T,String> column=new TableColumn<>(title);column.setPrefWidth(width);column.setCellValueFactory(v->new SimpleStringProperty(text.apply(v.getValue())));table.getColumns().add(column);
    }

    @FXML private void onStartGame() {
        if(modeCombo.getValue()==Mode.ARCADE) {
            race=null;snapshot=null;arcade=new ArcadeSession(seeds.nextLong());arcadePaused=false;running=true;frames=0;clearKeys();pressedHotkeys.clear();playerX=arcade.snapshot().playerX();
            resetResults();pauseButton.setDisable(false);statusLabel.setText("Arcade clásico · usa ← → y evita los obstáculos. +10 puntos por coche superado.");
            lockSetup(false);startButton.setText("Reiniciar arcade");restartTimer();refreshDashboard();focus(liveHud);
        } else startRace(false);
        canvas.requestFocus();
    }
    private void startRace(boolean next) {
        championship=modeCombo.getValue()==Mode.CHAMPIONSHIP;
        if(championship&&!next)progress.beginChampionship();
        Track track=championship?Track.values()[Math.min(progress.rounds(),Track.values().length-1)]:trackCombo.getValue();
        race=new RaceEngine(new Config(lapsCombo.getValue(),difficultyCombo.getValue(),track,weatherCombo.getValue(),startingTyreCombo.getValue(),seeds.nextLong(),true));
        race.setStyle(styleCombo.getValue());race.setEnergyMode(energyCombo.getValue());race.setAutoSteer(assistCheck.isSelected());race.setDrsArmed(drsButton.isSelected());
        arcade=null;running=true;frames=0;lastRadioRevision=-1;clearKeys();pressedHotkeys.clear();resetResults();lockSetup(true);
        startButton.setText(championship?"Reiniciar campeonato":"Reiniciar GP");
        sessionLabel.setText(championship?"GP "+(progress.rounds()+1)+" / 3 · "+track:"GP LIBRE · "+track);
        pauseButton.setDisable(false);pitButton.setDisable(false);statusLabel.setText("Salida · vigila gomas, pronóstico y energía; P solicita boxes.");
        restartTimer();refreshDashboard();canvas.requestFocus();focus(liveHud);
    }
    private void resetResults(){resultsPane.setVisible(false);resultsPane.setManaged(false);resultsTable.getItems().clear();nextButton.setDisable(true);}
    private void restartTimer(){lastFrame=0;dashboardClock=0;if(viewActive)timer.start();}
    private void lockSetup(boolean active) {
        for(Control control:List.of(difficultyCombo,lapsCombo,trackCombo,weatherCombo,startingTyreCombo))control.setDisable(active);
        boolean classic=modeCombo.getValue()==Mode.ARCADE;
        for(Control control:List.of(styleCombo,energyCombo,drsButton,pitTyreCombo,assistCheck))control.setDisable(classic);
        for(Node node:List.of(towerPane,telemetryPane,strategyPane)){node.setVisible(!classic);node.setManaged(!classic);}
        speedCombo.setDisable(classic);
        controlsHint.setText(classic?"← → (o A/D) esquiva los obstáculos. Espacio pausa. Cada coche evitado suma 10 puntos.":"← → cambia de trayectoria. P pide/cancela boxes. E alterna ERS. D solicita DRS. Espacio pausa. La asistencia recupera la trazada al soltar las flechas. 1× = 3 segundos del juego por segundo real.");
    }
    private void changeMode() {
        timer.stop();running=false;race=null;arcade=null;snapshot=null;clearKeys();pressedHotkeys.clear();resetResults();lockSetup(false);timingTable.getItems().clear();eventsList.getItems().clear();
        pitButton.setDisable(true);pauseButton.setDisable(true);sessionLabel.setText(modeCombo.getValue().toString());startButton.setText(modeCombo.getValue()==Mode.ARCADE?"Iniciar arcade":"Iniciar GP");
        statusLabel.setText(modeCombo.getValue()==Mode.ARCADE?"Arcade clásico: flechas para esquivar, conserva tu elección de color.":"Configura el GP. El campeonato encadena tres circuitos del juego; la progresión es de esta sesión.");
        clearTelemetry();render();
    }
    private void advanceGame(double dt) {
        double steering=(rightPressed?1:0)-(leftPressed?1:0);
        if(race!=null){race.steer(steering);race.advance(dt*CLOCK_RATE*speedCombo.getValue());snapshot=race.snapshot();playerX=450+snapshot.player().lane()*200;running=snapshot.state()!=State.FINISHED;}
        else if(arcade!=null&&!arcadePaused){arcade.advance(dt,steering);playerX=arcade.snapshot().playerX();running=arcade.snapshot().running();if(!running){timer.stop();statusLabel.setText("¡Colisión! Puntuación final: "+arcade.snapshot().score()+". Pulsa Iniciar para intentarlo de nuevo.");pauseButton.setDisable(true);refreshDashboard();}}
    }
    private void refreshDashboard() {
        if(race==null) {
            if(arcade!=null){positionLabel.setText("ARCADE");lapLabel.setText("∞");clockLabel.setText(""+arcade.snapshot().score()+" pts");flagLabel.setText(arcadePaused?"PAUSA":running?"EN PISTA":"COLISIÓN");weatherLabel.setText("Arcade clásico · flechas para esquivar obstáculos");pauseButton.setText(arcadePaused?"Reanudar [Espacio]":"Pausa [Espacio]");}return;
        }
        snapshot=race.snapshot();var player=snapshot.player();playerX=450+player.lane()*200;
        lapLabel.setText(Math.min(snapshot.config().laps(),player.laps()+1)+" / "+snapshot.config().laps());positionLabel.setText("P"+player.position()+" / "+snapshot.cars().size());
        clockLabel.setText(time(snapshot.elapsed()));lapTimeLabel.setText(time(player.lastLap())+" / "+time(player.bestLap()));
        flagLabel.setText(snapshot.flag()+(snapshot.neutralisationLeft()>0?String.format(Locale.ROOT," · %.0f s",snapshot.neutralisationLeft()):""));
        weatherLabel.setText(snapshot.weather()+" · pista "+Math.round(snapshot.water()*100)+"% mojada · pronóstico: "+snapshot.forecast());
        tyreLabel.setText(player.tyre()+" · vida "+Math.round((1-player.wear())*100)+"%");tyreBar.setProgress(1-player.wear());
        temperatureLabel.setText(String.format(Locale.ROOT,"Temperatura %.0f °C",player.temperature()));
        fuelLabel.setText(String.format(Locale.ROOT,"Combustible %.1f kg",player.fuel()));fuelBar.setProgress(player.fuel()/player.initialFuel());
        energyLabel.setText(String.format(Locale.ROOT,"ERS %.0f%%",player.energy()));energyBar.setProgress(player.energy()/100);
        speedLabel.setText(String.format(Locale.ROOT,"Velocidad %.0f km/h",player.speed()*3.6));drsLabel.setText("DRS · "+player.drsReason());
        penaltyLabel.setText("Avisos "+player.warnings()+"/3 · +"+Math.round(player.penalties())+" s · "+player.overtakes()+" adelantamientos");
        pitLabel.setText(player.pitPhase()==PitPhase.TRACK?player.pitRequested()?"BOX confirmado · espera a la próxima entrada de pits":"Paradas: "+player.stops()+" · elige goma y solicita BOX":player.pitPhase()+String.format(Locale.ROOT," · %.1f s",player.pitRemaining()));
        pitButton.setText(player.pitPhase()!=PitPhase.TRACK?"Parada en curso":player.pitRequested()?"Cancelar BOX [P]":"BOX · Solicitar parada [P]");pitButton.setDisable(player.pitPhase()!=PitPhase.TRACK||player.finished()||player.retired()||snapshot.state()==State.FINISHED);
        pitTyreCombo.setDisable(player.pitRequested()||player.pitPhase()!=PitPhase.TRACK||player.finished()||player.retired());
        pauseButton.setText(snapshot.state()==State.PAUSED?"Reanudar [Espacio]":"Pausa [Espacio]");
        timingTable.getItems().setAll(snapshot.cars());
        if(lastRadioRevision!=snapshot.revision()) {
            lastRadioRevision=snapshot.revision();eventsList.getItems().setAll(snapshot.events().stream().map(e->time(e.time())+" · "+e.message()).toList());
            if(!snapshot.events().isEmpty())statusLabel.setText(snapshot.events().getFirst().message());
        }
        if(snapshot.state()==State.FINISHED)finishRace();
    }
    private void finishRace() {
        timer.stop();running=false;pauseButton.setDisable(true);lockSetup(false);
        if(progress.record(snapshot,championship)) {
            resultsTable.getItems().setAll(snapshot.cars());
            var player=snapshot.player();int points=player.retired()?0:GameRules.points(player.position());
            resultSummary.setText((player.retired()?player.status():"Terminas P"+player.position())+" · "+points+" puntos · "+player.stops()+" paradas · "+player.cleanLaps()+" vueltas limpias · "+player.overtakes()+" adelantamientos · "+player.mistakes()+" errores · +"+Math.round(player.penalties())+" s de penalización.");
            var stats=progress.stats();progressLabel.setText("Esta sesión: "+stats.races()+" GP · "+stats.wins()+" victorias · "+stats.podiums()+" podios · "+stats.points()+" puntos · "+stats.experience()+" XP · nivel "+stats.level()+" · mejor vuelta "+time(stats.bestLap()));
            championshipTable.getItems().setAll(progress.standings());for(Node node:List.of(championshipTable,championshipTitle)){node.setVisible(championship);node.setManaged(championship);}
            focus(resultsPane);
        }
        resultsPane.setVisible(true);resultsPane.setManaged(true);nextButton.setDisable(!championship||progress.rounds()>=Track.values().length);
        nextResultButton.setVisible(championship&&progress.rounds()<Track.values().length);nextResultButton.setManaged(nextResultButton.isVisible());
        statusLabel.setText(championship&&progress.rounds()>=3?"Campeonato completado · consulta la clasificación final y tus estadísticas.":"Bandera a cuadros · resultados y estadísticas debajo de la radio.");
    }
    private void requestPit(){if(race!=null){race.requestPit(pitTyreCombo.getValue());refreshDashboard();}}
    private void togglePause() {
        clearKeys();lastFrame=0;
        if(race!=null){if(race.snapshot().state()==State.PAUSED)race.resume();else race.pause();refreshDashboard();}
        else if(arcade!=null&&running){arcadePaused=!arcadePaused;refreshDashboard();}
        if(viewActive&&running)timer.start();render();
    }
    private void key(KeyEvent event,boolean pressed) {
        if(!viewActive||!running)return;
        Object target=event.getTarget();if(target instanceof TextInputControl||target instanceof ComboBoxBase<?>||target instanceof Spinner<?>)return;
        KeyCode code=event.getCode();boolean handled=true;
        if(!pressed)pressedHotkeys.remove(code);
        if(code==KeyCode.LEFT||code==KeyCode.A)leftPressed=pressed;
        else if(code==KeyCode.RIGHT||code==KeyCode.D&&modeCombo.getValue()==Mode.ARCADE)rightPressed=pressed;
        else if(pressed&&!event.isAltDown()&&!event.isControlDown()) {
            if(!pressedHotkeys.add(code)){event.consume();return;}
            if(code==KeyCode.SPACE)togglePause();else if(modeCombo.getValue()==Mode.ARCADE)handled=false;else if(code==KeyCode.P)requestPit();
            else if(code==KeyCode.E)energyCombo.getSelectionModel().select((energyCombo.getSelectionModel().getSelectedIndex()+1)%EnergyMode.values().length);
            else if(code==KeyCode.D)drsButton.fire();else handled=false;
        } else handled=false;
        if(handled)event.consume();
    }
    private void clearKeys(){leftPressed=false;rightPressed=false;if(race!=null)race.steer(0);}
    private void clearTelemetry(){lapLabel.setText("—");positionLabel.setText("—");clockLabel.setText("0:00.0");lapTimeLabel.setText("— / —");flagLabel.setText("LISTO");weatherLabel.setText("El pronóstico aparecerá al iniciar.");tyreLabel.setText("NEUMÁTICOS · —");fuelLabel.setText("COMBUSTIBLE · —");energyLabel.setText("ERS · —");drsLabel.setText("DRS · —");temperatureLabel.setText("Temperatura · —");speedLabel.setText("Velocidad · —");tyreBar.setProgress(1);fuelBar.setProgress(1);energyBar.setProgress(1);pitLabel.setText("Se entra al alcanzar la zona de boxes.");penaltyLabel.setText("Avisos 0/3 · penalización 0 s");}
    private void render(){if(renderer==null||teamCombo.getValue()==null)return;if(modeCombo.getValue()==Mode.ARCADE)renderer.drawArcade(arcade==null?null:arcade.snapshot(),teamCombo.getValue().color(),frames);else renderer.draw(snapshot,teamCombo.getValue().color());}
    private String gap(RaceSnapshot.Car car){return car.retired()?"RET":car.position()==1?"LÍDER":String.format(Locale.ROOT,"+%.1f",car.gap());}
    private void focus(Node node){Platform.runLater(()->{Parent content=(Parent)gameScroll.getContent();content.applyCss();content.layout();double range=content.getLayoutBounds().getHeight()-gameScroll.getViewportBounds().getHeight();if(range>0){double y=content.sceneToLocal(node.localToScene(node.getBoundsInLocal())).getMinY();gameScroll.setVvalue(Math.max(0,Math.min(1,y/range)));}});}
    private static String time(Double seconds){if(seconds==null)return "—";long tenths=Math.round(seconds*10);return String.format(Locale.ROOT,"%d:%04.1f",tenths/600,(tenths%600)/10d);}
    private void pauseForDeparture(){clearKeys();pressedHotkeys.clear();lastFrame=0;if(race!=null)race.pause();if(arcade!=null&&running)arcadePaused=true;}
    private void listenWindow(Window window){if(listenedWindow!=null)listenedWindow.focusedProperty().removeListener(windowFocus);listenedWindow=window;if(window!=null)window.focusedProperty().addListener(windowFocus);}
    private void sceneChanged(Scene oldScene,Scene newScene){if(oldScene!=null)oldScene.windowProperty().removeListener(windowChanged);listenWindow(null);if(newScene!=null){newScene.windowProperty().addListener(windowChanged);listenWindow(newScene.getWindow());}else onTabUnselected();}
    public void onTabSelected() {
        viewActive=true;lastFrame=0;
        if(gameMusic==null)gameMusic=AppAudio.playLoop("assets/audio/game/game.mp3","assets/audio/game/game","src/assets/audio/game/game.mp3","src/assets/audio/game/game");
        if(running)timer.start();refreshDashboard();render();
    }
    public void onTabUnselected(){viewActive=false;pauseForDeparture();if(timer!=null)timer.stop();AppAudio.stop(gameMusic);gameMusic=null;}
    @Override public void dispose(){onTabUnselected();listenWindow(null);}
}
