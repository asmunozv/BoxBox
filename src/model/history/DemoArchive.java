package model.history;

import com.google.gson.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static model.history.RaceData.*;

/** Portable, attributed public racing observations. Never includes app_users or credentials. */
public final class DemoArchive {
    public record RaceSnapshot(RaceInfo race,List<Document> documents){}
    public record Bundle(int format,String purpose,List<RaceSnapshot> races){}
    private final Gson gson=new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,(JsonSerializer<LocalDate>)(d,t,c)->new JsonPrimitive(d.toString()))
        .registerTypeAdapter(LocalDate.class,(JsonDeserializer<LocalDate>)(d,t,c)->LocalDate.parse(d.getAsString()))
        .registerTypeAdapter(Instant.class,(JsonSerializer<Instant>)(d,t,c)->new JsonPrimitive(d.toString()))
        .registerTypeAdapter(Instant.class,(JsonDeserializer<Instant>)(d,t,c)->Instant.parse(d.getAsString()))
        .disableHtmlEscaping().create();
    public void exportYear(int year,Path file)throws Exception{
        List<RaceSnapshot> races=new ArrayList<>();HistoryRepository repo=new HistoryRepository();
        for(RaceInfo r:repo.races())if(r.season()==year)races.add(new RaceSnapshot(r,repo.documents(r.key())));
        if(races.isEmpty())throw new IllegalArgumentException("No hay datos preparados para exportar.");
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file,gson.toJson(new Bundle(1,"Datos públicos de F1 para demostración educativa sin internet; véase LICENSE-DATA.md",races)),StandardOpenOption.CREATE_NEW);
    }
    public int importFile(Path file)throws Exception{
        if(!Files.isRegularFile(file)||Files.size(file)>50_000_000)throw new IllegalArgumentException("Archivo demo ausente o demasiado grande.");
        Bundle bundle=gson.fromJson(Files.readString(file),Bundle.class);
        if(bundle==null||bundle.format()!=1||bundle.races()==null)throw new IllegalArgumentException("Formato demo no admitido.");
        List<RaceInfo> races=new ArrayList<>();List<Document> documents=new ArrayList<>();HistoryParser parser=new HistoryParser();Set<String> keys=new HashSet<>();
        for(RaceSnapshot snapshot:bundle.races()){
            RaceInfo r=snapshot.race();if(r==null||r.season()<1950||r.round()<1||!r.key().equals(r.season()+":"+r.round())||!keys.add(r.key()))throw new IllegalArgumentException("Identidad de carrera inválida en demo.");
            for(Document d:snapshot.documents())if(!r.key().equals(d.raceKey())||!HistoryRepository.digest(d.payload()).equals(d.sha256())||!Set.of("Jolpica","OpenF1").contains(d.provider()))throw new IllegalArgumentException("La huella o procedencia de la demo no es válida.");
            parser.parse(r,snapshot.documents());races.add(r);documents.addAll(snapshot.documents());
        }
        return new HistoryRepository().append(races,documents);
    }
}
