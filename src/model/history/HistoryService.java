package model.history;

import java.util.*;
import static model.history.RaceData.*;

/** Database-only read facade; small bounded cache of parsed immutable race snapshots. */
public final class HistoryService {
    private final HistoryRepository repository=new HistoryRepository();
    private final HistoryParser parser=new HistoryParser();
    private final Map<String,RaceData> cache=new LinkedHashMap<>(16,0.75f,true){protected boolean removeEldestEntry(Map.Entry<String,RaceData> e){return size()>8;}};
    public List<RaceInfo> races()throws Exception{return repository.races();}
    public synchronized RaceData race(RaceInfo race)throws Exception{
        RaceData result=cache.get(race.key());if(result==null){result=parser.parse(race,repository.documents(race.key()));cache.put(race.key(),result);}return result;
    }
    public List<RaceData> season(int year)throws Exception{
        List<RaceData> out=new ArrayList<>();for(RaceInfo r:races())if(r.season()==year)out.add(race(r));return List.copyOf(out);
    }
    public synchronized void invalidate(){cache.clear();}
}
