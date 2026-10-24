package model.game;

import java.util.*;

/** Preserved avoid-crashes game, now time-based so refresh rate does not change its difficulty. */
public final class ArcadeSession {
    public record Obstacle(double x,double y) {}
    public record Snapshot(double playerX,int score,boolean running,List<Obstacle> obstacles) {
        public Snapshot{obstacles=List.copyOf(obstacles);}
    }
    public static final double WIDTH=900,HEIGHT=560,CAR_WIDTH=36,CAR_HEIGHT=60,PLAYER_Y=480;
    private final Random random;
    private final List<Obstacle> obstacles=new ArrayList<>();
    private double x=432,spawnClock;
    private int score;
    private boolean running=true;
    public ArcadeSession(long seed){random=new Random(seed);}
    public void advance(double seconds,double steering) {
        if(!Double.isFinite(seconds)||seconds<0||seconds>1||!Double.isFinite(steering))throw new IllegalArgumentException("Entrada arcade inválida.");
        if(!running)return;x=Math.max(120,Math.min(744,x+Math.max(-1,Math.min(1,steering))*300*seconds));
        spawnClock+=seconds;
        while(spawnClock>=2d/3){spawnClock-=2d/3;obstacles.add(new Obstacle(120+random.nextDouble()*624,-CAR_HEIGHT));}
        for(int i=obstacles.size()-1;i>=0;i--) {
            Obstacle old=obstacles.get(i),next=new Obstacle(old.x(),old.y()+210*seconds);obstacles.set(i,next);
            if(x<next.x()+CAR_WIDTH&&x+CAR_WIDTH>next.x()&&PLAYER_Y<next.y()+CAR_HEIGHT&&PLAYER_Y+CAR_HEIGHT>next.y())running=false;
            if(next.y()>HEIGHT){obstacles.remove(i);score+=10;}
        }
    }
    public Snapshot snapshot(){return new Snapshot(x,score,running,obstacles);}
}
