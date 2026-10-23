package model.entity;

import java.math.BigDecimal;

public class Result {

    private Integer idResult;
    private Integer idRace;
    private Integer idDriver;
    private Integer idConstructor;
    private String sessionType;
    private Integer finishingPosition;
    private BigDecimal points;

    public Integer getIdResult() {
        return idResult;
    }

    public void setIdResult(Integer idResult) {
        this.idResult = idResult;
    }

    public Integer getIdRace() {
        return idRace;
    }

    public void setIdRace(Integer idRace) {
        this.idRace = idRace;
    }

    public Integer getIdDriver() {
        return idDriver;
    }

    public void setIdDriver(Integer idDriver) {
        this.idDriver = idDriver;
    }

    public Integer getIdConstructor() {
        return idConstructor;
    }

    public void setIdConstructor(Integer idConstructor) {
        this.idConstructor = idConstructor;
    }

    public Integer getFinishingPosition() {
        return finishingPosition;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public void setFinishingPosition(Integer finishingPosition) {
        this.finishingPosition = finishingPosition;
    }

    public Integer getPosition() {
        return finishingPosition;
    }

    public void setPosition(Integer position) {
        this.finishingPosition = position;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }
}
