package controller;

import java.time.LocalDate;
import model.history.HistoryImportService;

/** Compatibility entry point: preparation now appends immutable historical snapshots. */
public class DataImportController {
    public String importAllFromApi(){return importAllFromApi(LocalDate.now().getYear());}
    public String importAllFromApi(int season){
        try{return new HistoryImportService().season(season,message->{}).message();}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new ControllerException("Preparación cancelada; los datos existentes se conservan.",e);}
        catch(Exception e){throw new ControllerException("No se pudo preparar el historial; los datos existentes se conservan.",e);}
    }
}
