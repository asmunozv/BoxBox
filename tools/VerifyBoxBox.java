import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Repeatable verification without new test-library/runtime dependencies. */
public final class VerifyBoxBox {
    public static void main(String[] args)throws Exception{
        Path root=Path.of("").toAbsolutePath().normalize();if(!root.getFileName().toString().equals("BoxBox"))throw new IllegalStateException("Ejecuta desde BoxBox.");
        Path output=root.resolve("private/verification/classes");Files.createDirectories(output);
        String mp=root.resolve("bin")+File.pathSeparator+root.resolve("lib/javafx-sdk-21.0.11/lib")+File.pathSeparator+root.resolve("gson-2.10.1.jar");
        String cp=root.resolve("lib/mysql-connector-j-9.1.0.jar").toString();
        List<String> names=List.of("SecurityChecks","AnalysisChecks","ArchiveFailureChecks","ExportDemo","HistoricalExplorerSmoke","ApplicationSmoke");
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("Se necesita JDK.");
        try(StandardJavaFileManager files=compiler.getStandardFileManager(null,null,StandardCharsets.UTF_8)){
            List<String> options=List.of("--release","25","-encoding","UTF-8","--module-path",mp,"--class-path",cp,"--add-modules","java.desktop,jdk.httpserver","--add-reads","boxbox=java.desktop","--add-reads","boxbox=jdk.httpserver","--patch-module","boxbox="+root.resolve("tools/verification"),"-d",output.toString());
            if(!compiler.getTask(null,files,null,options,null,files.getJavaFileObjectsFromPaths(names.stream().map(n->root.resolve("tools/verification/"+n+".java")).toList())).call())throw new IllegalStateException("No se pudieron compilar las verificaciones.");
        }
        List<String> checks=new ArrayList<>(names.subList(0,4));if(Arrays.asList(args).contains("--ui")){checks.add("HistoricalExplorerSmoke");checks.add("ApplicationSmoke");}
        for(String check:checks){List<String> command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin/java.exe").toString(),"--enable-native-access=javafx.graphics,javafx.media","--module-path",mp,"--class-path",cp,"--add-modules","java.desktop,jdk.httpserver","--add-reads","boxbox=java.desktop","--add-reads","boxbox=jdk.httpserver","--patch-module","boxbox="+output,"--module","boxbox/verification."+check));if(check.equals("HistoricalExplorerSmoke")&&Arrays.asList(args).contains("--gallery"))command.add("--gallery");Process p=new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();if(p.waitFor()!=0)throw new IllegalStateException("Falló "+check);}
        System.out.println("VERIFICATION_SUITE_PASSED: "+checks.size()+" verificadores reales; sin acceso de escritura al origen.");
    }
}
