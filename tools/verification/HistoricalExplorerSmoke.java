package verification;

import application.Main;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.db.DatabaseConnection;
import model.history.RaceData;

public final class HistoricalExplorerSmoke {
    static Stage stage;static Object explorer;static int retries;static boolean gallery;
    static final java.nio.file.Path EVIDENCE=java.nio.file.Path.of("private/verification/gallery-"+UUID.randomUUID());
    public static void main(String[] args){
        gallery=Arrays.asList(args).contains("--gallery");
        Thread timeout=new Thread(()->{try{Thread.sleep(45000);System.err.println("HISTORY_UI_TIMEOUT");System.exit(1);}catch(InterruptedException ignored){}},"history-watchdog");timeout.setDaemon(true);timeout.start();
        Platform.startup(()->{try{stage=new Stage();new Main().start(stage);later(1000,()->{
            Parent login=stage.getScene().getRoot();String user,password;
            try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT username,password FROM app_users ORDER BY id_user LIMIT 1");ResultSet r=p.executeQuery()){require(r.next());user=r.getString(1);password=r.getString(2);}
            ((TextField)field(login,"usernameField")).setText(user);((PasswordField)field(login,"passwordField")).setText(password);((Button)field(login,"loginButton")).fire();
            waitLogin();
        });}catch(Throwable e){fail(e);}});
    }
    static void waitLogin(){later(250,()->{if(!stage.getScene().getRoot().getClass().getSimpleName().equals("MainViewController")){if(retries++>80)throw new AssertionError("Login did not finish");waitLogin();return;}explorer=field(stage.getScene().getRoot(),"explorer");waitLoaded();});}
    static void waitLoaded(){later(300,()->{if(((BooleanProperty)field(explorer,"busy")).get()||field(explorer,"data")==null){if(retries++>80)throw new AssertionError("No local data loaded");waitLoaded();return;}verify();});}
    static void verify()throws Exception{
        @SuppressWarnings("unchecked") ComboBox<RaceData.RaceInfo> races=(ComboBox<RaceData.RaceInfo>)field(explorer,"raceCombo");
        @SuppressWarnings("unchecked") TableView<RaceData.Entry> table=(TableView<RaceData.Entry>)field(explorer,"resultsTable");
        @SuppressWarnings("unchecked") ComboBox<String> driver=(ComboBox<String>)field(explorer,"driverFilter");
        @SuppressWarnings("unchecked") ComboBox<String> team=(ComboBox<String>)field(explorer,"teamFilter");
        @SuppressWarnings("unchecked") ComboBox<String> session=(ComboBox<String>)field(explorer,"sessionFilter");
        TextField search=(TextField)field(explorer,"searchField");
        require(races.getItems().size()==24&&races.getValue().season()==2024&&races.getValue().round()==1);
        require(table.getItems().size()==20);
        CheckBox qualifying=(CheckBox)field(explorer,"qualifyingDetails");
        require(!table.getColumns().get(6).isVisible());qualifying.fire();require(table.getColumns().get(6).isVisible());qualifying.fire();
        RaceData.Entry winner=table.getItems().stream().filter(e->Integer.valueOf(1).equals(e.position())).findFirst().orElseThrow();require(winner.points()==26);
        require(((Label)field(explorer,"pointsLabel")).getText().contains("25 por P1 + 1 por vuelta rápida"));
        driver.setValue(winner.driver());require(table.getItems().size()==1);
        driver.setValue("Todos los pilotos");team.setValue(winner.team());require(table.getItems().size()==2);
        team.setValue("Todas las escuderías");search.setText("Norris");require(table.getItems().size()==1);
        search.setText("NO_MATCH_BOXBOX_TEST");require(table.getItems().isEmpty());search.clear();require(table.getItems().size()==20);
        session.setValue("Sprint");require(table.getItems().isEmpty());session.setValue("Carrera");require(table.getItems().size()==20);
        require(((ListView<?>)field(explorer,"sourcesList")).getItems().size()>=6);
        System.out.println("HISTORICAL_EXPLORER_UI_PASSED: 24 races, P1 25 + 1 fastest-lap breakdown, driver/team/search/session filters, empty state and source records");
        if(explorer.getClass().getSimpleName().equals("RaceLabController"))verifyAnalysis();
        verifyGuide();
        verifyMedia();
    }
    static void verifyMedia()throws Exception {
        Object card=field(explorer,"driverMedia");
        var image=((javafx.scene.image.ImageView)field(card,"photo")).getImage();
        require(image!=null&&!image.isError()&&image.getWidth()>64);
        require(((Label)field(card,"nameLabel")).getText().equals("Max Verstappen"));
        ((TabPane)field(explorer,"analysisTabs")).getSelectionModel().select(0);
        Button play=(Button)field(card,"playButton");require(!play.isDisabled());play.fire();waitAudio(card,0);
        stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();require(play.getWidth()>=90);
    }
    static void waitAudio(Object card,int attempt)throws Exception {
        later(100,()->{
            var player=(javafx.scene.media.MediaPlayer)field(card,"player");
            require(player!=null&&player.getError()==null);
            if(player.getStatus()!=javafx.scene.media.MediaPlayer.Status.PLAYING){if(attempt>=25)throw new AssertionError("Driver audio did not start");waitAudio(card,attempt+1);return;}
            ((Button)field(card,"stopButton")).fire();require(field(card,"player")==null);
            require(player.getStatus()==javafx.scene.media.MediaPlayer.Status.DISPOSED);
            ((Button)field(card,"playButton")).fire();require(field(card,"player")!=null);
            ((TabPane)field(explorer,"analysisTabs")).getSelectionModel().select(1);require(field(card,"player")==null);
            System.out.println("HISTORICAL_MULTIMEDIA_PASSED: original driver photo decoded, user-triggered audio PLAYING, stop and tab-change disposal");
            verifyEmptySeason();
        });
    }
    static void verifyGuide()throws Exception {
        Parent main=stage.getScene().getRoot();
        ((Button)field(explorer,"helpButton")).fire();
        var host=(javafx.scene.layout.StackPane)field(main,"contentHost");
        Object guide=field(main,"guide");require(host.getChildren().contains(guide));
        ((Button)field(guide,"positions")).fire();
        TabPane tabs=(TabPane)field(explorer,"analysisTabs");require(tabs.getSelectionModel().getSelectedIndex()==4);
        var navigation=(javafx.scene.layout.VBox)field(main,"analysisNavigation");
        require(navigation.getChildren().get(4).getStyleClass().contains("nav-active"));
        tabs.getSelectionModel().select(5);require(navigation.getChildren().get(5).getStyleClass().contains("nav-active"));
        ((Button)navigation.getChildren().getFirst()).fire();require(tabs.getSelectionModel().getSelectedIndex()==0);
        System.out.println("ONBOARDING_NAVIGATION_PASSED: guide shortcuts, tab/sidebar synchronization, explorer returns to summary");
    }
    static void verifyEmptySeason()throws Exception {
        @SuppressWarnings("unchecked") ComboBox<Integer> seasons=(ComboBox<Integer>)field(explorer,"seasonCombo");
        @SuppressWarnings("unchecked") List<RaceData.RaceInfo> calendar=(List<RaceData.RaceInfo>)field(explorer,"calendar");
        int empty=seasons.getItems().stream().filter(y->calendar.stream().noneMatch(r->r.season()==y)).findFirst().orElseThrow();
        seasons.setValue(empty);require(field(explorer,"data")==null);
        require(((TableView<?>)field(explorer,"resultsTable")).getItems().isEmpty());
        require(((ListView<?>)field(explorer,"sourcesList")).getItems().isEmpty());
        TabPane tabs=(TabPane)field(explorer,"analysisTabs");for(int i=1;i<=6;i++)require(tabs.getTabs().get(i).isDisable());
        var navigation=(javafx.scene.layout.VBox)field(stage.getScene().getRoot(),"analysisNavigation");
        ((Button)navigation.getChildren().get(5)).fire();require(tabs.getSelectionModel().getSelectedIndex()==0);
        ((Button)field(explorer,"helpButton")).fire();
        ((Button)field(field(stage.getScene().getRoot(),"guide"),"demo")).fire();
        waitDemo(0);
    }
    static void waitDemo(int attempts) {
        later(150,()->{
            if(((BooleanProperty)field(explorer,"busy")).get()||field(explorer,"data")==null){if(attempts>80)throw new AssertionError("Offline demo did not finish");waitDemo(attempts+1);return;}
            RaceData data=(RaceData)field(explorer,"data");require(data.race().key().equals("2024:1"));
            require(((TableView<?>)field(explorer,"resultsTable")).getItems().size()==20);
            TabPane tabs=(TabPane)field(explorer,"analysisTabs");for(int i=1;i<=6;i++)require(!tabs.getTabs().get(i).isDisable());
            System.out.println("EMPTY_SELECTION_AND_OFFLINE_DEMO_PASSED: old race hidden, analyses unavailable for empty season, guide demo restores Bahrain without HTTP");
            if(gallery){((Label)field(stage.getScene().getRoot(),"userLabel")).setText("Sesión de demostración");capture(0);}else finish();
        });
    }
    static void capture(int index)throws Exception{
        TabPane tabs=(TabPane)field(explorer,"analysisTabs");if(index>=tabs.getTabs().size()){captureCompact();return;}tabs.getSelectionModel().select(index);
        if(index==5)((Button)field(field(explorer,"simulation"),"earlier")).fire();
        later(400,()->{snapshot("tab-"+index+".png");capture(index+1);});
    }
    static void captureCompact()throws Exception {
        stage.setWidth(1000);stage.setHeight(650);((TabPane)field(explorer,"analysisTabs")).getSelectionModel().select(0);
        later(400,()->{snapshot("compact-summary.png");((TabPane)field(explorer,"analysisTabs")).getSelectionModel().select(5);
            ((Button)field(field(explorer,"simulation"),"calculate")).fire();later(400,()->{snapshot("compact-simulator.png");((Button)field(explorer,"helpButton")).fire();later(400,()->{snapshot("guide.png");System.out.println("VISUAL_GALLERY="+EVIDENCE);finish();});});});
    }
    static void snapshot(String filename)throws Exception {
        stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
        var image=stage.getScene().snapshot(null);var png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        java.nio.file.Files.createDirectories(EVIDENCE);javax.imageio.ImageIO.write(png,"png",EVIDENCE.resolve(filename).toFile());
    }
    static void finish(){stage.close();Platform.exit();System.exit(0);}
    static void verifyAnalysis()throws Exception{
        Object comparison=field(explorer,"comparison"),tyres=field(explorer,"tyres"),pits=field(explorer,"pits"),positions=field(explorer,"positions"),simulation=field(explorer,"simulation");
        require(((TableView<?>)field(comparison,"metrics")).getItems().size()==8);
        require(field(((TableView<?>)field(comparison,"metrics")).getItems().getFirst(),"a").equals("437.00"));
        require(((TableView<?>)field(comparison,"racesTable")).getItems().size()==24);
        @SuppressWarnings("unchecked") ComboBox<String> mode=(ComboBox<String>)field(comparison,"mode");mode.setValue("Escuderías");require(((TableView<?>)field(comparison,"metrics")).getItems().size()==8);mode.setValue("Pilotos");
        require(((javafx.scene.chart.LineChart<?,?>)field(comparison,"pointsChart")).getData().size()==2);
        require(((javafx.scene.layout.VBox)field(tyres,"timelines")).getChildren().size()>=4);
        require(((javafx.scene.chart.LineChart<?,?>)field(tyres,"lapChart")).getData().size()==2);
        require(((TableView<?>)field(pits,"pitTable")).getItems().size()>=4);
        var chart=(javafx.scene.chart.LineChart<?,?>)field(positions,"positions");int count=chart.getData().size();require(count>=4);
        var axis=(javafx.scene.chart.NumberAxis)field(positions,"positionAxis");require(axis.getUpperBound()==-1&&axis.getLowerBound()<=-20);
        var checks=(javafx.scene.layout.FlowPane)field(positions,"driverChoices");((CheckBox)checks.getChildren().get(0)).fire();require(chart.getData().size()<count);
        Button calculate=(Button)field(simulation,"calculate");require(!calculate.isDisable());
        TextField stops=(TextField)field(simulation,"stops"),degradation=(TextField)field(simulation,"degradation");
        String original=stops.getText();degradation.setText("0.08");stops.setText(original.replaceFirst("17:","14:"));calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        require(((javafx.scene.chart.LineChart<?,?>)field(simulation,"deltaChart")).getData().size()==2);
        require(!((ListView<?>)field(explorer,"narrative")).getItems().isEmpty());
        stops.setText("14:HARD;14:SOFT");calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("Escenario no calculado"));stops.setText(original);calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        ((Button)field(simulation,"reset")).fire();require(stops.getText().startsWith("17:HARD"));
        ((Button)field(simulation,"earlier")).fire();require(stops.getText().startsWith("14:HARD"));require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        ((Button)field(simulation,"noStops")).fire();require(stops.getText().isEmpty());require(((Label)field(simulation,"interpretation")).getText().contains("un solo compuesto"));
        ((Button)field(simulation,"addStop")).fire();require(!stops.getText().isEmpty());calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        var stopRows=(javafx.scene.layout.VBox)field(simulation,"stopRows");var row=(javafx.scene.layout.FlowPane)stopRows.getChildren().getFirst();
        @SuppressWarnings("unchecked") Spinner<Integer> lap=(Spinner<Integer>)row.getChildren().get(1);lap.getValueFactory().setValue(15);require(stops.getText().startsWith("15:HARD"));
        ((Button)row.getChildren().getLast()).fire();require(stops.getText().isEmpty());
        ((Button)field(simulation,"reset")).fire();degradation.setText("-1");calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("Escenario no calculado"));
        degradation.setText("0,08");calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        ((Button)field(simulation,"reset")).fire();require(stopRows.getChildren().size()==2);
        System.out.println("GUIDED_SIMULATOR_PASSED: earlier pit, no stops, add/remove, lap spinner, reset, negative-input rejection and comma decimal");
        System.out.println("ANALYTICAL_UI_PASSED: driver/team comparison, two tyre timelines, pits, reversed position axis, pilot visibility, simulator controls and invalid-input handling");
    }
    static Object field(Object o,String name)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    static void later(int ms,Step step){PauseTransition p=new PauseTransition(Duration.millis(ms));p.setOnFinished(e->Platform.runLater(()->{try{step.run();}catch(Throwable x){fail(x);}}));p.play();}
    static void require(boolean condition){if(!condition)throw new AssertionError("Historical UI assertion");}
    static void fail(Throwable e){System.err.println("HISTORY_UI_FAILED: "+e.getClass().getSimpleName());for(StackTraceElement x:e.getStackTrace())if(x.getClassName().startsWith("verification.")){System.err.println(x.getMethodName()+":"+x.getLineNumber());break;}System.exit(1);}
    interface Step{void run()throws Exception;}
}
