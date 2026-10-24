package model.game;

import java.util.*;
import static model.game.GameRules.*;

/** Seeded forecast with gradual track wetting/drying rather than instantaneous grip switches. */
final class RaceWeather {
    private record Change(double fraction,Weather weather) {}
    private final List<Change> changes;
    private int index;
    private Weather weather;
    private double water;

    RaceWeather(WeatherPlan plan,Random random) {
        double shift=random.nextDouble()*0.06-0.03;
        changes=switch(plan) {
            case DRY->List.of(new Change(0,Weather.SUNNY),new Change(0.5,Weather.CLOUDY),new Change(0.78,Weather.SUNNY));
            case VARIABLE->List.of(new Change(0,Weather.SUNNY),new Change(0.23+shift,Weather.CLOUDY),new Change(0.44+shift,Weather.RAIN),new Change(0.74+shift,Weather.CLOUDY),new Change(0.87,Weather.SUNNY));
            case RAIN->List.of(new Change(0,Weather.CLOUDY),new Change(0.18,Weather.RAIN),new Change(0.5,Weather.STORM),new Change(0.85,Weather.RAIN));
            case DRYING->List.of(new Change(0,Weather.RAIN),new Change(0.22,Weather.CLOUDY),new Change(0.48,Weather.SUNNY));
        };
        weather=changes.getFirst().weather();water=plan==WeatherPlan.DRYING?0.7:0;
    }
    boolean advance(double dt,double progress) {
        boolean changed=false;
        while(index+1<changes.size()&&progress>=changes.get(index+1).fraction()){weather=changes.get(++index).weather();changed=true;}
        double target=switch(weather){case SUNNY,CLOUDY->0;case RAIN->0.7;case STORM->1;};
        double rate=target>water?0.023:weather==Weather.SUNNY?0.008:0.0035;
        water+=Math.signum(target-water)*Math.min(Math.abs(target-water),rate*dt);
        return changed;
    }
    Weather weather(){return weather;}
    double water(){return water;}
    String forecast(double progress,int laps) {
        if(index+1>=changes.size())return "Se mantiene "+weather.toString().toLowerCase(Locale.ROOT)+" hasta el final";
        Change next=changes.get(index+1);double remaining=Math.max(0,(next.fraction()-progress)*laps);
        return next.weather()+" en ≈"+String.format(Locale.ROOT,"%.1f",remaining)+" vueltas del líder";
    }
}
