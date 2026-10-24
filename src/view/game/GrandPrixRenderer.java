package view.game;

import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.game.*;
import static model.game.GameRules.*;

/** Original vector art; consumes snapshots and never advances or changes the race. */
public final class GrandPrixRenderer {
    private final Canvas canvas;
    private final GraphicsContext g;
    private static final String[] COLOURS={"#ff667c","#6bc5ff","#f6cb60","#b79aff","#55d3b0","#ff455d","#ffab71","#e7ecf5"};
    public GrandPrixRenderer(Canvas canvas){this.canvas=canvas;g=canvas.getGraphicsContext2D();}

    public void draw(RaceSnapshot race,Color team) {
        double w=canvas.getWidth(),h=canvas.getHeight();if(w<1||h<1)return;
        double offset=race==null?0:race.player().distance();
        track(w,h,offset);
        if(race==null) {
            car(w*0.51,h*0.7,28,team,Color.WHITE);
            g.setFill(Color.web("#e5edf9"));g.setFont(Font.font("Segoe UI",19));g.fillText("Tu próximo Gran Premio",24,40);
            g.setFont(Font.font("Segoe UI",12));g.fillText("Configura la carrera y pulsa Iniciar",24,62);
            for(int i=0;i<5;i++){g.setFill(Color.web("#ff455d"));g.fillOval(w*0.32+i*27,h*0.32,17,17);}
            return;
        }
        var player=race.player();double size=Math.max(19,Math.min(32,w/19));
        for(var rival:race.cars().reversed()) {
            double y=h*0.7-(rival.distance()-player.distance())*1.15;
            if(y< -80||y>h+80||rival.retired())continue;
            double x=rival.pitPhase()!=PitPhase.TRACK?w*0.09:w*0.52+rival.lane()*w*0.23;
            Color colour=rival.player()?team:Color.web(COLOURS[rival.id()%COLOURS.length]);
            car(x,y,size,colour,rival.player()?Color.WHITE:Color.web("#ced9ed"));
            g.setFont(Font.font("Segoe UI",11));g.setFill(Color.web("#f0f4ff"));
            g.fillText(rival.player()?"TÚ · P"+rival.position():"P"+rival.position()+" "+rival.name(),Math.max(3,x-size),y-size*1.1);
            if(rival.drs()){g.setStroke(Color.web("#61e3af"));g.strokeLine(x-size/2,y+size*1.6,x+size/2,y+size*1.6);}
            if(rival.player()&&rival.pitPhase()==PitPhase.SERVICE)mechanics(x,y,size,race.elapsed());
        }
        if(race.weather()==Weather.RAIN||race.weather()==Weather.STORM)rain(w,h,race.elapsed(),race.weather()==Weather.STORM?65:35);
        miniMap(race,w-112,18);
        g.setFill(Color.web("#0d1626",0.92));g.fillRoundRect(12,h-57,w-24,45,8,8);
        g.setFill(race.flag()==Flag.GREEN?Color.web("#65d89d"):Color.web("#f6cb60"));g.setFont(Font.font("Segoe UI",12));
        g.fillText(race.flag().toString(),24,h-37);
        g.setFill(Color.web("#c3d1e9"));g.setFont(Font.font("Segoe UI",11));
        g.fillText("← → trayectoria   P boxes   E ERS   D DRS   Espacio pausa",24,h-20);
        if(race.state()==State.PAUSED)overlay(w,h,"CARRERA EN PAUSA","Pulsa Reanudar o Espacio");
        else if(race.state()==State.FINISHED)overlay(w,h,"BANDERA A CUADROS","Clasificación y estadísticas disponibles abajo");
    }

