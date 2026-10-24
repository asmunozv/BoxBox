package verification;

import java.net.URI;
import java.nio.file.*;
import javax.imageio.ImageIO;
import model.util.LocalResources;
import view.LocalImageService;

/** Decodes the preserved images and resolves media from a non-project working directory. */
public final class ResourceChecks {
    public static void main(String[] args)throws Exception {
        Path root=LocalResources.projectRoot();int photos=0,audio=0;
        try(var files=Files.list(root.resolve("assets/images/drivers"))) {
            for(Path file:files.filter(Files::isRegularFile).toList()) {
                String filename=file.getFileName().toString();String id=filename.substring(0,filename.lastIndexOf('.'));
                String resolved=LocalImageService.resolveDriverImage(id,id);
                require(resolved!=null&&Path.of(URI.create(resolved)).equals(file));
                var image=ImageIO.read(file.toFile());require(image!=null&&image.getWidth()>1&&image.getHeight()>1);photos++;
                require(LocalResources.driverAudio(id,null).filter(p->{try{return Files.size(p)>100;}catch(Exception e){return false;}}).isPresent());audio++;
            }
        }
        require(photos>=22&&audio==photos);
        require(LocalImageService.resolveDriverImage("piastri",null).equals(LocalImageService.resolveDriverImage("oscar_piastri",null)));
        require(LocalResources.driverAudio("nico_hulkenberg",null).equals(LocalResources.driverAudio("hulkenberg",null)));
        require(LocalImageService.resolveDriverImage("SYNTHETIC_MISSING_RESOURCE",null)==null);
        require(LocalResources.find("assets/video/login.mp4","missing").isPresent());
        require(LocalResources.find("assets/audio/log_in/log.mp3").isPresent());
        require(LocalResources.find("assets/images/maps/world_map_light.png").isPresent());
        require(LocalResources.find("../outside-boxbox.mp3").isEmpty());
        require(!Path.of("").toAbsolutePath().normalize().equals(root));
        System.out.println("LOCAL_RESOURCE_CHECKS_PASSED: "+photos+" original driver photos decoded; "+audio+" driver audios, aliases, map, login music/video; lookup works outside project directory; missing photos stay offline");
    }
    private static void require(boolean condition){if(!condition)throw new AssertionError("Local resource verification failed");}
}
