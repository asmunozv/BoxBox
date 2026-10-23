package controller;

import java.util.List;
import java.util.Optional;

import model.dao.ConstructorDAO;
import model.entity.Constructor;
import model.entity.ConstructorSeasonStats;

public class ConstructorController {

    private final ConstructorDAO constructorDAO;

    public ConstructorController() {
        this(new ConstructorDAO());
    }

    public ConstructorController(ConstructorDAO constructorDAO) {
        this.constructorDAO = constructorDAO;
    }

    public Optional<Constructor> getConstructorById(int idConstructor) {
        try {
            return constructorDAO.findById(idConstructor);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch constructor by id.", exception);
        }
    }

    public List<Constructor> getAllConstructors() {
        try {
            return constructorDAO.findAll();
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch constructors.", exception);
        }
    }

    public ConstructorSeasonStats getConstructorSeasonStats(int idConstructor, int season) {
        try {
            return constructorDAO.findSeasonStats(idConstructor, season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch constructor season stats.", exception);
        }
    }
}
