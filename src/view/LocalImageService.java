package view;

import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import model.util.LocalResources;

/** Uses the user's existing images offline. Missing photos never trigger web downloads. */
public final class LocalImageService {
    private LocalImageService() {}

    public static String resolveDriverImage(String id,String name) {
        return resolve("drivers",LocalResources.driverKeys(id,name));
    }
    public static String resolveConstructorImage(String id,String name) {
        return resolve("constructors",List.of(LocalResources.key(id),LocalResources.key(name)));
    }
    public static String resolveCircuitImage(String id,String name) {
        return resolve("circuits",List.of(LocalResources.key(id),LocalResources.key(name)));
    }
    public static String resolveRaceImage(String name,int season,int round) {
        return resolve("races",List.of(season+"_"+round,LocalResources.key(name+"_"+season+"_"+round),LocalResources.key(name)));
    }
    private static String resolve(String category,List<String> keys) {
        return LocalResources.category("images",category,keys,List.of(".jpg",".jpeg",".png",".gif"))
            .map(p->p.toUri().toString()).orElse(null);
    }
    public static Image load(String uri) {
        if(uri!=null) {
            Image image=new Image(uri,false);
            if(!image.isError()&&image.getWidth()>0)return image;
        }
        // An original, neutral silhouette, rather than an unrelated or unverified photograph.
        WritableImage image=new WritableImage(64,64);
        Color foreground=Color.web("#8295b2"),background=Color.web("#18253a");
        for(int y=0;y<64;y++)for(int x=0;x<64;x++) {
            boolean head=(x-32)*(x-32)+(y-22)*(y-22)<90;
            boolean body=y>36&&(x-32)*(x-32)+(y-64)*(y-64)<650;
            image.getPixelWriter().setColor(x,y,head||body?foreground:background);
        }
        return image;
    }
}
