package model.history;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.*;
import static model.history.RaceData.*;

/** Deterministic, data-only statistics. Coverage and sample sizes are part of the result. */
public final class HistoryAnalysis {
    public record Stats(double points,int wins,int podiums,int entries,int races,Double grid,Double finish,Double qualifying,int qualifyingSamples){}
    public record Pace(Double median,int samples){}
    public record Progress(int round,double points,Integer finish){}
    public record Estimates(double degradation,double pitLoss,Map<String,Double> effects,String evidence){}
    public Estimates estimates(RaceData data,String driver){
        List<Lap> clean=cleanLaps(data,driver);Map<String,Double> medians=new HashMap<>();
        for(String compound:List.of("SOFT","MEDIUM","HARD")){
            Set<Integer> selected=new HashSet<>();for(Stint s:data.stints())if(s.driverId().equals(driver)&&s.compound().equals(compound))for(int n=s.firstLap();n<=s.lastLap();n++)selected.add(n);
            Double median=median(clean.stream().filter(l->selected.contains(l.number())).map(Lap::seconds).toList());if(median!=null)medians.put(compound,median);
        }
        double reference=medians.getOrDefault("HARD",medians.values().stream().findFirst().orElse(0d));Map<String,Double> effects=new LinkedHashMap<>();
        for(String compound:List.of("SOFT","MEDIUM","HARD"))effects.put(compound,medians.containsKey(compound)?medians.get(compound)-reference:0d);
        List<Double> trends=data.stints().stream().filter(s->s.driverId().equals(driver)).map(s->stintTrend(data,s)).filter(Objects::nonNull).toList();Double slope=median(trends);double degradation=slope==null?0:Math.max(0,slope);
        List<Double> losses=new ArrayList<>();
        for(Pit p:data.pits())if(p.driverId().equals(driver)){
            Double in=time(data,driver,p.lap()),out=time(data,driver,p.lap()+1);
            Double adjacent=median(clean.stream().filter(l->Math.abs(l.number()-p.lap())<=5).map(Lap::seconds).toList());
            if(in!=null&&out!=null&&adjacent!=null){double loss=in+out-2*adjacent;if(loss>0)losses.add(loss);}
        }
        Double pit=median(losses);
        String evidence="Parámetros iniciales aproximados: pendiente neta mediana de "+trends.size()+" stints (se limita a cero si es negativa); pérdida de pits = exceso de las vueltas de entrada+salida frente a dos vueltas vecinas ("+losses.size()+" muestras). Los efectos de compuesto son diferencias de medianas sin corregir combustible. Compuestos observados: "+medians.keySet()+"; los restantes parten del supuesto editable 0 s. No son coeficientes físicos identificados.";
        return new Estimates(degradation,pit==null?0:pit,Map.copyOf(effects),evidence);
    }
    public Double time(RaceData data,String driver,int lap){return data.laps().stream().filter(l->l.driverId().equals(driver)&&l.number()==lap).map(Lap::seconds).filter(Objects::nonNull).findFirst().orElse(null);}
    public Stats statistics(List<RaceData> season,String identity,boolean team){
        List<Entry> all=season.stream().flatMap(d->d.entries().stream()).filter(matches(identity,team)).toList();
        List<Entry> races=all.stream().filter(Entry::race).toList();
        double points=all.stream().map(e->BigDecimal.valueOf(e.points())).reduce(BigDecimal.ZERO,BigDecimal::add).doubleValue();
        int wins=(int)races.stream().filter(e->Objects.equals(e.position(),1)).count();
        int podiums=(int)races.stream().filter(e->e.position()!=null&&e.position()<=3).count();
        int rounds=(int)season.stream().filter(d->d.raceEntries().stream().anyMatch(matches(identity,team))).count();
        return new Stats(points,wins,podiums,races.size(),rounds,average(races.stream().map(Entry::grid).filter(n->n!=null&&n>0).toList()),
            average(races.stream().map(Entry::position).filter(Objects::nonNull).toList()),average(races.stream().map(Entry::qualifying).filter(Objects::nonNull).toList()),(int)races.stream().filter(e->e.qualifying()!=null).count());
    }
    public Predicate<Entry> matches(String identity,boolean team){return e->(team?e.teamId():e.driverId()).equals(identity);}
    private Double average(List<Integer> values){return values.isEmpty()?null:values.stream().mapToInt(Integer::intValue).average().orElseThrow();}
    public List<Progress> progress(List<RaceData> season,String id,boolean team){
        List<Progress> out=new ArrayList<>();double accumulated=0;
        for(RaceData d:season.stream().sorted(Comparator.comparingInt(r->r.race().round())).toList()){
            if(d.raceEntries().isEmpty())continue;
            accumulated+=d.entries().stream().filter(matches(id,team)).mapToDouble(Entry::points).sum();
            Integer finish=d.raceEntries().stream().filter(matches(id,team)).map(Entry::position).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
            out.add(new Progress(d.race().round(),accumulated,finish));
        }
        return out;
    }
    public List<Lap> cleanLaps(RaceData data,String id){
        Set<Integer> excluded=new HashSet<>(neutralized(data));
        data.pits().stream().filter(p->p.driverId().equals(id)).forEach(p->{excluded.add(p.lap());excluded.add(p.lap()+1);});
        return data.laps().stream().filter(l->l.driverId().equals(id)&&l.number()>1&&l.seconds()!=null&&l.seconds()>0&&!l.pitOut()&&!excluded.contains(l.number())).sorted(Comparator.comparingInt(Lap::number)).toList();
    }
    public Set<Integer> neutralized(RaceData data){
        Set<Integer> excluded=new HashSet<>();Integer start=null;
        for(Event e:data.events().stream().sorted(Comparator.comparing(x->x.date()==null?java.time.Instant.MIN:x.date())).toList()){
            String message=e.message().toUpperCase(Locale.ROOT);if(e.lap()==null)continue;
            if(message.contains("SAFETY CAR")&&message.contains("DEPLOYED"))start=e.lap();
            if(start!=null&&message.contains("SAFETY CAR")&&(message.contains("IN THIS LAP")||message.contains("ENDING"))){for(int i=start;i<=e.lap();i++)excluded.add(i);start=null;}
        }
        if(start!=null)for(int i=start;i<=data.lapCount();i++)excluded.add(i);
        return excluded;
    }
    public Pace pace(RaceData data,String id){List<Double> times=cleanLaps(data,id).stream().map(Lap::seconds).toList();return new Pace(median(times),times.size());}
    public Pace commonPace(RaceData data,String own,String other){Set<Integer> common=cleanLaps(data,other).stream().map(Lap::number).collect(Collectors.toSet());List<Double> times=cleanLaps(data,own).stream().filter(l->common.contains(l.number())).map(Lap::seconds).toList();return new Pace(median(times),times.size());}
    public Double stintTrend(RaceData data,Stint stint){
        List<Lap> laps=cleanLaps(data,stint.driverId()).stream().filter(l->l.number()>=stint.firstLap()&&l.number()<=stint.lastLap()).toList();
        if(laps.size()<4)return null;double sx=0,sy=0,sxx=0,sxy=0;
        for(Lap l:laps){double x=l.number()-stint.firstLap();sx+=x;sy+=l.seconds();sxx+=x*x;sxy+=x*l.seconds();}
        double denominator=laps.size()*sxx-sx*sx;return denominator==0?null:(laps.size()*sxy-sx*sy)/denominator;
    }
    public static Double median(List<Double> values){List<Double> sorted=values.stream().filter(Objects::nonNull).filter(Double::isFinite).sorted().toList();if(sorted.isEmpty())return null;int n=sorted.size();return n%2==1?sorted.get(n/2):(sorted.get(n/2-1)+sorted.get(n/2))/2;}
    public String compareExplanation(List<RaceData> season,String a,String b,String nameA,String nameB,boolean team){
        Stats x=statistics(season,a,team),y=statistics(season,b,team);long prepared=season.stream().filter(d->!d.raceEntries().isEmpty()).count();
        String text=String.format(Locale.ROOT,"%s sumó %.2f puntos y %s %.2f en %d carreras preparadas de %d. Se incluyen puntos sprint; victorias y podios cuentan solo carreras principales. ",nameA,x.points(),nameB,y.points(),prepared,season.size());
        if(x.qualifying()!=null&&y.qualifying()!=null)text+=String.format(Locale.ROOT,"Posición media de clasificación cronometrada: %.2f (%d inscripciones) frente a %.2f (%d). ",x.qualifying(),x.qualifyingSamples(),y.qualifying(),y.qualifyingSamples());
        else text+="No hay clasificación cronometrada suficiente para ambas selecciones. ";
        return text+"Las medias no ajustan circuitos, participación o fiabilidad; no equivalen a habilidad causal. Las sanciones de campeonato ajenas al resultado no se reconstruyen.";
    }
    public String pitExplanation(RaceData data,String a,String b){
        int window=5;
        for(Pit first:data.pits().stream().filter(p->p.driverId().equals(a)).toList())for(Pit second:data.pits().stream().filter(p->p.driverId().equals(b)).toList()){
            if(first.lap()==second.lap()||Math.abs(first.lap()-second.lap())>window)continue;
            int before=Math.min(first.lap(),second.lap())-1,after=Math.max(first.lap(),second.lap())+1;
            Integer ap=position(data,a,before),bp=position(data,b,before),aq=position(data,a,after),bq=position(data,b,after);
            if(ap!=null&&bp!=null&&aq!=null&&bq!=null&&ap>bp&&aq<bq){
                return "Cambio de orden observado entre vueltas "+before+" y "+after+", compatible con un posible "+(first.lap()<second.lap()?"undercut":"overcut")+" de la primera selección. Detector heurístico: paradas separadas por hasta "+window+" vueltas. El tráfico, banderas, ritmo y otras incidencias pueden explicar el cambio; no se demuestra causalidad.";
            }
        }
        return "No se identificó un intercambio de orden compatible con undercut/overcut en la ventana de cinco vueltas entre estas paradas, o faltan posiciones de referencia. Undercut: parar antes buscando aprovechar neumáticos nuevos. Overcut: permanecer en pista buscando un ritmo relativo favorable. La coincidencia temporal no prueba causalidad.";
    }
    public Integer position(RaceData data,String id,int lap){return data.laps().stream().filter(l->l.driverId().equals(id)&&l.number()==lap).map(Lap::position).filter(Objects::nonNull).findFirst().orElse(null);}
}
