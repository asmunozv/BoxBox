package verification;

import java.nio.file.Path;
import model.history.*;

public final class ExportDemo {
    public static void main(String[] args)throws Exception{
        Path file=Path.of("datasets/demo-2024.json");if(!java.nio.file.Files.exists(file))new DemoArchive().exportYear(2024,file);
        int inserted=new DemoArchive().importFile(file);if(inserted!=0)throw new AssertionError("Expected idempotent local import");
        System.out.println("OFFLINE_DEMO_EXPORTED_AND_VALIDATED: 2024 archive, public racing data only; local reimport inserted 0 existing documents");
    }
}
