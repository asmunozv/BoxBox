package verification;

import controller.AuthController;
import controller.UserSession;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import model.config.DatabaseConfig;
import model.dao.*;
import model.db.DatabaseConnection;
import model.entity.*;
import model.entity.Driver;

/** Integration checks use only boxbox_app; every persistent fixture is owned and labelled. */
public final class DatabaseIsolationCheck {
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("reject-url")) {
                try { DatabaseConfig.getUrl(); throw new AssertionError("Unsafe URL accepted"); }
                catch (IllegalStateException expected) { System.out.println("PASS: unsafe URL rejected before JDBC"); }
                return;
            }
            if (args.length > 0 && args[0].equals("reject-user")) {
                try { DatabaseConfig.getUsername(); throw new AssertionError("Administrative user accepted"); }
                catch (IllegalStateException expected) { System.out.println("PASS: administrative account rejected before JDBC"); }
                return;
            }
            if (args.length > 0 && args[0].equals("config-only")) {
                try (Connection c=DatabaseConnection.getConnection()) {
                    require("boxbox_db".equals(c.getCatalog()));
                    require(DatabaseConfig.getConfigurationPath().toString().contains("BoxBox"));
                }
                System.out.println("PASS: private config resolves to BoxBox and legacy F1_DB_* variables are ignored");
                return;
            }
            verifyConnectionAndRollback();
            verifyDaosAndAccounts();
            System.out.println("DATABASE_INTEGRATION_PASSED");
        } catch (Throwable e) {
            System.err.println("DATABASE_TEST_FAILED: " + e.getClass().getSimpleName());
            for (StackTraceElement line : e.getStackTrace()) {
                if (line.getClassName().startsWith("verification.")) { System.err.println("At " + line.getMethodName() + ":" + line.getLineNumber()); break; }
            }
            System.exit(1);
        }
    }

    static void verifyConnectionAndRollback() throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            require("boxbox_db".equals(c.getCatalog()));
            try (PreparedStatement p=c.prepareStatement("SELECT CURRENT_USER()"); ResultSet r=p.executeQuery()) {
                require(r.next() && r.getString(1).equals("boxbox_app@localhost"));
            }
            boolean denied=false;
            try (PreparedStatement p=c.prepareStatement("SELECT 1 FROM f1_championship.drivers WHERE 1=0")) {
                p.executeQuery();
            } catch (SQLException e) { denied=Set.of(1044,1142).contains(e.getErrorCode()); }
            require(denied);
            long original;
            try (PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM drivers"); ResultSet r=p.executeQuery()) { r.next(); original=r.getLong(1); }
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p=c.prepareStatement("UPDATE drivers SET nationality=? ORDER BY id_driver LIMIT 1")) {
                    p.setString(1,"__BOXBOX_ISOLATION_TEST__"); require(p.executeUpdate()==1);
                }
                try (PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM drivers WHERE nationality=?")) {
                    p.setString(1,"__BOXBOX_ISOLATION_TEST__");
                    try(ResultSet r=p.executeQuery()){r.next(); require(r.getInt(1)==1);}
                }
            } finally { c.rollback(); }
            try (PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM drivers");ResultSet r=p.executeQuery()){r.next();require(r.getLong(1)==original);}
        }
        System.out.println("PASS: exclusive runtime connection, source denial, target update/read/rollback");
    }

    static void verifyDaosAndAccounts() throws Exception {
        DriverDAO drivers = new DriverDAO();
        ConstructorDAO constructors = new ConstructorDAO();
        CircuitDAO circuits = new CircuitDAO();
        RaceDAO races = new RaceDAO();
        ResultDAO results = new ResultDAO();
        DriverConstructorSeasonDAO mappings = new DriverConstructorSeasonDAO();
        require(!drivers.findAll().isEmpty() && !constructors.findAll().isEmpty() && !circuits.findAll().isEmpty());
        require(!races.findAll().isEmpty() && !results.findAll().isEmpty());
        new UserAccountDAO(); new UserBingoDAO();
        System.out.println("PASS: all DAO reads and schema validation without CREATE/ALTER privileges");

        String token=UUID.randomUUID().toString().replace("-","");
        String marker="BOXBOX_TEST_"+token;
        Integer driverId=null, constructorId=null, circuitId=null, raceId=null, mappingId=null, resultId=null;
        try {
            Driver d=new Driver(); d.setErgastDriverId("boxbox_test_"+token); d.setGivenName("BOXBOX_TEST");
            d.setFamilyName(token); d.setNationality("TEST_ONLY"); d.setCode("BX"+token.substring(0,6));
            driverId=drivers.create(d);
            Constructor team=new Constructor();team.setErgastConstructorId("boxbox_test_"+token);team.setName(marker);team.setCountry("TEST_ONLY");
            constructorId=constructors.create(team);
            Circuit circuit=new Circuit();circuit.setErgastCircuitId("boxbox_test_"+token);circuit.setName(marker);circuit.setCountry("TEST_ONLY");
            circuitId=circuits.create(circuit);
            Race race=new Race();race.setSeason(2099);race.setRoundNumber(1);race.setName(marker);race.setRaceDate(LocalDate.of(2099,1,1));race.setIdCircuit(circuitId);
            raceId=races.create(race);
            DriverConstructorSeason mapping=new DriverConstructorSeason();mapping.setIdDriver(driverId);mapping.setIdConstructor(constructorId);mapping.setSeason(2099);
            mappingId=mappings.create(mapping);
            Result result=new Result();result.setIdRace(raceId);result.setIdDriver(driverId);result.setIdConstructor(constructorId);result.setSessionType("RACE");result.setPosition(1);
            resultId=results.create(result);
            require(results.findById(resultId).orElseThrow().getPoints().compareTo(new BigDecimal("25"))==0);
            result.setPosition(2);require(results.update(result));
            require(results.findById(resultId).orElseThrow().getPosition()==2);
            require(results.findById(resultId).orElseThrow().getPoints().compareTo(new BigDecimal("18"))==0);
            require(mappings.findByDriverAndSeason(driverId,2099).isPresent());
            require(drivers.findSeasonStats(driverId,2099).getRaces()==1);
            require(constructors.findSeasonStats(constructorId,2099).getPoints().compareTo(new BigDecimal("18"))==0);
            require(!results.findDriverStandingsBySeason(2099).isEmpty());
            require(!results.findConstructorStandingsBySeason(2099).isEmpty());
            require(results.delete(resultId));require(results.findById(resultId).isEmpty()); resultId=null;
            System.out.println("PASS: real DAO create/read/update/delete, foreign-key mapping, statistics and standings (labelled fixtures)");
        } finally {
            if(resultId!=null)results.delete(resultId);
            if(raceId!=null)races.delete(raceId);
            if(mappingId!=null)mappings.delete(mappingId);
            if(driverId!=null)drivers.delete(driverId);
            if(constructorId!=null)constructors.delete(constructorId);
            if(circuitId!=null)circuits.delete(circuitId);
        }

        String username="BOXBOX_TEST_"+UUID.randomUUID().toString().replace("-","");
        byte[] random=new byte[24];new SecureRandom().nextBytes(random);
        String temporaryPassword=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Path accountFile=Path.of("private/verification/account-test.properties");
        Files.createDirectories(accountFile.getParent());
        Properties owned=new Properties();owned.setProperty("username",username);
        try(var out=Files.newOutputStream(accountFile,StandardOpenOption.CREATE_NEW)){owned.store(out,"Owned transient fixture; no password recorded");}
        AuthController auth=new AuthController();
        require(auth.register(username,temporaryPassword));
        require(UserSession.getCurrentUserId()!=null);
        int userId=UserSession.getCurrentUserId();
        UserSession.logout();require(auth.authenticate(username,temporaryPassword));
        require(UserSession.getCurrentUserId()==userId);
        require(!auth.authenticate(username,UUID.randomUUID().toString()));
        UserBingoDAO bingo=new UserBingoDAO();
        bingo.upsert(userId,0,"BOXBOX_TEST_PREDICTION",false);
        require(bingo.loadByUser(userId).size()==1 && !bingo.loadByUser(userId).get(0).isCompleted());
        bingo.upsert(userId,0,"BOXBOX_TEST_PREDICTION_UPDATED",true);
        require(bingo.loadByUser(userId).get(0).isCompleted());
        UserSession.logout();
        System.out.println("PASS: registration, persisted login/rejection and bingo insert/update/read on an owned temporary account");
        System.out.println("NOTE: owned temporary account must now be removed by the administrative verifier before final equality check");
    }

    static void require(boolean condition) { if(!condition)throw new AssertionError("Verification failed"); }
}
