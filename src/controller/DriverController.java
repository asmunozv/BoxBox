package controller;

import java.util.List;
import java.util.Optional;

import model.dao.DriverDAO;
import model.entity.Driver;
import model.entity.DriverSeasonStats;

public class DriverController {

    private final DriverDAO driverDAO;

    public DriverController() {
        this(new DriverDAO());
    }

    public DriverController(DriverDAO driverDAO) {
        this.driverDAO = driverDAO;
    }

    public Optional<Driver> getDriverById(int idDriver) {
        try {
            return driverDAO.findById(idDriver);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch driver by id.", exception);
        }
    }

    public List<Driver> getAllDrivers() {
        try {
            return driverDAO.findAll();
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch drivers.", exception);
        }
    }

    public DriverSeasonStats getDriverSeasonStats(int idDriver, int season) {
        try {
            return driverDAO.findSeasonStats(idDriver, season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch driver season stats.", exception);
        }
    }

    public Optional<Integer> getDriverDebutSeason(int idDriver) {
        try {
            return driverDAO.findDebutSeason(idDriver);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch driver debut season.", exception);
        }
    }
}
