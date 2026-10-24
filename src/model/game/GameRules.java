package model.game;

import java.util.List;

/** Explicit balancing rules for the fictional, shortened Grands Prix. No historical data or JavaFX. */
public final class GameRules {
    private GameRules() {}
    public static final double STEP=0.1;
    public static final double CLOCK_RATE=3;
    public static final int GRID_SIZE=8;
    private static final List<Integer> POINTS=List.of(25,18,15,12,10,8,6,4,2,1);
    public static int points(int position){return position>0&&position<=POINTS.size()?POINTS.get(position-1):0;}

    public enum Difficulty {
        ROOKIE("Aprendiz",0.965),NORMAL("Competitivo",1),EXPERT("Experto",1.025);
        public final double rivalPace;private final String label;
        Difficulty(String label,double pace){this.label=label;rivalPace=pace;}
        public String toString(){return label;}
    }
    public enum Track {
        COAST("Costa · rectas",4000,46,0.95),HIGHLANDS("Sierra · técnico",3600,45,1.15),NIGHT("Nocturno · mixto",4400,50,1.05);
        public final double length,lapTime,abrasion;private final String label;
        Track(String label,double length,double lapTime,double abrasion){this.label=label;this.length=length;this.lapTime=lapTime;this.abrasion=abrasion;}
        public String toString(){return label;}
    }
    public enum DrivingStyle {
        CONSERVE("Conservar",0.968,0.70,0.86,0.45),BALANCED("Equilibrado",1,1,1,1),ATTACK("Atacar",1.025,1.42,1.14,1.9);
        public final double pace,wear,fuel,risk;private final String label;
        DrivingStyle(String label,double pace,double wear,double fuel,double risk){this.label=label;this.pace=pace;this.wear=wear;this.fuel=fuel;this.risk=risk;}
        public String toString(){return label;}
    }
    public enum EnergyMode {
        HARVEST("Recuperar"),BALANCED("Equilibrado"),DEPLOY("Desplegar");
        private final String label;EnergyMode(String label){this.label=label;}public String toString(){return label;}
    }
    public enum Personality {
        AGGRESSIVE("Agresivo"),CONSERVATIVE("Conservador"),STRATEGIST("Estratega"),BALANCED("Equilibrado");
        private final String label;Personality(String label){this.label=label;}public String toString(){return label;}
    }
    public enum WeatherPlan {
        VARIABLE("Variable"),DRY("Seco estable"),RAIN("Lluvia en aumento"),DRYING("Pista secándose");
        private final String label;WeatherPlan(String label){this.label=label;}public String toString(){return label;}
    }
    public enum Weather {
        SUNNY("Soleado"),CLOUDY("Nublado"),RAIN("Lluvia"),STORM("Lluvia intensa");
        private final String label;Weather(String label){this.label=label;}public String toString(){return label;}
    }
    public enum Flag {
        GREEN("VERDE"),VSC("VIRTUAL SAFETY CAR"),SC("SAFETY CAR"),CHECKERED("BANDERA A CUADROS");
        private final String label;Flag(String label){this.label=label;}public String toString(){return label;}
    }
    public enum State { RACING,PAUSED,FINISHED }
    public enum PitPhase {
        TRACK("En pista"),ENTRY("Entrada a boxes"),SERVICE("Mecánicos trabajando"),EXIT("Salida de boxes");
        private final String label;PitPhase(String label){this.label=label;}public String toString(){return label;}
    }
    public record Config(int laps,Difficulty difficulty,Track track,WeatherPlan weather,TyreCompound initialTyre,long seed,boolean incidents) {
        public Config {
            if(laps<3||laps>30||difficulty==null||track==null||weather==null||initialTyre==null)throw new IllegalArgumentException("Configuración de carrera inválida.");
        }
    }
}
