package model.game;

import java.util.*;
import static model.game.GameRules.*;

/** Seeded, fixed-step race simulation. No rendering, database, network or wall-clock dependency. */
public final class RaceEngine {
    private final Config config;
    private final Random random;
    private final RaceWeather weather;
    private final List<RaceCar> cars=new ArrayList<>();
    private final Deque<RaceSnapshot.Event> events=new ArrayDeque<>();
    private final RaceCar player;
    private final boolean[][] confirmedAhead=new boolean[GRID_SIZE][GRID_SIZE];
    private final double[][] passCooldown=new double[GRID_SIZE][GRID_SIZE];
    private final double incidentAt;
    private State state=State.RACING;
    private Flag flag=Flag.GREEN;
    private double elapsed,accumulator,neutralisationLeft,drsRelease,manualUntil,lastBattleMessage=-10;
    private double steering;
    private DrivingStyle style=DrivingStyle.BALANCED;
    private EnergyMode energyMode=EnergyMode.BALANCED;
    private boolean autoSteer=true,drsArmed=true,incidentDone,wetRace;
    private long revision;

    public RaceEngine(Config config) {
        this.config=Objects.requireNonNull(config);random=new Random(config.seed());weather=new RaceWeather(config.weather(),random);
        String[] names={"A. Vega","L. Costa","M. Ríos","S. Cruz","N. Silva","Tu piloto","D. León","I. Torres"};
        for(int i=0;i<GRID_SIZE;i++)cars.add(new RaceCar(i,names[i],Personality.values()[i%Personality.values().length],i==5,
            i==5?1.008:(0.985+random.nextDouble()*0.028)*config.difficulty().rivalPace,i,config));
        player=cars.get(5);incidentAt=0.32+random.nextDouble()*0.26;
        for(RaceCar car:cars)for(RaceCar rival:cars)confirmedAhead[car.id][rival.id]=car.distance>rival.distance;
        event("Semáforos apagados · "+config.laps()+" vueltas · rivales y circuitos ficticios");
    }

