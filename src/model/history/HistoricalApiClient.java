package model.history;

import com.google.gson.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static model.history.Timing.*;

/** Used only by explicit data preparation. Analysis views never call this client. */
public final class HistoricalApiClient {
    public static final String JOLPICA="https://api.jolpi.ca/ergast/f1/";
    public static final String OPENF1="https://api.openf1.org/v1/";
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final String jolpicaBase,openBase;
    public HistoricalApiClient(){this(JOLPICA,OPENF1);}
    public HistoricalApiClient(String jolpicaBase,String openBase){this.jolpicaBase=jolpicaBase;this.openBase=openBase;}
    private static long lastJolpica,lastOpen;
    private static synchronized void pace(boolean open)throws InterruptedException{
        long min=open?2100:300,last=open?lastOpen:lastJolpica;
        long wait=min-(System.currentTimeMillis()-last);if(wait>0)Thread.sleep(wait);
        if(open)lastOpen=System.currentTimeMillis();else lastJolpica=System.currentTimeMillis();
    }
    public record Fetch(String url,JsonArray pages,Instant time){}

    public Fetch jolpica(String route,String field)throws IOException,InterruptedException{
        JsonArray pages=new JsonArray();int offset=0,total=-1,seen=0;
        do {
            String url=jolpicaBase+route+(route.contains("?")?"&":"?")+"limit=100&offset="+offset;
            JsonObject page=get(url,false).getAsJsonObject();JsonObject meta=object(page,"MRData");
            Integer returnedTotal=integer(meta,"total"),limit=integer(meta,"limit"),returnedOffset=integer(meta,"offset");
            if(returnedTotal==null||limit==null||limit<1||returnedOffset==null||returnedOffset!=offset)throw new IOException("Respuesta paginada inválida de Jolpica.");
            if(total<0)total=returnedTotal;else if(total!=returnedTotal)throw new IOException("La fuente cambió durante la descarga. Reintenta la preparación.");
            int count=0;
            for(JsonElement r:array(object(meta,"RaceTable"),"Races")){
                if(field.equals("Races"))count++;
                else if(field.equals("Laps"))for(JsonElement lap:array(r.getAsJsonObject(),"Laps"))count+=array(lap.getAsJsonObject(),"Timings").size();
                else count+=array(r.getAsJsonObject(),field).size();
            }
            if(count==0&&offset<total)throw new IOException("Página vacía antes de completar la fuente.");
            seen+=count;pages.add(page);offset+=limit;
            if(pages.size()>1000)throw new IOException("Paginación fuera del límite de protección.");
        }while(offset<total);
        if(seen!=total)throw new IOException("Se recibieron "+seen+" registros de "+total+". No se guardó una importación incompleta.");
        return new Fetch(jolpicaBase+route,pages,Instant.now());
    }
    public JsonArray open(String route)throws IOException,InterruptedException{return get(openBase+route,true).getAsJsonArray();}
    private JsonElement get(String url,boolean open)throws IOException,InterruptedException{
        for(int attempt=0;attempt<3;attempt++){
            pace(open);
            HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(35))
                .header("User-Agent","BoxBox/1.0 educational-desktop").header("Accept","application/json").GET().build();
            try{
                HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()==429||response.statusCode()>=500){
                    if(attempt==2)throw new IOException("La fuente está temporalmente limitada (HTTP "+response.statusCode()+"). Los datos locales se conservan.");
                    long delay=1000L*(1L<<attempt);try{delay=Math.max(delay,Long.parseLong(response.headers().firstValue("Retry-After").orElse("0"))*1000);}catch(NumberFormatException ignored){}
                    Thread.sleep(Math.min(delay,60000));continue;
                }
                if(response.statusCode()!=200)throw new IOException("Fuente no disponible (HTTP "+response.statusCode()+").");
                return JsonParser.parseString(response.body());
            }catch(JsonParseException|IllegalStateException e){throw new IOException("La fuente no devolvió JSON válido.",e);}
        }
        throw new IOException("No se pudo completar la descarga.");
    }
}
