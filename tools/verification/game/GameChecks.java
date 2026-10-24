package verification.game;

import java.util.*;
import model.game.*;
import static model.game.GameRules.*;

/** Gameplay outcome/invariant checks through the public model API, with reproducible seeds. */
public final class GameChecks {
    private static int checks;
    private static long states;
    public static void main(String[] args) {
        reject(()->new Config(0,Difficulty.NORMAL,Track.COAST,WeatherPlan.DRY,TyreCompound.SOFT,1,false));
        RaceEngine invalid=engine(1,WeatherPlan.DRY,false);reject(()->invalid.advance(Double.NaN));reject(()->invalid.advance(-1));
        require(GameRules.points(1)==25&&GameRules.points(10)==1&&GameRules.points(0)==0,"position points");
        require(TyreCompound.SOFT.pace>TyreCompound.HARD.pace&&TyreCompound.SOFT.wearPerLap>TyreCompound.HARD.wearPerLap,"compound tradeoff");
        require(TyreCompound.WET.grip(0.95)>TyreCompound.SOFT.grip(0.95),"wet grip");
        require(TyreCompound.WET.wearMultiplier(0)>TyreCompound.WET.wearMultiplier(0.9),"wet tyres overheat on dry track");

        RaceEngine a=engine(11,WeatherPlan.DRY,false),b=engine(11,WeatherPlan.DRY,false);
        for(int i=0;i<200;i++)a.advance(0.1);for(int i=0;i<800;i++)b.advance(0.025);
        close(a.snapshot().player().distance(),b.snapshot().player().distance(),"fixed-step frame independence");
        a.pause();double clock=a.snapshot().elapsed(),distance=a.snapshot().player().distance();a.advance(5);close(a.snapshot().elapsed(),clock,"pause clock");close(a.snapshot().player().distance(),distance,"pause movement");a.resume();a.advance(1);require(a.snapshot().elapsed()>clock,"resume");

        RaceEngine conserving=engine(21,WeatherPlan.DRY,false),attacking=engine(21,WeatherPlan.DRY,false);
        conserving.setStyle(DrivingStyle.CONSERVE);attacking.setStyle(DrivingStyle.ATTACK);
        conserving.advance(30);attacking.advance(30);
        require(conserving.snapshot().player().wear()<attacking.snapshot().player().wear(),"style affects wear");
        require(conserving.snapshot().player().fuel()>attacking.snapshot().player().fuel(),"style affects fuel");
        RaceEngine battery=engine(31,WeatherPlan.DRY,false);battery.setEnergyMode(EnergyMode.DEPLOY);battery.advance(25);
        double depleted=battery.snapshot().player().energy();require(depleted<99,"ERS deployment consumes battery");battery.setEnergyMode(EnergyMode.HARVEST);battery.advance(15);require(battery.snapshot().player().energy()>depleted,"ERS harvest recharges");

        RaceEngine pits=engine(41,WeatherPlan.DRY,false);require(pits.requestPit(TyreCompound.HARD),"request pit");require(!pits.requestPit(TyreCompound.HARD),"cancel pit");pits.requestPit(TyreCompound.HARD);
        boolean entry=false,service=false,exit=false;double initialFuel=pits.snapshot().player().fuel(),entryGap=0;int entryXP=0;
        for(int i=0;i<1500&&pits.snapshot().player().stops()<1;i++) {
            pits.advance(0.1);var p=pits.snapshot().player();if(!entry&&p.pitPhase()==PitPhase.ENTRY){entryGap=p.gap();entryXP=p.strategyScore();}entry|=p.pitPhase()==PitPhase.ENTRY;service|=p.pitPhase()==PitPhase.SERVICE;exit|=p.pitPhase()==PitPhase.EXIT;
            if(p.pitPhase()==PitPhase.SERVICE)require(p.speed()==0,"stationary service");
        }
        require(entry&&service&&exit,"all pit phases");require(pits.snapshot().player().tyre()==TyreCompound.HARD&&pits.snapshot().player().wear()<0.01,"fresh requested tyres");
        require(pits.snapshot().player().strategyScore()==entryXP,"unnecessary fresh-tyre stop does not farm strategy XP");
        require(pits.snapshot().player().fuel()<initialFuel,"no refuelling");require(pits.snapshot().elapsed()>10,"pit clock advances");
        while(pits.snapshot().player().pitPhase()!=PitPhase.TRACK)pits.advance(0.1);
        require(pits.snapshot().player().gap()>entryGap+4,"pit stop costs competitive time");
        pits.requestPit(TyreCompound.MEDIUM);for(int i=0;i<2000&&pits.snapshot().player().stops()<2;i++)pits.advance(0.1);
        require(pits.snapshot().player().stops()==2,"multiple stops");

        RaceEngine limits=engine(51,WeatherPlan.DRY,false);limits.setAutoSteer(false);
        for(int violation=0;violation<3;violation++) {
            limits.steer(1);for(int i=0;i<40;i++)limits.advance(0.1);
            limits.steer(-1);while(limits.snapshot().player().lane()>0.1)limits.advance(0.1);
            limits.steer(0);limits.advance(0.2);
        }
        require(limits.snapshot().player().penalties()>=5,"track-limit penalty");

        Set<Flag> flags=EnumSet.noneOf(Flag.class);boolean weatherChange=false,aiPits=false,drs=false,slowStop=false;
        int races=0;
        for(Difficulty difficulty:Difficulty.values())for(WeatherPlan plan:WeatherPlan.values())for(int seed=1;seed<=2;seed++) {
            RaceEngine race=new RaceEngine(new Config(6,difficulty,Track.values()[seed%3],plan,plan==WeatherPlan.DRYING?TyreCompound.INTERMEDIATE:TyreCompound.MEDIUM,seed*81L+difficulty.ordinal()*17+plan.ordinal()*5,true));
            Weather first=race.snapshot().weather();int guard=0;
            while(race.snapshot().state()!=State.FINISHED&&guard++<12000) {
                RaceSnapshot s=race.snapshot();var player=s.player();
                if(player.pitPhase()==PitPhase.TRACK&&!player.pitRequested()&&player.laps()<5) {
                    TyreCompound wanted=TyreCompound.suitable(s.water(),6-player.laps());
                    boolean wrong=player.tyre().dry()&&s.water()>0.3||!player.tyre().dry()&&s.water()<0.12||player.tyre()==TyreCompound.INTERMEDIATE&&s.water()>0.8;
                    if(wrong||player.wear()>0.6||plan==WeatherPlan.DRY&&player.laps()==2&&player.stops()==0)race.requestPit(wanted==player.tyre()?TyreCompound.HARD:wanted);
                }
                race.advance(0.5);s=race.snapshot();flags.add(s.flag());weatherChange|=s.weather()!=first;
                for(var car:s.cars()) {
                    requireFinite(car);
                    if(!car.player()&&car.stops()>0)aiPits=true;
                    if(car.drs())drs=true;
                    if(car.drs()&&!(s.flag()==Flag.GREEN&&s.water()<0.18&&car.laps()>=2))throw new AssertionError("DRS eligibility");
                }
                slowStop|=s.events().stream().anyMatch(e->e.message().startsWith("Parada lenta"));
            }
            RaceSnapshot result=race.snapshot();require(result.state()==State.FINISHED,"race finishes "+difficulty+" "+plan);
            require(result.cars().size()==8&&result.cars().stream().allMatch(c->c.finished()||c.retired()),"complete classification");
            require(result.player().finished(),"balanced player can finish "+difficulty+" "+plan);
            require(result.player().lastLap()!=null&&result.player().bestLap()!=null,"lap timing");
            require(result.player().overtakes()<45,"overtakes require clearance, not side-by-side jitter");
            double previous=-1;for(var car:result.cars())if(car.finished()){double adjusted=car.finishTime()+car.penalties();require(adjusted>=previous,"penalty-adjusted ordering");previous=adjusted;}
            races++;
        }
        require(flags.contains(Flag.SC)&&flags.contains(Flag.VSC)&&flags.contains(Flag.GREEN),"both neutralisations and restart");
        require(weatherChange&&aiPits,"weather and rival strategy");require(drs,"DRS activated during dry running");
        require(slowStop,"seeded slow stop really occurred");
        for(int laps:List.of(10,16)) {
            RaceEngine longer=new RaceEngine(new Config(laps,Difficulty.EXPERT,Track.NIGHT,WeatherPlan.VARIABLE,TyreCompound.MEDIUM,8000+laps,true));
            int guard=0;
            while(longer.snapshot().state()!=State.FINISHED&&guard++<10000) {
                var s=longer.snapshot();var p=s.player();TyreCompound target=TyreCompound.suitable(s.water(),laps-p.laps());
                if(p.pitPhase()==PitPhase.TRACK&&!p.pitRequested()&&p.laps()<laps-1&&(p.wear()>0.55||p.tyre().dry()&&s.water()>0.3||!p.tyre().dry()&&s.water()<0.12))longer.requestPit(target);
                longer.advance(1);
            }
            require(longer.snapshot().state()==State.FINISHED&&longer.snapshot().player().finished(),"long race completes: "+laps);
        }

        GameProgress progress=new GameProgress();progress.beginChampionship();
        for(int round=0;round<3;round++) {
            RaceEngine race=engine(901+round,WeatherPlan.DRY,false);race.requestPit(TyreCompound.HARD);
            while(race.snapshot().state()!=State.FINISHED)race.advance(1);
            require(progress.record(race.snapshot(),true),"championship result recorded");require(!progress.record(race.snapshot(),true),"result recorded once");
        }
        require(progress.rounds()==3&&progress.standings().size()==8&&progress.stats().races()==3,"three-round progression");
        require(progress.stats().experience()>0&&progress.stats().bestLap()!=null,"XP and personal statistics");
        ArcadeSession arcade=new ArcadeSession(123);double x=arcade.snapshot().playerX();arcade.advance(0.2,-1);require(arcade.snapshot().playerX()<x,"classic keyboard movement");
        for(int i=0;i<2000&&arcade.snapshot().running();i++)arcade.advance(0.05,0);
        require(!arcade.snapshot().running()&&arcade.snapshot().score()>=0,"classic collision and scoring");
        System.out.println("GAME_MODEL_CHECKS_PASSED: "+checks+" targeted assertions and "+states+" car-state invariant checks; "+races+" six-lap GPs, 10/16-lap GPs and 3-round championship; pit phases, wet grip, resource tradeoffs, penalties, SC/VSC, DRS, slow stops and classic arcade");
    }
    private static RaceEngine engine(long seed,WeatherPlan weather,boolean incidents){return new RaceEngine(new Config(6,Difficulty.ROOKIE,Track.COAST,weather,TyreCompound.MEDIUM,seed,incidents));}
    private static void requireFinite(RaceSnapshot.Car car){states++;if(!Double.isFinite(car.distance())||!Double.isFinite(car.speed())||car.wear()<0||car.wear()>1||car.energy()<0||car.energy()>100||car.fuel()<0||!Double.isFinite(car.temperature())||car.finished()&&car.retired())throw new AssertionError("Resource/state invariant");}
    private static void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static void close(double a,double b,String message){require(Math.abs(a-b)<1e-7,message);}
    private static void reject(Runnable action){try{action.run();throw new AssertionError("Invalid input accepted");}catch(IllegalArgumentException expected){checks++;}}
}
