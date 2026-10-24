package verification;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import model.history.HistoryService;

/** Runs the unchanged historical UI regression on its 2024 fixture, even if newer seasons were imported. */
public final class GameHistoryRegression {
    public static void main(String[] args)throws Exception {
        var calendar=new HistoryService().races();
        System.out.println("HISTORY_REGRESSION_CONTEXT: default archive selection="+(calendar.isEmpty()?"empty":calendar.getFirst().key())+"; explicit fixture=2024:1");
        HistoricalExplorerSmoke.main(args);
        Platform.runLater(()->{
            var scene=HistoricalExplorerSmoke.stage.getScene();
            java.util.function.Consumer<Object> select=root->{
                if(root.getClass().getSimpleName().equals("MainViewController"))try {
                    Object explorer=HistoricalExplorerSmoke.field(root,"explorer");
                    @SuppressWarnings("unchecked") ComboBox<Integer> season=(ComboBox<Integer>)HistoricalExplorerSmoke.field(explorer,"seasonCombo");
                    season.setValue(2024);
                } catch(Exception e){HistoricalExplorerSmoke.fail(e);}
            };
            scene.rootProperty().addListener((o,a,b)->select.accept(b));select.accept(scene.getRoot());
        });
    }
}
