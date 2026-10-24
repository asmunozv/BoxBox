import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Game checks without SQL/HTTP; optional historical regression uses an explicit existing local fixture. */
public final class VerifyGame {
    public static void main(String[] args)throws Exception {
        Path root=Path.of("").toAbsolutePath().normalize();
        if(!Files.isRegularFile(root.resolve("src/controller/MiniGameViewController.java")))throw new IllegalStateException("Ejecuta desde BoxBox.");
        Path output=root.resolve("private/verification/game-classes");Files.createDirectories(output);
        String mp=root.resolve("bin")+File.pathSeparator+root.resolve("lib/javafx-sdk-21.0.11/lib")+File.pathSeparator+root.resolve("gson-2.10.1.jar");
        String cp=root.resolve("lib/mysql-connector-j-9.1.0.jar").toString();
        boolean ui=Arrays.asList(args).contains("--ui"),history=Arrays.asList(args).contains("--history-regression");
        List<String> names=new ArrayList<>(List.of("GameChecks"));if(ui)names.add("GameSmoke");if(history)names.add("GameHistoryRegression");
        List<Path> sources=new ArrayList<>(names.stream().map(n->root.resolve("tools/verification/game/"+n+".java")).toList());
        if(history)sources.add(root.resolve("tools/verification/HistoricalExplorerSmoke.java"));
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("Se necesita JDK.");
        try(StandardJavaFileManager files=compiler.getStandardFileManager(null,null,StandardCharsets.UTF_8)) {
            List<String> options=List.of("--release","25","-encoding","UTF-8","--module-path",mp,"--class-path",cp,"--add-modules","java.desktop","--add-reads","boxbox=java.desktop","--patch-module","boxbox="+root.resolve("tools/verification/game"),"-d",output.toString());
            if(!compiler.getTask(null,files,null,options,null,files.getJavaFileObjectsFromPaths(sources)).call())throw new IllegalStateException("Falló la compilación de pruebas del juego.");
        }
        for(String check:names) {
            String main=check.equals("GameHistoryRegression")?"verification.GameHistoryRegression":"verification.game."+check;
            List<String> command=List.of(Path.of(System.getProperty("java.home"),"bin/java.exe").toString(),"--enable-native-access=javafx.graphics,javafx.media","--module-path",mp,"--class-path",cp,"--add-modules","java.desktop","--add-reads","boxbox=java.desktop","--patch-module","boxbox="+output,"--module","boxbox/"+main);
            Process process=new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();if(process.waitFor()!=0)throw new IllegalStateException("Falló "+check);
        }
        System.out.println("GAME_VERIFICATION_PASSED: "+names.size()+" verificadores; "+(history?"incluye regresión histórica con MySQL local y selección explícita de 2024":"juego sin SQL ni HTTP"));
    }
}
