package model.entity;

import java.math.BigDecimal;

public class DriverSeasonStats {

    private int races;
    private int wins;
    private int podiums;
    private BigDecimal points;

    public int getRaces() {
        return races;
    }

    public void setRaces(int races) {
        this.races = races;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getPodiums() {
        return podiums;
    }

    public void setPodiums(int podiums) {
        this.podiums = podiums;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }
}
