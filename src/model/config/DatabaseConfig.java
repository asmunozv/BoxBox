package model.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {

    public static final String DATABASE_NAME = "boxbox_db";
    public static final String DATABASE_USER = "boxbox_app";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/boxbox_db"
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        + "&connectTimeout=5000&socketTimeout=30000";

    private DatabaseConfig() {
    }

    public static String getUrl() {
        String url = read("BOXBOX_DB_URL", "url", DEFAULT_URL);
        try {
            if (!url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException();
            }
            URI address = new URI(url.substring("jdbc:".length()));
            String query = address.getRawQuery();
            if (!("/" + DATABASE_NAME).equals(address.getPath())
                || address.getHost() == null || address.getUserInfo() != null
                || (query != null && query.matches("(?is).*(^|&)(user|username|password|createDatabaseIfNotExist)=.*"))) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            // Deliberately do not include the supplied URL: it might contain a secret.
            throw new IllegalStateException("BoxBox requiere una URL JDBC dirigida exclusivamente a boxbox_db, sin credenciales en la URL.");
        }
    }

    public static String getUsername() {
        String user = required("BOXBOX_DB_USER", "user");
        if (!DATABASE_USER.equals(user)) {
            throw new IllegalStateException("BoxBox debe utilizar su usuario exclusivo boxbox_app.");
        }
        return user;
    }

    public static String getPassword() {
        return required("BOXBOX_DB_PASSWORD", "password");
    }

    public static Path getConfigurationPath() {
        String explicit = System.getProperty("boxbox.db.config");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("BOXBOX_DB_CONFIG");
        }
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        try {
            Path codeLocation = Path.of(DatabaseConfig.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            return codeLocation.getParent().resolve("private/config/database.properties");
        } catch (URISyntaxException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo localizar la configuración privada de BoxBox.");
        }
    }

    private static String required(String environmentName, String propertyName) {
        String value = read(environmentName, propertyName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta " + environmentName
                + ". Configura las variables BOXBOX_DB_* o el archivo privado de BoxBox.");
        }
        return value;
    }

    private static String read(String environmentName, String propertyName, String defaultValue) {
        String value = System.getenv(environmentName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return PrivateSettings.PROPERTIES.getProperty(propertyName, defaultValue);
    }

    private static final class PrivateSettings {
        private static final Properties PROPERTIES = load();

        private static Properties load() {
            Properties properties = new Properties();
            Path file = getConfigurationPath();
            if (Files.exists(file)) {
                try (InputStream input = Files.newInputStream(file)) {
                    properties.load(input);
                } catch (IOException exception) {
                    throw new IllegalStateException("No se pudo leer la configuración privada de BoxBox.");
                }
            }
            return properties;
        }
    }
}
