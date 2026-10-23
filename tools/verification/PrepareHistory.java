package verification;

import model.history.*;

/** Explicit preparation command; subsequent exploration reads only MySQL. */
public final class PrepareHistory {
    public static void main(String[] args)throws Exception{
        int year=args.length>0?Integer.parseInt(args[0]):2024;
        int round=args.length>1?Integer.parseInt(args[1]):1;
        HistoryImportService importer=new HistoryImportService();
        System.out.println(importer.season(year,System.out::println).message());
        HistoryService service=new HistoryService();
        var race=service.races().stream().filter(r->r.season()==year&&r.round()==round).findFirst().orElseThrow();
        System.out.println(importer.detail(race,System.out::println).message());
        service.invalidate();var data=service.race(race);
        if(data.raceEntries().isEmpty())throw new AssertionError("Missing real results");
        if(year==2024&&round==1){
            var winner=data.raceEntries().stream().filter(e->Integer.valueOf(1).equals(e.position())).findFirst().orElseThrow();
            if(!winner.driverId().equals("max_verstappen")||winner.points()!=26||data.lapCount()!=57)throw new AssertionError("Golden historical result differs; review source before continuing");
        }
        System.out.println("REAL_HISTORY_PREPARED: races="+service.races().size()+"; entries="+data.raceEntries().size()+"; laps="+data.laps().size()+"; stints="+data.stints().size()+"; pits="+data.pits().size()+"; sources="+data.sources().size());
    }
}
