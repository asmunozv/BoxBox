package model.entity;

import java.time.LocalDate;

public class Driver {

    private Integer idDriver;
    private String ergastDriverId;
    private Integer permanentNumber;
    private String code;
    private String givenName;
    private String familyName;
    private LocalDate dateOfBirth;
    private String nationality;
    private String audioFilePath;

    public Integer getIdDriver() {
        return idDriver;
    }

    public void setIdDriver(Integer idDriver) {
        this.idDriver = idDriver;
    }

    public String getErgastDriverId() {
        return ergastDriverId;
    }

    public void setErgastDriverId(String ergastDriverId) {
        this.ergastDriverId = ergastDriverId;
    }

    public Integer getPermanentNumber() {
        return permanentNumber;
    }

    public void setPermanentNumber(Integer permanentNumber) {
        this.permanentNumber = permanentNumber;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
}
