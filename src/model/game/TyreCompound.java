package model.game;

/** Simplified game coefficients, deliberately separate from historical strategy analysis. */
public enum TyreCompound {
    SOFT("Blandos", "S", "#ff667c",1.022,0.175,99),
    MEDIUM("Medios", "M", "#f6cb60",1,0.115,94),
    HARD("Duros", "H", "#e3eaf4",0.981,0.075,90),
    INTERMEDIATE("Intermedios", "I", "#65d89d",0.975,0.135,76),
    WET("Lluvia extrema", "W", "#67bfff",0.96,0.12,68);

    public final String code,color;
    public final double pace,wearPerLap,optimumTemperature;
    private final String label;
    TyreCompound(String label,String code,String color,double pace,double wear,double temperature) {
        this.label=label;this.code=code;this.color=color;this.pace=pace;wearPerLap=wear;optimumTemperature=temperature;
    }
    public boolean dry(){return this==SOFT||this==MEDIUM||this==HARD;}
    public double grip(double water) {
        water=Math.max(0,Math.min(1,water));
        if(dry())return 1-0.55*Math.max(0,water-0.08);
        if(this==INTERMEDIATE)return 1-0.24*Math.abs(water-0.48);
        return 1-0.32*Math.max(0,0.82-water);
    }
    public double wearMultiplier(double water) {
        if(dry())return 1+Math.max(0,water-0.25)*0.3;
        return 1+Math.max(0,(this==WET?0.65:0.3)-water)*2.8;
    }
    public static TyreCompound suitable(double water,double lapsLeft) {
        return water>=0.7?WET:water>=0.25?INTERMEDIATE:lapsLeft<=2?SOFT:HARD;
    }
    public String toString(){return label+" ("+code+")";}
}
