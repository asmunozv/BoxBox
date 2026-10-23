package controller;

import java.util.List;
import java.util.Optional;

import model.dao.ResultDAO;
import model.entity.ConstructorStandingRow;
import model.entity.DriverStandingRow;
import model.entity.Result;

public class ResultController {

    private final ResultDAO resultDAO;

    public ResultController() {
        this(new ResultDAO());
    }

    public ResultController(ResultDAO resultDAO) {
        this.resultDAO = resultDAO;
    }

    public int createResult(Result result) {
        try {
            return resultDAO.create(result);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to create result.", exception);
        }
    }

    public Optional<Result> getResultById(int idResult) {
        try {
            return resultDAO.findById(idResult);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch result by id.", exception);
        }
    }

    public List<Result> getAllResults() {
        try {
            return resultDAO.findAll();
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch results.", exception);
        }
    }

    public List<Result> getResultsBySeason(int season) {
        try {
            return resultDAO.findBySeason(season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch results by season.", exception);
        }
    }

    public boolean updateResult(Result result) {
        try {
            return resultDAO.update(result);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to update result.", exception);
        }
    }

    public boolean deleteResult(int idResult) {
        try {
            return resultDAO.delete(idResult);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to delete result.", exception);
        }
    }

    public List<Result> getResultsByRace(int idRace) {
        try {
            return resultDAO.findByRace(idRace);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch results by race.", exception);
        }
    }

    public List<DriverStandingRow> getDriverStandingsBySeason(int season) {
        try {
            return resultDAO.findDriverStandingsBySeason(season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch driver standings.", exception);
        }
    }

    public List<ConstructorStandingRow> getConstructorStandingsBySeason(int season) {
        try {
            return resultDAO.findConstructorStandingsBySeason(season);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch constructor standings.", exception);
        }
    }
}
