package model.history;

import com.google.gson.*;
import java.time.*;
import java.util.*;
import static model.history.RaceData.*;
import static model.history.Timing.*;

/** Parses stored provider responses. No network and no UI; all missing fields are explicit. */
public final class HistoryParser {
    public List<JsonObject> raceObjects(String payload){
        List<JsonObject> out=new ArrayList<>();JsonObject envelope=JsonParser.parseString(payload).getAsJsonObject();
        for(JsonElement page:array(envelope,"pages"))for(JsonElement race:array(object(object(page.getAsJsonObject(),"MRData"),"RaceTable"),"Races"))out.add(race.getAsJsonObject());
        return out;
    }
    public RaceInfo info(JsonObject race){
        Integer season=integer(race,"season"),round=integer(race,"round");
        if(season==null||season<1950||round==null||round<1)throw new IllegalArgumentException("Temporada o ronda inválida en la fuente.");
        JsonObject circuit=object(race,"Circuit"),location=object(circuit,"Location");
        return new RaceInfo(season+":"+round,season,round,Objects.requireNonNull(text(race,"raceName")),LocalDate.parse(text(race,"date")),
            or(text(circuit,"circuitId"),"unknown"),or(text(circuit,"circuitName"),"No disponible"),text(location,"country"),text(location,"locality"),decimal(location,"lat"),decimal(location,"long"));
    }
    public RaceData parse(RaceInfo race,List<Document> documents){
        Map<String,Entry> entries=new LinkedHashMap<>();Map<String,JsonObject> qualifying=new HashMap<>();
        Map<String,Lap> laps=new TreeMap<>();List<Stint> stints=new ArrayList<>();List<Pit> pits=new ArrayList<>();List<Event> events=new ArrayList<>();List<Source> sources=new ArrayList<>();
        for(Document d:documents){sources.add(new Source(d.kind(),d.provider(),d.url(),d.sha256(),d.fetchedAt()));
            if(d.kind().equals("QUALIFYING"))for(JsonObject r:raceObjects(d.payload()))for(JsonElement q:array(r,"QualifyingResults")){JsonObject x=q.getAsJsonObject();qualifying.put(text(object(x,"Driver"),"driverId"),x);}
        }
        for(Document d:documents)if(Set.of("RESULTS","SPRINT").contains(d.kind())){
            String session=d.kind().equals("SPRINT")?"SPRINT":"RACE",field=session.equals("RACE")?"Results":"SprintResults";
            for(JsonObject r:raceObjects(d.payload()))for(JsonElement value:array(r,field)){
                JsonObject x=value.getAsJsonObject(),driver=object(x,"Driver"),team=object(x,"Constructor"),q=session.equals("RACE")?qualifying.get(text(driver,"driverId")):null;
                String id=Objects.requireNonNull(text(driver,"driverId"));Double points=decimal(x,"points");if(points==null||points<0)throw new IllegalArgumentException("Puntos oficiales ausentes o inválidos.");
                Double millis=decimal(object(x,"Time"),"millis");Integer number=integer(x,"number"),completed=integer(x,"laps");
                Entry e=new Entry(id,or(text(driver,"givenName"),"")+" "+or(text(driver,"familyName"),""),or(text(driver,"code"),id),number==null?0:number,
                    or(text(team,"constructorId"),"unknown"),or(text(team,"name"),"Sin escudería"),integer(x,"grid"),integer(x,"position"),points,completed==null?0:completed,or(text(x,"status"),"Sin estado"),
                    integer(q,"position"),seconds(text(q,"Q1")),seconds(text(q,"Q2")),seconds(text(q,"Q3")),millis==null?null:millis/1000,session);
                if(entries.putIfAbsent(session+":"+id,e)!=null)throw new IllegalArgumentException("Resultado duplicado en las páginas de la fuente.");
            }
        }
        Map<Integer,String> numbers=new HashMap<>();entries.values().stream().filter(Entry::race).forEach(e->{if(e.number()>0)numbers.put(e.number(),e.driverId());});
        for(Document d:documents){
            if(d.kind().equals("LAPS"))for(JsonObject r:raceObjects(d.payload()))for(JsonElement l:array(r,"Laps")){
                int n=Objects.requireNonNull(integer(l.getAsJsonObject(),"number"));
                for(JsonElement t:array(l.getAsJsonObject(),"Timings")){JsonObject x=t.getAsJsonObject();String id=text(x,"driverId");Lap lap=new Lap(id,n,integer(x,"position"),seconds(text(x,"time")),null,false);if(laps.putIfAbsent(id+":"+n,lap)!=null)throw new IllegalArgumentException("Vuelta duplicada.");}
            }
            if(d.kind().equals("PITS"))for(JsonObject r:raceObjects(d.payload()))for(JsonElement p:array(r,"PitStops")){
                JsonObject x=p.getAsJsonObject();Integer n=integer(x,"lap"),stop=integer(x,"stop");String id=text(x,"driverId");if(n!=null&&pits.stream().noneMatch(stopData->stopData.driverId().equals(id)&&stopData.lap()==n&&stopData.durationMeaning().contains("OpenF1")))pits.add(new Pit(id,stop==null?0:stop,n,seconds(text(x,"duration")),null,"Tránsito de pits Jolpica; puede incluir bandera roja"));
            }
            if(d.provider().equals("OpenF1"))for(JsonElement element:JsonParser.parseString(d.payload()).getAsJsonArray()){
                JsonObject x=element.getAsJsonObject();String id=numbers.get(integer(x,"driver_number"));
                if(d.kind().equals("STINTS")&&id!=null){Integer a=integer(x,"lap_start"),b=integer(x,"lap_end"),n=integer(x,"stint_number");if(a!=null&&b!=null&&n!=null&&a>0&&b>=a)stints.add(new Stint(id,n,a,b,or(text(x,"compound"),"UNKNOWN"),integer(x,"tyre_age_at_start")));}
                if(d.kind().equals("OPEN_PITS")&&id!=null){Integer n=integer(x,"lap_number");if(n!=null){pits.removeIf(p->p.driverId().equals(id)&&p.lap()==n);pits.add(new Pit(id,0,n,decimal(x,"lane_duration")!=null?decimal(x,"lane_duration"):decimal(x,"pit_duration"),decimal(x,"stop_duration"),"Tránsito por pit lane (OpenF1), no pérdida neta"));}}
                if(d.kind().equals("OPEN_LAPS")&&id!=null){Integer n=integer(x,"lap_number");if(n!=null){Lap old=laps.get(id+":"+n);boolean out=x.has("is_pit_out_lap")&&!x.get("is_pit_out_lap").isJsonNull()&&x.get("is_pit_out_lap").getAsBoolean();laps.put(id+":"+n,new Lap(id,n,old==null?null:old.position(),old!=null&&old.seconds()!=null?old.seconds():decimal(x,"lap_duration"),instant(text(x,"date_start")),out));}}
                if(d.kind().equals("FLAGS"))events.add(new Event(integer(x,"lap_number"),or(text(x,"category"),"Control"),or(text(x,"message"),"Sin mensaje"),instant(text(x,"date"))));
            }
        }
        return new RaceData(race,new ArrayList<>(entries.values()),new ArrayList<>(laps.values()),stints,pits,events,sources);
    }
}
