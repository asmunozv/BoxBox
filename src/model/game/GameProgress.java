package model.game;

import java.util.*;

/** Session-only game progression, isolated from real driver accounts and championship statistics. */
public final class GameProgress {
    public record Standing(String driver,int points,int wins) {}
    public record Stats(int races,int wins,int podiums,int points,int experience,Double bestLap) {
        public int level(){return 1+experience/100;}
    }
    private final Set<Long> recorded=new HashSet<>();
    private final Map<String,Integer> championshipPoints=new HashMap<>(),championshipWins=new HashMap<>();
    private int rounds,races,wins,podiums,points,experience;
    private Double bestLap;
    public void beginChampionship(){rounds=0;championshipPoints.clear();championshipWins.clear();}
    public boolean record(RaceSnapshot race,boolean championship) {
        if(race.state()!=GameRules.State.FINISHED||!recorded.add(race.config().seed()))return false;
        RaceSnapshot.Car player=race.player();int earned=player.retired()?0:GameRules.points(player.position());
        races++;points+=earned;if(!player.retired()&&player.position()==1)wins++;if(!player.retired()&&player.position()<=3)podiums++;
        experience+=Math.max(0,earned+player.strategyScore()-Math.round((float)player.penalties()));
        if(player.bestLap()!=null)bestLap=bestLap==null?player.bestLap():Math.min(bestLap,player.bestLap());
        if(championship){rounds++;for(var car:race.cars()){championshipPoints.merge(car.name(),car.retired()?0:GameRules.points(car.position()),Integer::sum);if(!car.retired()&&car.position()==1)championshipWins.merge(car.name(),1,Integer::sum);}}
        return true;
    }
    public Stats stats(){return new Stats(races,wins,podiums,points,experience,bestLap);}
    public int rounds(){return rounds;}
    public List<Standing> standings(){return championshipPoints.entrySet().stream().map(e->new Standing(e.getKey(),e.getValue(),championshipWins.getOrDefault(e.getKey(),0)))
        .sorted(Comparator.comparingInt(Standing::points).reversed().thenComparing(Comparator.comparingInt(Standing::wins).reversed()).thenComparing(Standing::driver)).toList();}
}
