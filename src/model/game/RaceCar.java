package model.game;

import java.util.EnumSet;
import static model.game.GameRules.*;

/** Mutable state belongs exclusively to one RaceEngine, never to a JavaFX control. */
final class RaceCar {
    final int id;
    final String name;
    final Personality personality;
    final boolean player;
    final double pace,initialFuel;
    final EnumSet<TyreCompound> used=EnumSet.noneOf(TyreCompound.class);
    double distance,lane,speed,wear,temperature,fuel,energy=100,penalties;
    double phaseLeft,slowLeft,mistakeCooldown,offTrackTime,lastCrossing,laneTarget,laneHoldUntil;
    int laps,stops,warnings,trackViolations,overtakes,mistakes,lapMistakes,cleanLaps,strategyScore;
    boolean pitRequested,retired,offTrackLatched,drs;
    String drsReason="Disponible desde la vuelta 3",status="En pista";
    Double lastLap,bestLap,finishTime;
    TyreCompound tyre,pitTyre;
    PitPhase pit=PitPhase.TRACK;

    RaceCar(int id,String name,Personality personality,boolean player,double pace,int grid,Config config) {
        this.id=id;this.name=name;this.personality=personality;this.player=player;this.pace=pace;
        distance=-grid*12;lane=grid%2==0?-0.45:0.45;laneTarget=lane;tyre=config.initialTyre();
        if(!player&&tyre.dry())tyre=id%3==0?TyreCompound.SOFT:id%3==1?TyreCompound.MEDIUM:TyreCompound.HARD;
        pitTyre=tyre;used.add(tyre);temperature=tyre.optimumTemperature-12;
        initialFuel=config.laps()*2.14+0.9;fuel=initialFuel;
    }
    boolean done(){return finishTime!=null||retired;}
}
