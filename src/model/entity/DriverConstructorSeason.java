package model.entity;

public class DriverConstructorSeason {

    private Integer idDriverConstructorSeason;
    private Integer idDriver;
    private Integer idConstructor;
    private Integer season;

    public Integer getIdDriverConstructorSeason() {
        return idDriverConstructorSeason;
    }

    public void setIdDriverConstructorSeason(Integer idDriverConstructorSeason) {
        this.idDriverConstructorSeason = idDriverConstructorSeason;
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

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }
}
