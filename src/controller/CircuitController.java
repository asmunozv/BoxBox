package controller;

import java.util.List;
import java.util.Optional;

import model.dao.CircuitDAO;
import model.entity.Circuit;

public class CircuitController {

    private final CircuitDAO circuitDAO;

    public CircuitController() {
        this(new CircuitDAO());
    }

    public CircuitController(CircuitDAO circuitDAO) {
        this.circuitDAO = circuitDAO;
    }

    public Optional<Circuit> getCircuitById(int idCircuit) {
        try {
            return circuitDAO.findById(idCircuit);
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch circuit by id.", exception);
        }
    }

    public List<Circuit> getAllCircuits() {
        try {
            return circuitDAO.findAll();
        } catch (RuntimeException exception) {
            throw new ControllerException("Unable to fetch circuits.", exception);
        }
    }

}
