package model.game;

import java.util.List;
import static model.game.GameRules.*;

/** Immutable boundary between the simulation and its JavaFX presentation. All times are game seconds. */
public record RaceSnapshot(Config config,State state,double elapsed,Flag flag,double neutralisationLeft,
                           Weather weather,double water,String forecast,List<Car> cars,List<Event> events,long revision) {
    public RaceSnapshot {cars=List.copyOf(cars);events=List.copyOf(events);}
    public Car player(){return cars.stream().filter(Car::player).findFirst().orElseThrow();}
    public record Event(double time,String message) {}
    public record Car(int id,String name,Personality personality,boolean player,int position,int laps,
                      double distance,double lane,double speed,TyreCompound tyre,double wear,double temperature,
                      double fuel,double initialFuel,double energy,boolean drs,String drsReason,
                      PitPhase pitPhase,double pitRemaining,boolean pitRequested,int stops,
                      Double lastLap,Double bestLap,Double finishTime,double penalties,int warnings,
                      boolean retired,String status,double gap,int overtakes,int mistakes,int cleanLaps,int strategyScore) {
        public boolean finished(){return finishTime!=null;}
    }
}
