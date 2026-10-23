package model.history;

import java.time.*;
import java.util.*;

/** Immutable analysis models, independent from JavaFX and JDBC. Missing values remain null. */
public record RaceData(RaceInfo race, List<Entry> entries, List<Lap> laps, List<Stint> stints,
                       List<Pit> pits, List<Event> events, List<Source> sources) {
    public RaceData {
        entries=List.copyOf(entries); laps=List.copyOf(laps); stints=List.copyOf(stints);
        pits=List.copyOf(pits); events=List.copyOf(events); sources=List.copyOf(sources);
    }
    public record RaceInfo(String key,int season,int round,String name,LocalDate date,
                           String circuitId,String circuit,String country,String locality,Double latitude,Double longitude) {
        @Override public String toString(){return "R"+round+" · "+name;}
    }
    public record Entry(String driverId,String driver,String code,int number,String teamId,String team,
                        Integer grid,Integer position,double points,int completedLaps,String status,
                        Integer qualifying,Double q1,Double q2,Double q3,Double totalSeconds,String session) {
        @Override public String toString(){return driver;}
        public boolean race(){return "RACE".equals(session);}
    }
    public record Lap(String driverId,int number,Integer position,Double seconds,Instant start,boolean pitOut) {}
    public record Stint(String driverId,int number,int firstLap,int lastLap,String compound,Integer initialAge) {
        public int length(){return lastLap-firstLap+1;}
    }
    public record Pit(String driverId,int stop,int lap,Double laneSeconds,Double stationarySeconds,String durationMeaning) {}
    public record Event(Integer lap,String category,String message,Instant date) {}
    public record Source(String kind,String provider,String url,String sha256,Instant fetchedAt) {}
    public record Document(String raceKey,String kind,String provider,String url,String payload,String sha256,Instant fetchedAt) {}
    public List<Entry> raceEntries(){return entries.stream().filter(Entry::race).sorted(Comparator.comparing(e->e.position()==null?Integer.MAX_VALUE:e.position())).toList();}
    public Optional<Entry> driver(String id){return raceEntries().stream().filter(e->e.driverId().equals(id)).findFirst();}
    public int lapCount(){return raceEntries().stream().mapToInt(Entry::completedLaps).max().orElse(0);}
}