    public void advance(double seconds) {
        if(!Double.isFinite(seconds)||seconds<0||seconds>60)throw new IllegalArgumentException("Paso de tiempo inválido.");
        if(state!=State.RACING)return;
        accumulator+=seconds;
        while(accumulator+1e-9>=STEP&&state==State.RACING){accumulator=Math.max(0,accumulator-STEP);step(STEP);}
    }
    public void pause(){if(state==State.RACING){state=State.PAUSED;accumulator=0;steering=0;}}
    public void resume(){if(state==State.PAUSED)state=State.RACING;}
    public void setStyle(DrivingStyle value){style=Objects.requireNonNull(value);}
    public void setEnergyMode(EnergyMode value){energyMode=Objects.requireNonNull(value);}
    public void setAutoSteer(boolean value){autoSteer=value;}
    public void setDrsArmed(boolean value){drsArmed=value;}
    public void steer(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Dirección inválida.");steering=clamp(value,-1,1);}
    public boolean requestPit(TyreCompound tyre) {
        Objects.requireNonNull(tyre);
        if(state==State.FINISHED||player.done()||player.pit!=PitPhase.TRACK)return false;
        player.pitTyre=tyre;player.pitRequested=!player.pitRequested;
        event(player.pitRequested?"BOX confirmado: "+tyre+" al llegar a la entrada de pits":"Entrada a boxes cancelada");return player.pitRequested;
    }

    private void step(double dt) {
        List<RaceCar> order=order();Map<Integer,Integer> previous=positions(order);
        boolean[] onTrack=new boolean[GRID_SIZE];
        for(RaceCar car:cars)onTrack[car.id]=!car.done()&&car.pit==PitPhase.TRACK;
        double progress=progress();
        if(weather.advance(dt,progress))event("Clima: "+weather.weather()+" · el agua cambia gradualmente el agarre");
        wetRace|=weather.water()>=0.2;
        flags(dt,progress);
        for(RaceCar car:order) {
            if(car.done())continue;
            DrivingStyle driving=car.player?style:aiStyle(car);
            if(!car.player)planPit(car);
            RaceCar ahead=ahead(car,order);
            RaceCar follower=order.stream().filter(c->c!=car&&!c.done()&&c.distance<car.distance&&c.pit==PitPhase.TRACK).max(Comparator.comparingDouble(c->c.distance)).orElse(null);
            steerCar(car,ahead,follower,dt);
            double old=car.distance;
            if(car.pit!=PitPhase.TRACK)pitStep(car,dt);
            else drive(car,ahead,driving,dt);
            if(flag!=Flag.GREEN&&ahead!=null&&car.distance>ahead.distance-6)car.distance=Math.max(old,ahead.distance-6);
            double travelled=Math.max(0,car.distance-old);
            car.fuel=Math.max(0,car.fuel-travelled/config.track().length*2.05*driving.fuel);
            crossing(car,old,dt);
            if(car.fuel<=0&&!car.done()){car.retired=true;car.speed=0;car.drs=false;car.drsReason="Retirado";car.status="Abandono · sin combustible";event(car.name+" se queda sin combustible");}
        }
        elapsed+=dt;
        Map<Integer,Integer> current=positions(order());
        for(RaceCar car:cars) {
            int passed=0;
            for(RaceCar rival:cars)if(rival!=car&&car.distance>rival.distance+8&&!confirmedAhead[car.id][rival.id]) {
                confirmedAhead[car.id][rival.id]=true;confirmedAhead[rival.id][car.id]=false;
                boolean cleanPass=onTrack[car.id]&&onTrack[rival.id]&&car.pit==PitPhase.TRACK&&rival.pit==PitPhase.TRACK&&!car.retired&&!rival.retired&&flag==Flag.GREEN;
                if(cleanPass&&elapsed>=passCooldown[car.id][rival.id]){passed++;passCooldown[car.id][rival.id]=elapsed+12;passCooldown[rival.id][car.id]=elapsed+12;}
            }
            car.overtakes+=passed;
            if(car.player&&passed>0){car.strategyScore+=passed;event("Adelantamiento en pista: subes a P"+current.get(car.id));}
            else if(car.player&&previous.get(car.id)>current.get(car.id)&&flag==Flag.GREEN&&car.pit==PitPhase.TRACK&&elapsed-lastBattleMessage>=4){event("Batalla por P"+current.get(car.id)+" · la posición cambia al completar la maniobra");lastBattleMessage=elapsed;}
        }
        if(cars.stream().allMatch(RaceCar::done)) {state=State.FINISHED;flag=Flag.CHECKERED;event("Gran Premio terminado · clasificación corregida por penalizaciones");}
        if(elapsed>config.laps()*config.track().lapTime*4+180) {
            for(RaceCar car:cars)if(!car.done()){car.retired=true;car.status="Fuera del tiempo máximo del juego";}
            state=State.FINISHED;flag=Flag.CHECKERED;
        }
    }

    private void flags(double dt,double progress) {
        if(flag!=Flag.GREEN) {
            neutralisationLeft=Math.max(0,neutralisationLeft-dt);
            if(neutralisationLeft==0){flag=Flag.GREEN;drsRelease=elapsed+8;event("Bandera verde · reanudación, DRS aún bloqueado unos segundos");}
        }
        if(config.incidents()&&!incidentDone&&progress>=incidentAt) {
            incidentDone=true;flag=random.nextBoolean()?Flag.SC:Flag.VSC;neutralisationLeft=14+random.nextDouble()*9;
            RaceCar cause=cars.get(random.nextInt(cars.size()));cause.slowLeft=2.5;
            event("Restos en pista · "+flag+" · prohibido adelantar; boxes abiertos");
        }
    }

    private DrivingStyle aiStyle(RaceCar car) {
        double left=Math.max(0,config.laps()-car.distance/config.track().length);
        if(car.fuel<left*2.03||car.wear>0.7)return DrivingStyle.CONSERVE;
        return switch(car.personality){case AGGRESSIVE->DrivingStyle.ATTACK;case CONSERVATIVE->DrivingStyle.CONSERVE;default->DrivingStyle.BALANCED;};
    }

    private void planPit(RaceCar car) {
        if(car.pit!=PitPhase.TRACK||car.pitRequested)return;
        double left=config.laps()-car.distance/config.track().length;
        if(left<0.45)return;
        double water=weather.water();TyreCompound suitable=TyreCompound.suitable(water,left);
        boolean wrong=car.tyre.dry()&&water>(car.personality==Personality.STRATEGIST?0.27:0.36)
            ||!car.tyre.dry()&&water<0.12||car.tyre==TyreCompound.INTERMEDIATE&&water>0.78||car.tyre==TyreCompound.WET&&water<0.42;
        double threshold=car.personality==Personality.AGGRESSIVE?0.72:car.personality==Personality.CONSERVATIVE?0.57:0.65;
        boolean mandatory=!wetRace&&car.used.size()<2&&car.distance/config.track().length>config.laps()*0.48;
        if(wrong||car.wear>threshold&&left>0.8||mandatory) {
            car.pitRequested=true;car.pitTyre=suitable;
            if(car.pitTyre==car.tyre&&mandatory)car.pitTyre=car.tyre==TyreCompound.HARD?TyreCompound.MEDIUM:TyreCompound.HARD;
        }
    }

    private void steerCar(RaceCar car,RaceCar ahead,RaceCar follower,double dt) {
        if(car.pit!=PitPhase.TRACK)return;
        if(car.player&&steering!=0){car.lane=clamp(car.lane+steering*0.85*dt,-1.25,1.25);manualUntil=elapsed+1.8;}
        else if(!car.player||autoSteer&&elapsed>=manualUntil) {
            if(elapsed>=car.laneHoldUntil) {
                car.laneTarget=car.id%2==0?-0.34:0.34;car.laneHoldUntil=elapsed+1;
                if(ahead!=null&&ahead.distance-car.distance<65&&flag==Flag.GREEN){car.laneTarget=ahead.lane>=0?-0.68:0.68;car.laneHoldUntil=elapsed+4.5;}
                else if(!car.player&&car.personality==Personality.AGGRESSIVE&&follower!=null&&car.distance-follower.distance<30&&flag==Flag.GREEN){car.laneTarget=clamp(follower.lane,-0.65,0.65);car.laneHoldUntil=elapsed+3.5;}
            }
            car.lane+=clamp(car.laneTarget-car.lane,-dt*0.55,dt*0.55);
        }
    }

    private void drive(RaceCar car,RaceCar ahead,DrivingStyle driving,double dt) {
        double fraction=fraction(car.distance);
        double sector=fraction>0.38&&fraction<0.63?0.83:1.08;
        boolean braking=sector<1;
        double heatTarget=car.tyre.optimumTemperature+(driving==DrivingStyle.ATTACK?14:driving==DrivingStyle.CONSERVE?-8:0)-weather.water()*22;
        car.temperature+=(heatTarget-car.temperature)*dt*0.12;
        double temperaturePace=Math.max(0.86,1-Math.abs(car.temperature-car.tyre.optimumTemperature)*0.0015);
        double degradation=1-0.11*car.wear-0.28*Math.pow(Math.max(0,car.wear-0.6)/0.4,2);
        double speed=config.track().length/config.track().lapTime*car.pace*driving.pace*car.tyre.pace*car.tyre.grip(weather.water())*temperaturePace*degradation*sector;
        EnergyMode mode=car.player?energyMode:car.energy>30&&ahead!=null&&ahead.distance-car.distance<80?EnergyMode.DEPLOY:EnergyMode.BALANCED;
        double energyChange=braking?1.3:mode==EnergyMode.HARVEST?0.95:mode==EnergyMode.DEPLOY?-1.25:0.12;
        if(mode==EnergyMode.HARVEST)speed*=0.98;
        if(mode==EnergyMode.DEPLOY&&car.energy>3)speed*=1.055;
        car.energy=clamp(car.energy+energyChange*dt,0,100);
        updateDrs(car,ahead,fraction);
        if(car.drs)speed*=1.06;
        if(Math.abs(car.lane)>1) {
            speed*=0.72;car.offTrackTime+=dt;
            if(car.offTrackTime>1&&!car.offTrackLatched) {
                car.offTrackLatched=true;car.trackViolations++;car.warnings++;car.mistakes++;car.lapMistakes++;
                if(car.player)event("Límites de pista: aviso "+car.warnings+"/3");
                if(car.warnings==3){car.penalties+=5;car.warnings=0;if(car.player)event("Penalización del juego: +5 s por límites de pista");}
            }
        } else {car.offTrackTime=0;car.offTrackLatched=false;}
        car.mistakeCooldown=Math.max(0,car.mistakeCooldown-dt);
        if(config.incidents()&&car.mistakeCooldown==0&&random.nextDouble()<dt*0.00065*driving.risk*(1+weather.water()+car.wear)) {
            car.slowLeft=1.5+random.nextDouble()*2;car.mistakeCooldown=14;car.mistakes++;car.lapMistakes++;
            if(car.player)event("Corrección de volante: pierdes tiempo; conservar reduce el riesgo");
        }
        if(car.slowLeft>0){car.slowLeft=Math.max(0,car.slowLeft-dt);speed*=0.48;}
        if(flag!=Flag.GREEN)speed=Math.min(speed,config.track().length/config.track().lapTime*(flag==Flag.SC?0.48:0.62));
        if(ahead!=null&&ahead.distance-car.distance<17&&Math.abs(car.lane-ahead.lane)<0.38) {
            speed=Math.min(speed,Math.max(8,ahead.speed*0.985));
            if(car.player&&ahead.distance-car.distance<4&&car.mistakeCooldown==0) {
                car.slowLeft=2;car.mistakeCooldown=8;car.mistakes++;car.lapMistakes++;car.wear=clamp(car.wear+0.02,0,1);
                if(driving==DrivingStyle.ATTACK){car.penalties+=5;event("Contacto evitable atacando: +5 s y pérdida de ritmo");}else event("Contacto: pierdes impulso; cambia de trayectoria para adelantar");
            }
        }
        car.speed=Math.max(5,speed);
        double travel=car.speed*dt;
        if(ahead!=null&&Math.abs(car.lane-ahead.lane)<0.38&&ahead.distance>car.distance)travel=Math.min(travel,Math.max(0,ahead.distance-car.distance-3));
        if(car.pitRequested&&fraction<0.88&&fraction(car.distance+travel)>=0.88) {
            travel=(Math.floor(Math.max(0,car.distance)/config.track().length)+0.88)*config.track().length-car.distance;
            car.pit=PitPhase.ENTRY;car.phaseLeft=7;car.status="Entrada a boxes";
            if(car.player)event("Entras a boxes · se aplica el tiempo de tránsito y servicio");
        }
        car.distance+=Math.max(0,travel);
        car.wear=clamp(car.wear+Math.max(0,travel)/config.track().length*car.tyre.wearPerLap*driving.wear*config.track().abrasion
            *car.tyre.wearMultiplier(weather.water())*(Math.abs(car.lane)>1?1.7:1),0,1);
        if(car.pit==PitPhase.TRACK)car.status=car.slowLeft>0?"Recuperando control":car.drs?"DRS activo":"En pista";
    }

    private void updateDrs(RaceCar car,RaceCar ahead,double fraction) {
        String reason=flag!=Flag.GREEN?"Bloqueado por neutralización":weather.water()>=0.18?"Bloqueado por pista mojada":car.laps<2||elapsed<drsRelease?"Aún no autorizado":ahead==null||ahead.distance-car.distance>car.speed?"Rival a más de 1 s":!(fraction>=0.1&&fraction<=0.35||fraction>=0.67&&fraction<=0.86)?"Fuera de zona DRS":car.player&&!drsArmed?"Solicitud desactivada":"ACTIVO";
        car.drsReason=reason;car.drs=reason.equals("ACTIVO");
    }

    private void pitStep(RaceCar car,double dt) {
        car.drs=false;car.drsReason="En boxes";car.phaseLeft=Math.max(0,car.phaseLeft-dt);
        car.speed=car.pit==PitPhase.SERVICE?0:22;car.distance+=car.speed*dt;
        if(car.phaseLeft>0)return;
        switch(car.pit) {
            case ENTRY->{car.pit=PitPhase.SERVICE;car.speed=0;car.phaseLeft=2.4+random.nextDouble()*0.65;
                if(config.incidents()&&random.nextDouble()<0.12){car.phaseLeft+=2+random.nextDouble()*3;if(car.player)event("Parada lenta: problema con una rueda, +tiempo de servicio");}
                car.status="Mecánicos trabajando";}
            case SERVICE->{
                boolean adapts=car.tyre.dry()!=car.pitTyre.dry()||!car.tyre.dry()&&car.tyre!=car.pitTyre;
                boolean needed=car.wear>=0.4||!wetRace&&car.used.size()<2&&car.tyre!=car.pitTyre&&car.distance/config.track().length>=config.laps()*0.3;
                boolean opportunity=flag!=Flag.GREEN&&car.wear>=0.2;
                boolean suited=car.pitTyre==TyreCompound.suitable(weather.water(),config.laps()-car.distance/config.track().length);
                if(car.player&&suited&&(adapts||needed||opportunity)){car.strategyScore+=8;event("Decisión estratégica: +8 XP por una parada oportuna y un compuesto adecuado");}
                car.pit=PitPhase.EXIT;car.speed=22;car.phaseLeft=7;car.tyre=car.pitTyre;car.used.add(car.tyre);car.wear=0;car.temperature=car.tyre.optimumTemperature-20;car.stops++;car.status="Salida de boxes";}
            case EXIT->{car.pit=PitPhase.TRACK;car.pitRequested=false;car.status="En pista";if(car.player)event("Sales de boxes con "+car.tyre+" · no se reposta combustible");}
            default->{}
        }
    }

    private void crossing(RaceCar car,double old,double dt) {
        int completed=Math.min(config.laps(),Math.max(0,(int)Math.floor(car.distance/config.track().length)));
        if(completed<=car.laps)return;
        double line=completed*config.track().length;
        double at=elapsed+dt*clamp((line-old)/Math.max(1e-9,car.distance-old),0,1);
        car.lastLap=at-car.lastCrossing;car.lastCrossing=at;
        car.bestLap=car.bestLap==null?car.lastLap:Math.min(car.bestLap,car.lastLap);car.laps=completed;
        if(car.lapMistakes==0){car.cleanLaps++;if(car.player)car.strategyScore+=2;}car.lapMistakes=0;
        if(car.player)event("Vuelta "+completed+" completada · "+String.format(Locale.ROOT,"%.2f",car.lastLap)+" s");
        if(completed==config.laps()) {
            if(!wetRace&&car.used.size()<2){car.penalties+=15;if(car.player)event("Regla del juego: +15 s por no usar dos compuestos en carrera seca");}
            car.finishTime=at;car.distance=config.laps()*config.track().length;car.speed=0;car.drs=false;car.status="Finalizado";
        }
    }

    public RaceSnapshot snapshot() {
        List<RaceCar> order=order();RaceCar leader=order.getFirst();List<RaceSnapshot.Car> views=new ArrayList<>();int position=0;
        for(RaceCar car:order) {
            double gap=car.finishTime!=null&&leader.finishTime!=null?car.finishTime+car.penalties-leader.finishTime-leader.penalties:
                Math.max(0,(leader.distance-car.distance)/(config.track().length/config.track().lapTime));
            views.add(new RaceSnapshot.Car(car.id,car.name,car.personality,car.player,++position,car.laps,car.distance,car.lane,car.speed,car.tyre,car.wear,car.temperature,
                car.fuel,car.initialFuel,car.energy,car.drs,car.drsReason,car.pit,car.phaseLeft,car.pitRequested,car.stops,car.lastLap,car.bestLap,car.finishTime,car.penalties,car.warnings,
                car.retired,car.status,gap,car.overtakes,car.mistakes,car.cleanLaps,car.strategyScore));
        }
        return new RaceSnapshot(config,state,elapsed,flag,neutralisationLeft,weather.weather(),weather.water(),weather.forecast(progress(),config.laps()),views,new ArrayList<>(events),revision);
    }
    private List<RaceCar> order() {
        return cars.stream().sorted((a,b)->{
            if(a.retired!=b.retired)return a.retired?1:-1;
            if(a.finishTime!=null&&b.finishTime!=null)return Double.compare(a.finishTime+a.penalties,b.finishTime+b.penalties);
            if((a.finishTime!=null)!=(b.finishTime!=null))return a.finishTime!=null?-1:1;
            return Double.compare(b.distance,a.distance);
        }).toList();
    }
    private Map<Integer,Integer> positions(List<RaceCar> order){Map<Integer,Integer> out=new HashMap<>();for(int i=0;i<order.size();i++)out.put(order.get(i).id,i+1);return out;}
    private RaceCar ahead(RaceCar car,List<RaceCar> order){return order.stream().filter(c->c!=car&&!c.done()&&c.distance>car.distance&&c.pit==PitPhase.TRACK).min(Comparator.comparingDouble(c->c.distance)).orElse(null);}
    private double progress(){return clamp(cars.stream().mapToDouble(c->c.distance).max().orElse(0)/(config.laps()*config.track().length),0,1);}
    private double fraction(double distance){return Math.max(0,distance)/config.track().length%1;}
    private void event(String text){events.addFirst(new RaceSnapshot.Event(elapsed,text));while(events.size()>60)events.removeLast();revision++;}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