    private void track(double w,double h,double distance) {
        g.setFill(Color.web("#101d25"));g.fillRect(0,0,w,h);
        g.setFill(Color.web("#203326"));g.fillRect(w*0.14,0,w*0.76,h);
        g.setFill(Color.web("#2a303d"));g.fillRect(w*0.18,0,w*0.68,h);
        g.setFill(Color.web("#182231"));g.fillRect(w*0.035,0,w*0.11,h);
        g.setStroke(Color.web("#c8d3e2"));g.setLineWidth(2);g.strokeLine(w*0.15,0,w*0.15,h);
        double shift=distance%52;
        for(double y=-52;y<h;y+=26) {
            g.setFill(((int)(y/26)&1)==0?Color.web("#e94256"):Color.web("#d9e3ed"));
            g.fillRect(w*0.17,y+shift,5,24);g.fillRect(w*0.86,y+shift,5,24);
        }
        g.setStroke(Color.web("#667488"));g.setLineWidth(1.2);
        for(double y=-52;y<h;y+=52){g.strokeLine(w*0.405,y+shift,w*0.405,y+shift+20);g.strokeLine(w*0.635,y+shift,w*0.635,y+shift+20);}
        g.save();g.translate(w*0.07,Math.min(140,h*0.3));g.rotate(-90);g.setFill(Color.web("#8cabc9"));g.setFont(Font.font("Segoe UI",11));g.fillText("PIT LANE",0,0);g.restore();
    }
    private void mechanics(double x,double y,double size,double time) {
        g.setStroke(Color.web("#f6cb60"));g.setLineWidth(2);g.strokeRoundRect(x-size,y-size,size*2,size*3,8,8);
        for(int i=0;i<4;i++) {
            double mx=x+(i%2==0?-1:1)*(size+5+Math.sin(time*10)*3),my=y+(i/2)*size;
            g.setFill(Color.web("#ffb768"));g.fillOval(mx-5,my-5,10,10);g.setStroke(Color.web("#e4edf7"));g.strokeLine(mx,my,x+(i%2==0?-1:1)*size/2,my);
        }
    }
    private void miniMap(RaceSnapshot race,double x,double y) {
        g.setFill(Color.web("#0d1626",0.93));g.fillRoundRect(x-6,y-5,108,80,8,8);
        g.setStroke(Color.web("#52617b"));g.setLineWidth(7);g.strokeOval(x+7,y+13,77,42);
        for(var car:race.cars()) {
            double angle=car.distance()/race.config().track().length*Math.PI*2-Math.PI/2;
            g.setFill(car.player()?Color.WHITE:Color.web(COLOURS[car.id()%COLOURS.length]));
            double r=car.player()?4:2.8;g.fillOval(x+45+38*Math.cos(angle)-r,y+34+21*Math.sin(angle)-r,r*2,r*2);
        }
        g.setFont(Font.font("Segoe UI",10));g.setFill(Color.web("#bacbe4"));g.fillText("MAPA DE CARRERA",x+1,y+5);
    }
    private void rain(double w,double h,double time,int count) {
        g.setStroke(Color.web("#a4cbe8",0.35));g.setLineWidth(1);
        for(int i=0;i<count;i++){double x=(i*97.7+time*27)%w,y=(i*63.3+time*170)%h;g.strokeLine(x,y,x-5,y+13);}
    }
    private void overlay(double w,double h,String title,String subtitle) {
        g.setFill(Color.web("#080f1b",0.85));g.fillRoundRect(w*0.08,h*0.36,w*0.84,98,12,12);
        g.setFill(Color.WHITE);g.setFont(Font.font("Segoe UI",Math.max(15,Math.min(23,w/23))));g.fillText(title,w*0.12,h*0.36+38);
        g.setFont(Font.font("Segoe UI",11));g.fillText(subtitle,w*0.12,h*0.36+65);
    }
    private void car(double cx,double cy,double width,Color colour,Color accent) {
        double x=cx-width/2,y=cy-width*0.5,height=width*1.8;
        g.setFill(Color.web("#060a11"));g.fillRoundRect(x-4,y+4,width+8,height,8,8);
        g.setFill(colour);g.fillRoundRect(x,y,width,height,8,8);
        g.setFill(accent);g.fillRect(x+width*0.32,y+width*0.25,width*0.36,width*0.66);
        g.setFill(Color.web("#0a0c12"));g.fillOval(cx-width*0.15,y+width*0.8,width*0.3,width*0.38);
        g.setFill(colour);g.fillRect(x-5,y+height-4,width+10,5);g.fillRect(x-4,y+3,width+8,4);
        g.setFill(Color.web("#020409"));for(double dx:new double[]{-4,width-3}){g.fillRoundRect(x+dx,y+width*0.3,7,width*0.5,2,2);g.fillRoundRect(x+dx,y+height-width*0.6,7,width*0.5,2,2);}
    }

    public void drawArcade(ArcadeSession.Snapshot arcade,Color team,long frames) {
        g.save();g.scale(canvas.getWidth()/ArcadeSession.WIDTH,canvas.getHeight()/ArcadeSession.HEIGHT);
        g.setFill(Color.web("#0f171f"));g.fillRect(0,0,900,560);g.setFill(Color.web("#2a303d"));g.fillRect(110,0,680,560);
        g.setStroke(Color.web("#d9e3ed"));g.setLineWidth(4);g.strokeLine(110,0,110,560);g.strokeLine(790,0,790,560);
        g.setStroke(Color.web("#718198"));g.setLineWidth(2);for(int y=-40;y<560;y+=40){double shift=frames%40;g.strokeLine(450,y+shift,450,y+shift+18);}
        if(arcade!=null){car(arcade.playerX()+18,ArcadeSession.PLAYER_Y+18,36,team,Color.WHITE);for(var obstacle:arcade.obstacles())car(obstacle.x()+18,obstacle.y()+18,36,Color.web("#ff667c"),Color.WHITE);}
        g.setFont(Font.font("Segoe UI",18));g.setFill(Color.WHITE);g.fillText("ARCADE CLÁSICO · "+(arcade==null?"ESQUIVA LOS OBSTÁCULOS":"PUNTOS "+arcade.score()),25,35);
        if(arcade!=null&&!arcade.running())overlay(900,560,"¡COLISIÓN!","Pulsa Iniciar para volver a intentarlo");g.restore();
    }
}
