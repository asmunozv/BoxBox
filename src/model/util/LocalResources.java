package model.util;

import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

/** Resolves the project's preserved resources, including when launched outside its directory. */
public final class LocalResources {
    private LocalResources() {}

    public static Path projectRoot() {
        try {
            Path location=Path.of(LocalResources.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path parent=location.getParent();
            if(Files.isDirectory(location.resolve("assets")))return location;
            if(parent!=null&&Files.isDirectory(parent.resolve("assets")))return parent;
        } catch(Exception ignored) {
            // The configured Eclipse working directory remains a valid fallback.
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    public static Optional<Path> find(String... candidates) {
        Path root=projectRoot();
        for(String candidate:candidates) {
            if(candidate==null||candidate.isBlank())continue;
            try {
                Path file=root.resolve(candidate).toAbsolutePath().normalize();
                if(file.startsWith(root)&&Files.isRegularFile(file))return Optional.of(file);
            } catch(InvalidPathException ignored) {
                // A malformed legacy path must not prevent lookup by the driver's identifier.
            }
        }
        return Optional.empty();
    }

    public static List<String> driverKeys(String id,String name) {
        String key=key(id);
        String alias=switch(key) {
            case "piastri" -> "oscar_piastri";
            case "nico_hulkenberg" -> "hulkenberg";
            default -> key;
        };
        return List.of(alias,key,key(name)).stream().filter(s->!s.isBlank()).distinct().toList();
    }

    public static Optional<Path> driverAudio(String id,String explicit) {
        Optional<Path> configured=find(explicit);
        return configured.isPresent()?configured:category("audio","drivers",driverKeys(id,null),List.of(".mp3",".wav",".m4a"));
    }

    public static Optional<Path> category(String type,String category,List<String> keys,List<String> extensions) {
        for(String key:keys)for(String prefix:List.of("assets/","src/assets/"))for(String extension:extensions) {
            Optional<Path> file=find(prefix+type+"/"+category+"/"+key+extension);
            if(file.isPresent())return file;
        }
        return Optional.empty();
    }

    public static String key(String value) {
        return value==null?"":Normalizer.normalize(value,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]+","_");
    }
}
