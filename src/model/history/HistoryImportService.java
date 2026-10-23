package model.history;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import static model.history.RaceData.*;
import static model.history.Timing.*;

/** Downloads, validates the entire batch, then atomically appends immutable snapshots. */
public final class HistoryImportService {
    private final HistoricalApiClient api=new HistoricalApiClient();
    private final HistoryParser parser=new HistoryParser();
    private final HistoryRepository repository=new HistoryRepository();
    public record Report(int races,int documents,String message){}

    public Report season(int season,Consumer<String> progress)throws Exception{
        validateYear(season);progress.accept("Descargando calendario "+season+"…");
        var calendar=api.jolpica(season+".json","Races");
        Map<String,RaceInfo> races=new LinkedHashMap<>();
        for(JsonObject r:parser.raceObjects(envelope(calendar.pages()))) {RaceInfo info=parser.info(r);races.put(info.key(),info);}
        if(races.isEmpty())throw new IllegalArgumentException("La fuente no contiene calendario para esa temporada.");
        List<Document> docs=new ArrayList<>();
        String[][] kinds={{"RESULTS","results.json","Results"},{"QUALIFYING","qualifying.json","QualifyingResults"},{"SPRINT","sprint.json","SprintResults"}};
        for(String[] spec:kinds){progress.accept("Descargando "+spec[0]+" "+season+" con paginación completa…");
            var fetched=api.jolpica(season+"/"+spec[1],spec[2]);
            for(RaceInfo race:races.values()){String body=subset(fetched.pages(),race);if(body!=null)docs.add(document(race.key(),spec[0],"Jolpica",fetched.url(),body,fetched.time()));}
        }
        for(RaceInfo race:races.values()){List<Document> selected=docs.stream().filter(d->d.raceKey().equals(race.key())).toList();if(!selected.isEmpty())parser.parse(race,selected);}
        progress.accept("Validando y guardando instantáneas nuevas en MySQL…");
        int inserted=repository.append(new ArrayList<>(races.values()),docs);
        return new Report(races.size(),inserted,"Temporada preparada: "+races.size()+" carreras; "+inserted+" documentos nuevos. Las instantáneas anteriores y catálogos manuales se conservaron.");
    }
    public Report detail(RaceInfo race,Consumer<String> progress)throws Exception{
        Set<String> existing=new HashSet<>();repository.documents(race.key()).forEach(d->existing.add(d.kind()));
        List<Document> docs=new ArrayList<>();String prefix=race.season()+"/"+race.round()+"/";
        String[][] specs={{"RESULTS","results.json","Results"},{"QUALIFYING","qualifying.json","QualifyingResults"},{"SPRINT","sprint.json","SprintResults"},{"LAPS","laps.json","Laps"},{"PITS","pitstops.json","PitStops"}};
        for(String[] spec:specs){if(existing.contains(spec[0]))continue;progress.accept("Preparando "+spec[0]+" · "+race.name()+"…");var fetched=api.jolpica(prefix+spec[1],spec[2]);String body=subset(fetched.pages(),race);if(body!=null)docs.add(document(race.key(),spec[0],"Jolpica",fetched.url(),body,fetched.time()));}
        String enrichment="";
        if(race.season()>=2023){
            try{
                progress.accept("Buscando sesión histórica OpenF1 por fecha UTC…");
                JsonArray sessions=api.open("sessions?year="+race.season()+"&session_name=Race");List<JsonObject> matches=new ArrayList<>();
                for(JsonElement e:sessions){JsonObject s=e.getAsJsonObject();Instant date=instant(text(s,"date_start"));if(date!=null&&date.atOffset(ZoneOffset.UTC).toLocalDate().equals(race.date()))matches.add(s);}
                if(matches.size()!=1)enrichment="No hay una sesión OpenF1 inequívoca por fecha UTC; se mantienen los datos Jolpica.";
                else{
                    int key=Objects.requireNonNull(integer(matches.get(0),"session_key"));
                    String[][] open={{"STINTS","stints"},{"OPEN_PITS","pit"},{"OPEN_LAPS","laps"},{"FLAGS","race_control"}};
                    for(String[] spec:open)if(!existing.contains(spec[0])){progress.accept("OpenF1: "+spec[1]+" (respetando 30 consultas/minuto)…");String route=spec[1]+"?session_key="+key;JsonArray values=api.open(route);docs.add(document(race.key(),spec[0],"OpenF1",HistoricalApiClient.OPENF1+route,values.toString(),Instant.now()));}
                }
            }catch(java.io.IOException e){enrichment="Enriquecimiento OpenF1 incompleto: "+e.getMessage();}
        }else enrichment="OpenF1 no ofrece stints para esta temporada (cobertura desde 2023).";
        List<Document> all=new ArrayList<>(repository.documents(race.key()));all.addAll(docs);
        // Existing kinds are not downloaded again, so no observations are silently replaced.
        RaceData validated=parser.parse(race,all);
        if(validated.raceEntries().isEmpty())throw new IllegalArgumentException("No existen resultados publicados para esta carrera.");
        int added=repository.append(List.of(race),docs);
        return new Report(1,added,"Carrera preparada: "+validated.raceEntries().size()+" pilotos, "+validated.laps().size()+" vueltas observadas, "+validated.stints().size()+" stints. "+enrichment);
    }
    private void validateYear(int year){if(year<1950||year>LocalDate.now().getYear()+1)throw new IllegalArgumentException("Temporada fuera de rango.");}
    private String envelope(JsonArray pages){JsonObject object=new JsonObject();object.add("pages",pages);return object.toString();}
    private String subset(JsonArray pages,RaceInfo race){
        JsonArray filtered=new JsonArray();boolean any=false;
        for(JsonElement page:pages){JsonObject copy=page.getAsJsonObject().deepCopy();JsonObject table=object(object(copy,"MRData"),"RaceTable");JsonArray selection=new JsonArray();
            for(JsonElement e:array(table,"Races")){JsonObject r=e.getAsJsonObject();if(Objects.equals(integer(r,"season"),race.season())&&Objects.equals(integer(r,"round"),race.round())){selection.add(r);any=true;}}
            if(!selection.isEmpty()){table.add("Races",selection);filtered.add(copy);}
        }
        return any?envelope(filtered):null;
    }
    private Document document(String key,String kind,String provider,String url,String body,Instant date)throws Exception{
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        return new Document(key,kind,provider,url,body,digest,date);
    }
}
