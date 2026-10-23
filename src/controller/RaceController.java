package controller;

import java.util.List;
import java.util.Optional;

import model.dao.RaceDAO;
import model.entity.Race;

public class RaceController {

    private final RaceDAO raceDAO;

    public RaceController() {
        this(new RaceDAO());
    }

    public RaceController(RaceDAO raceDAO) {
        this.raceDAO = raceDAO;
    }

    public Optional<Race> getRaceById(int idRace) {
        try {
            return raceDAO.findById(idRace);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch race by id.", exception);
        }
    }

    public List<Race> getRacesBySeason(int season) {
        try {
            return raceDAO.findBySeason(season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch races by season.", exception);
        }
    }

}
