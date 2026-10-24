package model.history;

import java.util.*;
import static model.history.RaceData.*;

/** Deterministic lap-wise educational free-track model, not a race prediction. */
public final class StrategySimulator {
    public record Stop(int afterLap,String compound,int age){}
    public record Strategy(String initialCompound,int initialAge,List<Stop> stops){public Strategy{stops=List.copyOf(stops);}}
    public record Parameters(double base,Map<String,Double> compoundEffects,double degradation,double fuelTrend,double pitLoss,double warmup,double startLoss){public Parameters{compoundEffects=Map.copyOf(compoundEffects);}}
    public record Result(List<Double> lapSeconds,List<Double> cumulative,double total){public Result{lapSeconds=List.copyOf(lapSeconds);cumulative=List.copyOf(cumulative);}}
    public record Comparison(Result historical,Result alternative,double delta,double sensitivityLow,double sensitivityHigh){}

    public Result simulate(int laps,Strategy strategy,Parameters p){
        if(laps<1||laps>500||!Double.isFinite(p.base())||p.base()<=0||!finiteNonnegative(p.degradation())||!finiteNonnegative(p.pitLoss())||!finiteNonnegative(p.warmup())||!finiteNonnegative(p.startLoss())||!Double.isFinite(p.fuelTrend()))throw new IllegalArgumentException("Parámetros del modelo inválidos.");
        requireCompound(strategy.initialCompound(),p);if(strategy.initialAge()<0)throw new IllegalArgumentException("Edad inicial negativa.");int previous=0;
        for(Stop s:strategy.stops()){if(s.afterLap()<=previous||s.afterLap()>=laps||s.age()<0)throw new IllegalArgumentException("Las paradas deben ser únicas, crecientes y anteriores a la última vuelta.");requireCompound(s.compound(),p);previous=s.afterLap();}
        List<Double> times=new ArrayList<>(),cumulative=new ArrayList<>();int age=strategy.initialAge(),next=0;String compound=strategy.initialCompound();double total=0;
        for(int lap=1;lap<=laps;lap++){
            double time=p.base()+p.compoundEffects().get(compound)+p.degradation()*age-p.fuelTrend()*(lap-1)+(age==0?p.warmup():0)+(lap==1?p.startLoss():0);
            if(next<strategy.stops().size()&&strategy.stops().get(next).afterLap()==lap)time+=p.pitLoss();
            if(!Double.isFinite(time)||time<=0)throw new IllegalArgumentException("El modelo genera un tiempo por vuelta no válido.");
            times.add(time);total+=time;cumulative.add(total);age++;
            if(next<strategy.stops().size()&&strategy.stops().get(next).afterLap()==lap){Stop stop=strategy.stops().get(next++);compound=stop.compound();age=stop.age();}
        }
        return new Result(times,cumulative,total);
    }
    public Comparison compare(int laps,Strategy historical,Strategy alternative,Parameters p){
        Result h=simulate(laps,historical,p),a=simulate(laps,alternative,p);double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        for(double d:List.of(0.8,1.2))for(double loss:List.of(0.8,1.2)){
            Parameters q=new Parameters(p.base(),p.compoundEffects(),p.degradation()*d,p.fuelTrend(),p.pitLoss()*loss,p.warmup(),p.startLoss());
            double delta=simulate(laps,alternative,q).total()-simulate(laps,historical,q).total();low=Math.min(low,delta);high=Math.max(high,delta);
        }
        return new Comparison(h,a,a.total()-h.total(),low,high);
    }
    public Strategy historical(RaceData data,String driver){
        int distance=data.driver(driver).orElseThrow().completedLaps();List<Stint> stints=data.stints().stream().filter(s->s.driverId().equals(driver)).sorted(Comparator.comparingInt(Stint::firstLap)).toList();
        if(stints.isEmpty()||stints.get(0).firstLap()!=1||stints.get(stints.size()-1).lastLap()!=distance)throw new IllegalArgumentException("La cobertura de stints no permite reconstruir toda la distancia observada del piloto.");
        List<Stop> stops=new ArrayList<>();for(int i=0;i<stints.size();i++){Stint s=stints.get(i);if(s.initialAge()==null||s.compound().equals("UNKNOWN"))throw new IllegalArgumentException("Falta compuesto o edad inicial observada.");if(i>0){Stint prior=stints.get(i-1);if(prior.lastLap()+1!=s.firstLap())throw new IllegalArgumentException("Hay huecos en los stints.");stops.add(new Stop(prior.lastLap(),s.compound(),s.initialAge()));}}
        return new Strategy(stints.get(0).compound(),stints.get(0).initialAge(),stops);
    }
    public Strategy parseAlternative(String initial,int age,String text){
        List<Stop> stops=new ArrayList<>();if(text!=null&&!text.isBlank())for(String part:text.split(";")){String[] p=part.trim().split(":");if(p.length<2||p.length>3)throw new IllegalArgumentException("Usa vuelta:COMPUESTO[:edad]; por ejemplo 17:HARD;37:SOFT:3. Sin edad se asume juego nuevo.");try{stops.add(new Stop(Integer.parseInt(p[0].trim()),p[1].trim().toUpperCase(Locale.ROOT),p.length==3?Integer.parseInt(p[2].trim()):0));}catch(NumberFormatException e){throw new IllegalArgumentException("La vuelta y la edad deben ser enteros.");}}
        return new Strategy(initial,age,stops);
    }
    public double calibratedBase(RaceData data,String driver,Parameters assumptions){
        HistoryAnalysis analysis=new HistoryAnalysis();List<Double> adjusted=new ArrayList<>();
        for(Lap l:analysis.cleanLaps(data,driver))for(Stint s:data.stints())if(s.driverId().equals(driver)&&l.number()>=s.firstLap()&&l.number()<=s.lastLap()&&s.initialAge()!=null&&assumptions.compoundEffects().containsKey(s.compound())){
            adjusted.add(l.seconds()-assumptions.compoundEffects().get(s.compound())-assumptions.degradation()*(s.initialAge()+l.number()-s.firstLap())+assumptions.fuelTrend()*(l.number()-1));break;
        }
        Double base=HistoryAnalysis.median(adjusted);if(base==null||adjusted.size()<4)throw new IllegalArgumentException("Se necesitan al menos cuatro vueltas comparables para calibrar el tiempo base.");return base;
    }
    private void requireCompound(String name,Parameters p){Double d=p.compoundEffects().get(name);if(d==null||!Double.isFinite(d))throw new IllegalArgumentException("Falta un efecto estimado para el compuesto "+name+".");}
    private boolean finiteNonnegative(double n){return Double.isFinite(n)&&n>=0;}
}
