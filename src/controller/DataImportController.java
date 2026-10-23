package controller;

import model.api.CircuitApiImporter;
import model.api.ConstructorApiImporter;
import model.api.DriverApiImporter;
import model.api.RaceApiImporter;
import model.api.ResultApiImporter;
import model.dao.CircuitDAO;
import model.dao.ConstructorDAO;
import model.dao.DriverDAO;
import model.dao.RaceDAO;
import model.dao.ResultDAO;
import model.entity.Driver;

public class DataImportController {

    public String importAllFromApi() {
        return importAllFromApi(2026);
    }

    public String importAllFromApi(int season) {
        try {
            ConstructorDAO constructorDAO = new ConstructorDAO();
            CircuitDAO circuitDAO = new CircuitDAO();
            DriverDAO driverDAO = new DriverDAO();
            RaceDAO raceDAO = new RaceDAO();
            ResultDAO resultDAO = new ResultDAO();

            resultDAO.deleteBySeason(season);
            raceDAO.deleteBySeason(season);

            ConstructorApiImporter.ImportSummary constructors =
                new ConstructorApiImporter(constructorDAO).importAllConstructors(season);
            int normalizedConstructors = normalizeConstructorNames(constructorDAO);
            int mergedConstructors = mergeSauberIntoAudi(constructorDAO);
            CircuitApiImporter.ImportSummary circuits =
                new CircuitApiImporter(circuitDAO).importAllCircuits(season);
            DriverApiImporter.ImportSummary drivers =
                new DriverApiImporter(driverDAO).importAllDrivers(season);
            driverDAO.deleteByErgastDriverId("jak_crawford");
            RaceApiImporter.ImportSummary races =
                new RaceApiImporter(raceDAO, circuitDAO).importSeasonRaces(season);
            ResultApiImporter.ImportSummary results =
                new ResultApiImporter(resultDAO, raceDAO, driverDAO, constructorDAO).importSeasonResults(season);
            int currentGridDrivers = driverDAO.deleteDriversNotInSeasonResults(season);
            int restoredKeyDrivers = ensureKeyDriversPresent(driverDAO);

            return "Import completed for season " + season + "\n"
                + "Constructors: inserted " + constructors.getInsertedCount() + ", skipped " + constructors.getSkippedCount() + "\n"
                + "Constructors normalized: " + normalizedConstructors + "\n"
                + "Constructors merged: " + mergedConstructors + "\n"
                + "Circuits: inserted " + circuits.getInsertedCount() + ", skipped " + circuits.getSkippedCount() + "\n"
                + "Drivers: inserted " + drivers.getInsertedCount() + ", skipped " + drivers.getSkippedCount() + "\n"
                + "Races: inserted " + races.getInsertedCount() + ", skipped existing " + races.getSkippedExistingCount()
                + ", missing circuit " + races.getSkippedMissingCircuitCount() + "\n"
                + "Results: inserted " + results.getInsertedCount() + ", skipped existing " + results.getSkippedExistingCount()
                + ", missing references " + results.getSkippedMissingReferenceCount() + "\n"
                + "Removed non-grid drivers: " + currentGridDrivers
                + "\nRestored key drivers: " + restoredKeyDrivers
                + "\nSeason workspace refreshed without deleting manual catalogs.";
        } catch (RuntimeException exception) {
            throw new ControllerException("Failed to import API data.", exception);
        }
    }

    private int normalizeConstructorNames(ConstructorDAO constructorDAO) {
        int updated = 0;
        for (model.entity.Constructor item : constructorDAO.findAll()) {
            String normalized = normalizeName(item.getErgastConstructorId(), item.getName());
            if (!normalized.equals(item.getName())) {
                item.setName(normalized);
                constructorDAO.update(item);
                updated++;
            }
        }
        return updated;
    }

    private String normalizeName(String ergastId, String currentName) {
        if (ergastId == null) {
            return currentName;
        }
        switch (ergastId) {
            case "rb":
                return "Racing Bulls";
            case "red_bull":
            case "redbull":
                return "Red Bull Racing";
            default:
                return currentName;
        }
    }

    private int mergeSauberIntoAudi(ConstructorDAO constructorDAO) {
        model.entity.Constructor audi = constructorDAO.findByErgastConstructorId("audi")
            .orElseGet(() -> constructorDAO.findByName("Audi").orElse(null));
        model.entity.Constructor sauber = constructorDAO.findByErgastConstructorId("sauber")
            .orElseGet(() -> constructorDAO.findByName("Sauber").orElse(null));

        if (audi == null && sauber != null) {
            sauber.setErgastConstructorId("audi");
            sauber.setName("Audi");
            constructorDAO.update(sauber);
            return 1;
        }
        if (audi == null || sauber == null) {
            return 0;
        }
        if (audi.getIdConstructor().equals(sauber.getIdConstructor())) {
            return 0;
        }

        constructorDAO.mergeConstructorReferences(sauber.getIdConstructor(), audi.getIdConstructor());
        return 1;
    }

    private int ensureKeyDriversPresent(DriverDAO driverDAO) {
        int restored = 0;
        restored += ensureDriverPresent(driverDAO, "oscar_piastri", "Oscar", "Piastri", "Australian", 81, "PIA");
        restored += ensureDriverPresent(driverDAO, "hulkenberg", "Nico", "Hulkenberg", "German", 27, "HUL");
        return restored;
    }

    private int ensureDriverPresent(
        DriverDAO driverDAO,
        String ergastId,
        String givenName,
        String familyName,
        String nationality,
        Integer number,
        String code
    ) {
        if (driverDAO.findByErgastDriverId(ergastId).isPresent()) {
            return 0;
        }
        Driver driver = new Driver();
        driver.setErgastDriverId(ergastId);
        driver.setGivenName(givenName);
        driver.setFamilyName(familyName);
        driver.setNationality(nationality);
        driver.setPermanentNumber(number);
        driver.setCode(code);
        driverDAO.create(driver);
        return 1;
    }
}
