package verification.game;

import controller.MiniGameViewController;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.UUID;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.game.*;
import static model.game.GameRules.*;

/** Actual game FXML/toolkit, media and controls, completing GPs without touching authentication or SQL. */
public final class GameSmoke {
    private static MiniGameViewController game;
    private static Stage stage;
    private static final Path EVIDENCE=Path.of("private/verification/game-ui-"+UUID.randomUUID());
    public static void main(String[] args) {
        Thread watchdog=new Thread(()->{try{Thread.sleep(60000);throw new AssertionError("Game UI timeout");}catch(InterruptedException ignored){}catch(Throwable e){fail(e);}},"game-ui-watchdog");watchdog.setDaemon(true);watchdog.start();
        Platform.startup(()->{try {
            game=new MiniGameViewController();stage=new Stage();stage.setScene(view.SceneTheme.apply(new Scene(game,1240,850)));stage.show();game.onTabSelected();
            button("startButton").fire();later(500,()->probeAudio(0));
        }catch(Throwable e){fail(e);}});
    }
    private static void probeAudio(int retry)throws Exception {
        MediaPlayer music=(MediaPlayer)field("gameMusic");require(music!=null&&music.getError()==null,"original song present");
        if(music.getStatus()!=MediaPlayer.Status.PLAYING){if(retry>20)throw new AssertionError("Background song did not play");later(100,()->probeAudio(retry+1));return;}
        require(music.getMedia().getSource().endsWith("/assets/audio/game/game.mp3"),"same original song");
        ((AnimationTimer)field("timer")).stop();
        require((Long)field("frames")>0,"animation advances");
        double before=(Double)field("playerX");key(KeyCode.LEFT,true);advanceFrame(0.25);key(KeyCode.LEFT,false);require((Double)field("playerX")<before,"keyboard steering");
        RaceEngine race=race();double clock=race.snapshot().elapsed();button("pauseButton").fire();race.advance(4);require(race.snapshot().elapsed()==clock,"pause freezes game");button("pauseButton").fire();stopTimer();
        game.onTabUnselected();require(race.snapshot().state()==State.PAUSED&&field("gameMusic")==null,"tab departure pauses and releases music");
        game.onTabSelected();require(race.snapshot().state()==State.PAUSED,"return does not silently resume");button("pauseButton").fire();stopTimer();
        @SuppressWarnings("unchecked") ComboBox<TyreCompound> tyres=(ComboBox<TyreCompound>)field("pitTyreCombo");tyres.setValue(TyreCompound.INTERMEDIATE);
        button("pitButton").fire();require(race.snapshot().player().pitRequested()&&tyres.isDisabled(),"pit button locks confirmed compound");button("pitButton").fire();require(!race.snapshot().player().pitRequested()&&!tyres.isDisabled(),"pit cancellation unlocks compound");
        key(KeyCode.P,true);require(race.snapshot().player().pitRequested(),"P requests pit");key(KeyCode.P,true);require(race.snapshot().player().pitRequested(),"held P does not cancel by keyboard repeat");key(KeyCode.P,false);
        int guard=0;while(race.snapshot().player().pitPhase()!=PitPhase.SERVICE&&guard++<3000)race.advance(0.1);
        require(race.snapshot().player().pitPhase()==PitPhase.SERVICE,"mechanics state reached");refresh();snapshot("pit-mechanics.png");
        require(button("pitButton").isDisabled(),"pit request locked while serviced");
        finishRace();refresh();require(((TableView<?>)field("resultsTable")).getItems().size()==8,"complete results");require(((javafx.scene.layout.VBox)field("resultsPane")).isVisible(),"results visible");
        require(((Label)field("resultSummary")).getText().contains("puntos"),"result statistics");
        later(150,()->{snapshot("gp-results.png");championship();});
    }
    private static void championship()throws Exception {
        ((ComboBox<?>)field("modeCombo")).getSelectionModel().select(1);button("startButton").fire();
        for(int round=0;round<3;round++) {
            stopTimer();finishRace();refresh();GameProgress progress=(GameProgress)field("progress");
            require(progress.rounds()==round+1,"championship round "+round);
            if(round<2){require(!button("nextButton").isDisabled(),"next GP enabled");button("nextButton").fire();}
        }
        require(button("nextButton").isDisabled(),"championship ends after three GPs");
        require(((TableView<?>)field("championshipTable")).getItems().size()==8,"championship standings");
        require(((GameProgress)field("progress")).stats().races()==4,"session statistics include all four races");
        later(150,()->{snapshot("championship.png");classic();});
    }
    private static void classic()throws Exception {
        ((ComboBox<?>)field("modeCombo")).getSelectionModel().select(2);button("startButton").fire();stopTimer();
        require(field("arcade") instanceof ArcadeSession,"classic mode retained");double before=(Double)field("playerX");key(KeyCode.LEFT,true);advanceFrame(0.2);key(KeyCode.LEFT,false);require((Double)field("playerX")<before,"classic keyboard");
        stage.setWidth(1000);stage.setHeight(650);
        later(150,()->{snapshot("classic-compact.png");game.onTabUnselected();require(field("gameMusic")==null,"music disposed");
            System.out.println("GAME_JAVAFX_SMOKE_PASSED: original game.mp3 PLAYING, real FXML/buttons/keyboard/pause, mechanics animation state, full GP results, 3-race championship, classic mode, responsive snapshots and lifecycle");
            System.out.println("GAME_UI_GALLERY="+EVIDENCE);game.dispose();stage.close();Platform.exit();System.exit(0);
        });
    }
    private static void finishRace()throws Exception {
        RaceEngine race=race();race.resume();int guard=0;
        while(race.snapshot().state()!=State.FINISHED&&guard++<10000) {
            var s=race.snapshot();var p=s.player();int left=s.config().laps()-p.laps();
            if(!p.retired()&&!p.finished()&&p.pitPhase()==PitPhase.TRACK&&!p.pitRequested()&&left>1) {
                boolean wrong=p.tyre().dry()&&s.water()>0.3||!p.tyre().dry()&&s.water()<0.12||p.tyre()==TyreCompound.INTERMEDIATE&&s.water()>0.8;
                if(wrong||p.wear()>0.58||s.config().weather()==WeatherPlan.DRY&&p.stops()==0&&p.laps()>=2)race.requestPit(TyreCompound.suitable(s.water(),left));
            }
            race.advance(0.5);
        }
        require(race.snapshot().state()==State.FINISHED&&race.snapshot().player().finished(),"player completes full GP");
    }
    private static void refresh()throws Exception{call("refreshDashboard");call("render");game.applyCss();game.layout();}
    private static void stopTimer()throws Exception{((AnimationTimer)field("timer")).stop();}
    private static void advanceFrame(double dt)throws Exception{Method m=game.getClass().getDeclaredMethod("advanceGame",double.class);m.setAccessible(true);m.invoke(game,dt);refresh();}
    private static RaceEngine race()throws Exception{return (RaceEngine)field("race");}
    private static Object field(String name)throws Exception{Field field=game.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(game);}
    private static Button button(String name)throws Exception{return (Button)field(name);}
    private static void call(String name)throws Exception{Method method=game.getClass().getDeclaredMethod(name);method.setAccessible(true);method.invoke(game);}
    private static void key(KeyCode code,boolean down){game.fireEvent(new KeyEvent(down?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,"","",code,false,false,false,false));}
    private static void later(int ms,Step action){PauseTransition pause=new PauseTransition(Duration.millis(ms));pause.setOnFinished(e->{try{action.run();}catch(Throwable failure){fail(failure);}});pause.play();}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void snapshot(String name)throws Exception {
        game.applyCss();game.layout();var image=stage.getScene().snapshot(null);
        var png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Files.createDirectories(EVIDENCE);javax.imageio.ImageIO.write(png,"png",EVIDENCE.resolve(name).toFile());
    }
    private static void fail(Throwable failure){failure.printStackTrace();System.exit(1);}
    private interface Step{void run()throws Exception;}
}
