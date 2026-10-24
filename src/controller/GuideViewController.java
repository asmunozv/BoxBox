package controller;

import java.io.IOException;
import java.util.function.IntConsumer;
import javafx.fxml.*;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

/** In-app onboarding with working shortcuts to the same race context. */
public final class GuideViewController extends BorderPane {
    @FXML private Button demo,summary,positions,simulation,narrative;

    public GuideViewController(IntConsumer openAnalysis,Runnable loadDemo) {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/view/GuideView.fxml"));
        loader.setRoot(this);loader.setController(this);
        try{loader.load();}catch(IOException e){throw new IllegalStateException("No se pudo abrir la guía de uso.",e);}
        demo.setOnAction(e->loadDemo.run());summary.setOnAction(e->openAnalysis.accept(0));
        positions.setOnAction(e->openAnalysis.accept(4));simulation.setOnAction(e->openAnalysis.accept(5));
        narrative.setOnAction(e->openAnalysis.accept(6));
    }
}
