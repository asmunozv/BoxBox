package model.entity;

import java.time.LocalDate;

public class Race {

    private Integer idRace;
    private Integer season;
    private Integer roundNumber;
    private String name;
    private LocalDate raceDate;
    private Integer idCircuit;

    public Integer getIdRace() {
        return idRace;
    }

    public void setIdRace(Integer idRace) {
        this.idRace = idRace;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(LocalDate raceDate) {
        this.raceDate = raceDate;
    }

    public Integer getIdCircuit() {
        return idCircuit;
    }

    public void setIdCircuit(Integer idCircuit) {
        this.idCircuit = idCircuit;
    }
}
