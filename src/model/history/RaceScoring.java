package model.history;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import static model.history.RaceData.*;

/** Explains published points conservatively; never recalculates or overwrites the official total. */
public final class RaceScoring {
    private static final List<Integer> GRAND_PRIX=List.of(25,18,15,12,10,8,6,4,2,1);
    private static final List<Integer> SPRINT_2021=List.of(3,2,1);
    private static final List<Integer> SPRINT=List.of(8,7,6,5,4,3,2,1);

    public record Breakdown(Double position,Double fastestLap,double official,String explanation) {
        public String label() {
            if(position==null)return number(official);
            return fastestLap!=null&&fastestLap>0
                ?number(position)+" + "+number(fastestLap)+" VR = "+number(official)
                :number(official);
        }
    }

    public Breakdown explain(RaceInfo race,Entry entry) {
        int year=race.season();Integer position=entry.position();
        String observed=entry.driver()+": "+number(entry.points())+" puntos oficiales. ";
        if(position!=null&&position>0) {
            if(entry.race()&&year>=2010) {
                double base=position<=GRAND_PRIX.size()?GRAND_PRIX.get(position-1):0;
                boolean bonus=year>=2019&&year<=2024&&position<=10
                    &&Objects.equals(entry.fastestLapRank(),1)&&entry.points()==base+1;
                if(bonus)return new Breakdown(base,1d,entry.points(),observed
                    +number(base)+" por P"+position+" + 1 por vuelta rápida (VR), identificada en la fuente. "
                    +"Ganar otorga 25 puntos con la escala completa; el extra de VR estuvo vigente de 2019 a 2024 para pilotos del top 10.");
                if(entry.points()==base)return new Breakdown(base,0d,entry.points(),observed
                    +number(base)+" por P"+position+", compatible con la escala completa de carrera. "
                    +(year>=2025?"Desde 2025 no se otorga el punto extra por vuelta rápida.":"No se añade un extra por vuelta rápida a este resultado."));
            } else if(!entry.race()&&year>=2021) {
                List<Integer> scale=year==2021?SPRINT_2021:SPRINT;
                double base=position<=scale.size()?scale.get(position-1):0;
                if(entry.points()==base)return new Breakdown(base,0d,entry.points(),observed
                    +number(base)+" por P"+position+" en sprint. El sprint usa una escala distinta a la carrera y no otorga punto por vuelta rápida.");
            }
        }
        return new Breakdown(null,null,entry.points(),observed
            +"Se conserva el total de la fuente sin inferir su desglose: puede corresponder a otra escala histórica, puntos reducidos o ajustes. "
            +"Un total diferente por sí solo no demuestra una bonificación por vuelta rápida.");
    }

    public static String number(double value) {
        return value==Math.rint(value)?String.format(Locale.ROOT,"%.0f",value):String.format(Locale.ROOT,"%.2f",value);
    }
}
