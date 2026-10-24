package verification;

import application.Main;
import controller.UserSession;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.media.*;
import javafx.stage.*;
import javafx.util.Duration;
import model.db.DatabaseConnection;
import model.entity.Driver;

/** Real JavaFX toolkit, production Main/FXML/controllers and the restricted runtime account. */
public final class ApplicationSmoke {
    private static Stage stage;
    private static Parent firstLogin;
    private static Object miniGame;
    private static boolean finished;
    private static final Path EVIDENCE=Path.of("private/verification/ui-"+UUID.randomUUID());

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t,e)->fail(e));
        Thread watchdog=new Thread(()->{try{Thread.sleep(45000);if(!finished)fail(new AssertionError("UI timeout"));}catch(InterruptedException ignored){}},"boxbox-ui-watchdog");
        watchdog.setDaemon(true); watchdog.start();
        Platform.startup(()->{
            try {
                stage=new Stage();
                new Main().start(stage);
                require(stage.isShowing() && stage.getScene()!=null);
                firstLogin=stage.getScene().getRoot();
                require(firstLogin.getClass().getName().equals("controller.LoginViewController"));
                later(1200,ApplicationSmoke::login);
            } catch(Throwable e){fail(e);}
        });
    }

    private static void login() throws Exception {
        MediaPlayer music=(MediaPlayer)field(firstLogin,"loginMusic");
        List<MediaPlayer> videos=new ArrayList<>();collectVideoPlayers(firstLogin,videos);
        boolean audioPlaying=music!=null&&music.getError()==null&&music.getStatus()==MediaPlayer.Status.PLAYING;
        boolean videoPlaying=!videos.isEmpty()&&videos.stream().allMatch(p->p.getError()==null&&p.getStatus()==MediaPlayer.Status.PLAYING);
        System.out.println("MEDIA_PROBE: login audio="+(music==null?"UNAVAILABLE":music.getStatus())+"; video="+(videos.isEmpty()?"UNAVAILABLE":videos.get(0).getStatus()));
        if(audioPlaying&&videoPlaying)System.out.println("PASS: login audio and background video reached PLAYING without media errors");
        else System.out.println("MEDIA_PROBE_PENDING: inspect device/codec availability separately; do not claim playback verified");
        String username, password;
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT username,password FROM app_users ORDER BY id_user LIMIT 1");ResultSet r=p.executeQuery()){
            require(r.next());username=r.getString(1);password=r.getString(2);
        }
        ((TextField)field(firstLogin,"usernameField")).setText(username);
        ((PasswordField)field(firstLogin,"passwordField")).setText(password);
        ((Button)field(firstLogin,"loginButton")).fire();
        waitLogin();
    }
    private static void waitLogin(){later(250,()->{if(!stage.getScene().getRoot().getClass().getName().equals("controller.MainViewController")){waitLogin();return;}require(UserSession.getCurrentUserId()!=null);disposeMedia(firstLogin);System.out.println("PASS: production login FXML, real database authentication and main-window transition");later(1000,ApplicationSmoke::inspectMain);});}

    private static void inspectMain() throws Exception {
        Parent main=stage.getScene().getRoot();
        TabPane tabs=(TabPane)field(main,"tabPane");
        require(tabs.getTabs().size()==6);
        ((Button)((javafx.scene.layout.VBox)field(main,"legacyNavigation")).getChildren().get(2)).fire();
        stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
        Object drivers=tabs.getTabs().get(0).getContent();
        Object constructors=tabs.getTabs().get(1).getContent();
        Object races=tabs.getTabs().get(2).getContent();
        Object results=tabs.getTabs().get(3).getContent();
        Object bingo=tabs.getTabs().get(4).getContent();
        miniGame=tabs.getTabs().get(5).getContent();
        TableView<?> driverTable=(TableView<?>)field(drivers,"driverTable");
        TableView<?> constructorTable=(TableView<?>)field(constructors,"constructorTable");
        TableView<?> resultTable=(TableView<?>)field(results,"resultTable");
        require(!driverTable.getItems().isEmpty()&&!constructorTable.getItems().isEmpty());
        int season=(Integer)field(results,"currentSeason");
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM results r JOIN races ra ON ra.id_race=r.id_race WHERE ra.season=?")){
            p.setInt(1,season);try(ResultSet r=p.executeQuery()){r.next();require(r.getInt(1)==resultTable.getItems().size());}
        }
        require(((List<?>)field(bingo,"predictionFields")).size()==9);
        require(((List<?>)field(bingo,"predictionChecks")).size()==9);
        require(((javafx.scene.image.ImageView)field(races,"mapImageView")).getImage()!=null);
        call(races,"onRefresh");
        require(!((javafx.scene.layout.Pane)field(races,"mapOverlayPane")).getChildren().isEmpty());
        System.out.println("PASS: six original tabs; drivers="+driverTable.getItems().size()+"; constructors="+constructorTable.getItems().size()+"; results="+resultTable.getItems().size()+"; map markers and nine bingo slots loaded");
        snapshot("boxbox-main.png");

        // Open and close real modal profile/standings windows using the FX event loop.
        Driver max=driverTable.getItems().stream().map(x->(Driver)x)
            .filter(d->"max_verstappen".equals(d.getErgastDriverId())).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") TableView<Driver> typed=(TableView<Driver>)driverTable;
        typed.getSelectionModel().select(max);
        closeNextDialog();call(drivers,"onViewProfile");
        constructorTable.getSelectionModel().selectFirst();
        closeNextDialog();call(constructors,"onViewProfile");
        closeNextDialog();call(results,"onShowDriverStandings");
        closeNextDialog();call(results,"onShowConstructorStandings");
        System.out.println("PASS: driver/constructor profiles and both standings windows opened from production controllers");

        tabs.getSelectionModel().select(5);
        call(miniGame,"onStartGame");
        double initial=(Double)field(miniGame,"playerX");
        ((Node)miniGame).fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,"","",KeyCode.LEFT,false,false,false,false));
        later(300,()->{
            ((Node)miniGame).fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED,"","",KeyCode.LEFT,false,false,false,false));
            require((Long)field(miniGame,"frames")>0);
            require((Double)field(miniGame,"playerX")<initial);
            ((AnimationTimer)field(miniGame,"timer")).stop();
            call(miniGame,"onTabUnselected");
            System.out.println("PASS: minigame rendered, animation advanced and keyboard input moved the player");
            snapshot("boxbox-minigame.png");
            ((Button)field(main,"logoutButton")).fire();
            require(UserSession.getCurrentUserId()==null);
            require(stage.getScene().getRoot().getClass().getName().equals("controller.LoginViewController"));
            disposeMedia(stage.getScene().getRoot());
            Object music=field(stage.getScene().getRoot(),"loginMusic");
            if(music instanceof MediaPlayer player)player.dispose();
            System.out.println("PASS: logout returned to the production login view");
            finished=true;
            System.out.println("JAVAFX_APPLICATION_SMOKE_PASSED");
            stage.close();Platform.exit();
        });
    }

    private static void closeNextDialog(){later(700,()->{boolean found=false;for(Window w:new ArrayList<>(Window.getWindows()))if(w!=stage&&w.isShowing()){w.hide();found=true;}require(found);});}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void call(Object object,String name)throws Exception{Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);try{m.invoke(object);}catch(InvocationTargetException e){throw new Exception(e.getCause());}}
    private static void later(int milliseconds,Step step){PauseTransition pause=new PauseTransition(Duration.millis(milliseconds));pause.setOnFinished(e->Platform.runLater(()->{try{step.run();}catch(Throwable failure){fail(failure);}}));pause.play();}
    private static void disposeMedia(Node node){if(node instanceof MediaView view&&view.getMediaPlayer()!=null)view.getMediaPlayer().dispose();if(node instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable())disposeMedia(child);}
    private static void collectVideoPlayers(Node node,List<MediaPlayer> players){if(node instanceof MediaView view&&view.getMediaPlayer()!=null)players.add(view.getMediaPlayer());if(node instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable())collectVideoPlayers(child,players);}
    private static void snapshot(String name)throws Exception{
        WritableImage image=stage.getScene().snapshot(null);
        require(image.getWidth()>0&&image.getHeight()>0);
        BufferedImage png=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Path file=EVIDENCE.resolve(name);Files.createDirectories(file.getParent());
        if(Files.exists(file))throw new FileAlreadyExistsException(file.toString());
        ImageIO.write(png,"png",file.toFile());
    }
    private static void require(boolean condition){if(!condition)throw new AssertionError("UI verification failed");}
    private static void fail(Throwable e){if(finished)return;finished=true;for(Throwable cause=e;cause!=null;cause=cause.getCause()){System.err.println("JAVAFX_SMOKE_FAILED: "+cause.getClass().getSimpleName());for(StackTraceElement line:cause.getStackTrace())if(line.getClassName().startsWith("verification.")||line.getClassName().startsWith("controller.")){System.err.println("At "+line.getClassName()+"."+line.getMethodName()+":"+line.getLineNumber());break;}}System.exit(1);}
    @FunctionalInterface private interface Step{void run()throws Exception;}
}
