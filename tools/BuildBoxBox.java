import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Build/run the existing Eclipse project with its local dependencies; no database writes. */
public final class BuildBoxBox {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        if (!root.getFileName().toString().equals("BoxBox")
            || !Files.readString(root.resolve(".project")).contains("<name>BoxBox</name>")) {
            throw new IllegalStateException("Ejecuta esta herramienta desde la carpeta BoxBox.");
        }
        Path source = root.resolve("src"), bin = root.resolve("bin");
        Path fx = root.resolve("lib/javafx-sdk-21.0.11/lib");
        String modulePath = fx + File.pathSeparator + root.resolve("gson-2.10.1.jar");
        String classPath = root.resolve("lib/mysql-connector-j-9.1.0.jar").toString();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Se necesita un JDK completo (objetivo Java 25).");

        // Only generated output belonging to this project is cleaned. Backups are under private/.
        if (Files.isSymbolicLink(bin)) throw new IllegalStateException("bin no puede ser un enlace.");
        if (Files.exists(bin)) {
            try (var paths = Files.walk(bin)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        Files.createDirectory(bin);
        List<Path> sources;
        try (var paths = Files.walk(source)) {
            sources = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            List<String> options = List.of("--release", "25", "-encoding", "UTF-8", "--module-path", modulePath,
                "--class-path", classPath, "-d", bin.toString());
            if (!compiler.getTask(null, files, null, options, null,
                    files.getJavaFileObjectsFromPaths(sources)).call()) {
                throw new IllegalStateException("La compilación de BoxBox falló.");
            }
        }
        try (var paths = Files.walk(source)) {
            for (Path file : paths.filter(Files::isRegularFile).filter(p -> !p.toString().endsWith(".java")).toList()) {
                Path destination = bin.resolve(source.relativize(file));
                Files.createDirectories(destination.getParent());
                Files.copy(file, destination);
            }
        }
        System.out.println("BUILD_OK: " + sources.size() + " Java sources; module boxbox; target Java 25");
        if (Arrays.asList(args).contains("--run")) {
            String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
            Process process = new ProcessBuilder(java, "--enable-native-access=javafx.graphics,javafx.media",
                "--module-path", bin + File.pathSeparator + modulePath,
                "--class-path", classPath, "--module", "boxbox/application.Main")
                .directory(root.toFile()).inheritIO().start();
            System.exit(process.waitFor());
        }
    }
}
