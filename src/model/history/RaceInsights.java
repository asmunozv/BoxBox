package model.history;

import java.util.*;
import static model.history.RaceData.*;

/** Evidence-linked race narration; rule outputs never claim counterfactual causation. */
public final class RaceInsights {
    public record Insight(int lap,String driverId,String title,String evidence){public String toString(){return (lap>0?"V"+lap+" · ":"")+title+"\n"+evidence;}}
    public List<Insight> narrate(RaceData data){
        List<Insight> out=new ArrayList<>();
        for(Entry e:data.raceEntries())if(Objects.equals(e.position(),1))out.add(new Insight(0,e.driverId(),"Resultado observado: "+e.driver()+" terminó P1","Parrilla: "+(e.grid()==null?"sin dato":e.grid()==0?"pit lane":"P"+e.grid())+"; puntos oficiales: "+e.points()+". Resultado y salida no demuestran qué decisión causó la victoria."));
        for(Stint s:data.stints())if(s.firstLap()>1){String name=data.driver(s.driverId()).map(Entry::driver).orElse(s.driverId());out.add(new Insight(s.firstLap(),s.driverId(),name+" comienza stint con "+s.compound(),"Fuente observada: stint "+s.number()+", vueltas "+s.firstLap()+"–"+s.lastLap()+", edad inicial "+(s.initialAge()==null?"desconocida":s.initialAge())+". Un stint es una tanda con un juego de neumáticos."));}
        for(Pit p:data.pits()){String name=data.driver(p.driverId()).map(Entry::driver).orElse(p.driverId());out.add(new Insight(p.lap(),p.driverId(),name+": paso por pits registrado","Tránsito/duración según fuente: "+Timing.number(p.laneSeconds())+" s; estacionario: "+Timing.number(p.stationarySeconds())+" s. No son la pérdida neta frente a permanecer en pista."));}
        for(Entry e:data.raceEntries()){
            List<Lap> laps=data.laps().stream().filter(l->l.driverId().equals(e.driverId())&&l.position()!=null).sorted(Comparator.comparingInt(Lap::number)).toList();
            for(int i=1;i<laps.size();i++){Lap a=laps.get(i-1),b=laps.get(i);if(b.number()==a.number()+1&&Math.abs(a.position()-b.position())>=3)out.add(new Insight(b.number(),e.driverId(),e.driver()+": P"+a.position()+" → P"+b.position(),"Cambio calculado entre vueltas consecutivas observadas. Puede incluir paradas, abandonos, incidentes o adelantamientos; no se atribuye automáticamente a pits."));}
        }
        for(Event e:data.events()){String text=e.message().toUpperCase(Locale.ROOT);if(text.contains("SAFETY CAR")||text.contains("RED FLAG")||text.contains("SESSION SUSPENDED"))out.add(new Insight(e.lap()==null?0:e.lap(),null,"Control de carrera: "+e.category(),e.message()+" · "+e.date()+". Se conserva el mensaje; SC/VSC pueden alterar ritmo y coste de parada."));}
        if(out.isEmpty())out.add(new Insight(0,null,"Faltan observaciones para narrar esta carrera","Prepara resultados, vueltas y estrategia. No se generan eventos ni explicaciones de decisiones ausentes."));
        return out.stream().sorted(Comparator.comparingInt(Insight::lap).thenComparing(Insight::title)).toList();
    }
}
