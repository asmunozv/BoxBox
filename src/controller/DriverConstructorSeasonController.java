package controller;

import java.util.Optional;
import model.dao.DriverConstructorSeasonDAO;
import model.entity.DriverConstructorSeason;

public class DriverConstructorSeasonController {

    private final DriverConstructorSeasonDAO driverConstructorSeasonDAO;

    public DriverConstructorSeasonController() {
        this(new DriverConstructorSeasonDAO());
    }

    public DriverConstructorSeasonController(DriverConstructorSeasonDAO driverConstructorSeasonDAO) {
        this.driverConstructorSeasonDAO = driverConstructorSeasonDAO;
    }

    public Optional<DriverConstructorSeason> getByDriverAndSeason(int idDriver, int season) {
        try {
            return driverConstructorSeasonDAO.findByDriverAndSeason(idDriver, season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch driver-constructor mapping.", exception);
        }
    }

    public Optional<DriverConstructorSeason> getLatestByDriver(int idDriver) {
        try {
            return driverConstructorSeasonDAO.findLatestByDriver(idDriver);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch latest driver-constructor mapping.", exception);
        }
    }
}
