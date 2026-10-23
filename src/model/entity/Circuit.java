package model.entity;

import java.math.BigDecimal;

public class Circuit {

    private Integer idCircuit;
    private String ergastCircuitId;
    private String name;
    private String country;
    private String locality;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public Integer getIdCircuit() {
        return idCircuit;
    }

    public void setIdCircuit(Integer idCircuit) {
        this.idCircuit = idCircuit;
    }

    public String getErgastCircuitId() {
        return ergastCircuitId;
    }

    public void setErgastCircuitId(String ergastCircuitId) {
        this.ergastCircuitId = ergastCircuitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
