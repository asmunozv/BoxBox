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
    static Stage stage;static Object explorer;static int retries;
    public static void main(String[] args){
        Thread timeout=new Thread(()->{try{Thread.sleep(45000);System.err.println("HISTORY_UI_TIMEOUT");System.exit(1);}catch(InterruptedException ignored){}},"history-watchdog");timeout.setDaemon(true);timeout.start();
        Platform.startup(()->{try{stage=new Stage();new Main().start(stage);later(1000,()->{
            Parent login=stage.getScene().getRoot();String user,password;
            try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT username,password FROM app_users ORDER BY id_user LIMIT 1");ResultSet r=p.executeQuery()){require(r.next());user=r.getString(1);password=r.getString(2);}
            ((TextField)field(login,"usernameField")).setText(user);((PasswordField)field(login,"passwordField")).setText(password);((Button)field(login,"loginButton")).fire();
            explorer=field(stage.getScene().getRoot(),"explorer");waitLoaded();
        });}catch(Throwable e){fail(e);}});
    }
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
        RaceData.Entry winner=table.getItems().stream().filter(e->Integer.valueOf(1).equals(e.position())).findFirst().orElseThrow();require(winner.points()==26);
        driver.setValue(winner.driver());require(table.getItems().size()==1);
        driver.setValue("Todos los pilotos");team.setValue(winner.team());require(table.getItems().size()==2);
        team.setValue("Todas las escuderías");search.setText("Norris");require(table.getItems().size()==1);
        search.setText("NO_MATCH_BOXBOX_TEST");require(table.getItems().isEmpty());search.clear();require(table.getItems().size()==20);
        session.setValue("Sprint");require(table.getItems().isEmpty());session.setValue("Carrera");require(table.getItems().size()==20);
        require(((ListView<?>)field(explorer,"sourcesList")).getItems().size()>=6);
        System.out.println("HISTORICAL_EXPLORER_UI_PASSED: 24 races, real 26-point winner, driver/team/search/session filters, empty state and source records");
        if(explorer.getClass().getSimpleName().equals("RaceLabController"))verifyAnalysis();
        stage.close();Platform.exit();System.exit(0);
    }
    static void verifyAnalysis()throws Exception{
        Object comparison=field(explorer,"comparison"),tyres=field(explorer,"tyres"),pits=field(explorer,"pits"),positions=field(explorer,"positions"),simulation=field(explorer,"simulation");
        require(((TableView<?>)field(comparison,"metrics")).getItems().size()==8);
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
        stops.setText("14:HARD;14:SOFT");calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("Escenario no calculado"));stops.setText(original);calculate.fire();require(((Label)field(simulation,"resultLabel")).getText().startsWith("ESTIMADO"));
        System.out.println("ANALYTICAL_UI_PASSED: driver/team comparison, two tyre timelines, pits, reversed position axis, pilot visibility, simulator controls and invalid-input handling");
    }
    static Object field(Object o,String name)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    static void later(int ms,Step step){PauseTransition p=new PauseTransition(Duration.millis(ms));p.setOnFinished(e->Platform.runLater(()->{try{step.run();}catch(Throwable x){fail(x);}}));p.play();}
    static void require(boolean condition){if(!condition)throw new AssertionError("Historical UI assertion");}
    static void fail(Throwable e){System.err.println("HISTORY_UI_FAILED: "+e.getClass().getSimpleName());for(StackTraceElement x:e.getStackTrace())if(x.getClassName().startsWith("verification.")){System.err.println(x.getMethodName()+":"+x.getLineNumber());break;}System.exit(1);}
    interface Step{void run()throws Exception;}
}
