package verification;

import java.time.*;
import java.util.*;
import model.history.*;
import static model.history.RaceData.*;
import static model.history.StrategySimulator.*;

/** Pure deterministic unit checks plus explicitly labelled real-data integration assertions. */
public final class AnalysisChecks {
    static int assertions;
    public static void main(String[] args)throws Exception{
        HistoryAnalysis analysis=new HistoryAnalysis();StrategySimulator simulator=new StrategySimulator();
        close(Timing.seconds("1:30.500"),90.5);close(Timing.seconds("40:55.302"),2455.302);require(Timing.seconds(null)==null);require(Timing.seconds("bad")==null);require(Timing.seconds("NaN")==null);
        close(HistoryAnalysis.median(List.of(1d,4d,2d,3d)),2.5);close(HistoryAnalysis.median(List.of(5d,1d,3d)),3);require(HistoryAnalysis.median(List.of())==null);
        Map<String,Double> zero=Map.of("SOFT",0d,"MEDIUM",0d,"HARD",0d);
        Parameters flat=new Parameters(100,zero,0,0,20,0,0);
        Strategy one=new Strategy("SOFT",0,List.of()),two=new Strategy("SOFT",0,List.of(new Stop(5,"HARD",0)));
        close(simulator.simulate(10,one,flat).total(),1000);close(simulator.simulate(10,two,flat).total(),1020);
        Parameters ageing=new Parameters(100,zero,1,0,20,0,0);
        close(simulator.simulate(10,one,ageing).total(),1045);close(simulator.simulate(10,two,ageing).total(),1040);
        close(simulator.compare(10,one,two,ageing).delta(),-5);close(simulator.compare(10,two,two,ageing).delta(),0);
        close(simulator.simulate(4,new Strategy("SOFT",3,List.of()),ageing).total(),418);
        var parsed=simulator.parseAlternative("SOFT",2,"4:HARD:3;8:SOFT");require(parsed.stops().size()==2&&parsed.stops().get(0).age()==3&&parsed.stops().get(1).age()==0);
        reject(()->simulator.simulate(10,new Strategy("SOFT",0,List.of(new Stop(5,"HARD",0),new Stop(5,"SOFT",0))),flat));
        reject(()->simulator.simulate(10,new Strategy("SOFT",0,List.of(new Stop(10,"HARD",0))),flat));
        reject(()->simulator.simulate(10,new Strategy("UNKNOWN",0,List.of()),flat));
        reject(()->simulator.simulate(10,one,new Parameters(Double.NaN,zero,0,0,0,0,0)));
        reject(()->simulator.simulate(10,one,new Parameters(100,zero,-1,0,0,0,0)));
        List<Entry> entries=List.of(entry("A",1,25,"RACE",1),entry("A",1,8,"SPRINT",1),entry("B",2,18,"RACE",null));
        RaceInfo synthetic=new RaceInfo("test",2000,1,"SYNTHETIC_UNIT_TEST",LocalDate.of(2000,1,1),"test","test","test","test",null,null);
        RaceData fake=new RaceData(synthetic,entries,List.of(new Lap("A",1,2,105d,null,false),new Lap("A",2,1,102d,null,false),new Lap("A",3,1,103d,null,false),new Lap("A",4,1,104d,null,false),new Lap("A",5,1,105d,null,false)),List.of(new Stint("A",1,1,5,"SOFT",0)),List.of(),List.of(),List.of());
        var stats=analysis.statistics(List.of(fake),"A",false);close(stats.points(),33);require(stats.wins()==1&&stats.podiums()==1&&stats.races()==1);close(analysis.stintTrend(fake,fake.stints().get(0)),1);
        require(analysis.cleanLaps(fake,"A").size()==4);close(analysis.pace(fake,"A").median(),103.5);
        require(analysis.statistics(List.of(fake),"B",false).qualifying()==null);
        HistoryService service=new HistoryService();var race=service.races().stream().filter(r->r.season()==2024&&r.round()==1).findFirst().orElseThrow();RaceData real=service.race(race);List<RaceData> season=service.season(2024);
        var max=analysis.statistics(season,"max_verstappen",false);close(max.points(),437);require(max.wins()==9&&max.podiums()==14);
        RaceScoring scoring=new RaceScoring();Entry winner=real.driver("max_verstappen").orElseThrow();
        var points=scoring.explain(race,winner);close(points.position(),25);close(points.fastestLap(),1);close(points.official(),26);require(points.label().equals("25 + 1 VR = 26"));require(winner.fastestLapRank()==1);
        require(new RaceInsights().narrate(real).stream().anyMatch(i->i.title().startsWith("Resultado observado")&&i.evidence().contains("25 + 1 VR = 26")));
        close(scoring.explain(race,real.driver("perez").orElseThrow()).position(),18);
        RaceInfo modern=new RaceInfo("2025:1",2025,1,"SYNTHETIC_SCORING_TEST",LocalDate.of(2025,1,1),"test","test","test","test",null,null);
        close(scoring.explain(modern,entry("A",1,25,"RACE",1)).position(),25);
        require(scoring.explain(modern,entry("A",1,26,"RACE",1)).position()==null);
        require(scoring.explain(race,entry("A",1,26,"RACE",1)).position()==null); // no fastest-lap evidence
        require(scoring.explain(race,entry("A",1,12.5,"RACE",1)).position()==null); // reduced points are not rewritten
        close(scoring.explain(race,entry("A",1,8,"SPRINT",1)).position(),8);
        require(scoring.explain(synthetic,entry("A",1,25,"RACE",1)).position()==null); // no modern scale imposed on old seasons
        require(real.raceEntries().size()==20&&real.lapCount()==57&&real.stints().size()==63&&real.pits().size()==43);
        require(real.pits().stream().allMatch(p->p.stationarySeconds()==null));
        var history=simulator.historical(real,"max_verstappen");require(history.initialAge()==3&&history.stops().get(0).afterLap()==17&&history.stops().get(1).afterLap()==37);
        var estimates=analysis.estimates(real,"max_verstappen");var assumptions=new Parameters(1,estimates.effects(),estimates.degradation(),0,estimates.pitLoss(),0,0);double base=simulator.calibratedBase(real,"max_verstappen",assumptions);var fitted=new Parameters(base,estimates.effects(),estimates.degradation(),0,estimates.pitLoss(),0,0);
        close(simulator.compare(57,history,history,fitted).delta(),0);require(simulator.simulate(57,history,fitted).total()>0);
        int documents=new HistoryRepository().documents(race.key()).size();require(new HistoryRepository().append(List.of(race),new HistoryRepository().documents(race.key()))==0);require(new HistoryRepository().documents(race.key()).size()==documents);
        System.out.println("ANALYSIS_CHECKS_PASSED: "+assertions+" assertions; pure arithmetic, invalid scenarios, stats/pace/stints, real 2024 totals and insert-only idempotency");
        System.out.println("REAL_MODEL_DESCRIPTIVE: base="+base+"; historicalModel="+simulator.simulate(57,history,fitted).total()+"; official="+real.driver("max_verstappen").orElseThrow().totalSeconds()+"; no prediction claim");
    }
    static Entry entry(String id,int pos,double points,String session,Integer qual){return new Entry(id,id,id,1,"T","TEST",pos,pos,points,5,"TEST",qual,null,null,null,null,session);}
    static void close(Double a,double b){require(a!=null&&Math.abs(a-b)<1e-7);}
    static void require(boolean condition){assertions++;if(!condition)throw new AssertionError("Check "+assertions+" failed");}
    static void reject(Runnable r){try{r.run();throw new AssertionError("Invalid input accepted");}catch(IllegalArgumentException expected){assertions++;}}
}
