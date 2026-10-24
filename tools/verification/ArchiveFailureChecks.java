package verification;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import model.history.*;
import static model.history.RaceData.*;

public final class ArchiveFailureChecks {
    public static void main(String[] args)throws Exception{
        HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        server.createContext("/",exchange->{
            boolean second=exchange.getRequestURI().getQuery().contains("offset=1");boolean broken=exchange.getRequestURI().getPath().contains("broken");
            String result=second&&broken?"":"{\"position\":\""+(second?2:1)+"\"}";
            String body="{\"MRData\":{\"total\":\"2\",\"limit\":\"1\",\"offset\":\""+(second?1:0)+"\",\"RaceTable\":{\"Races\":[{\"Results\":["+result+"]}]}}}";
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try{
            String base="http://localhost:"+server.getAddress().getPort()+"/";
            HistoricalApiClient api=new HistoricalApiClient(base,base);
            if(api.jolpica("complete","Results").pages().size()!=2)throw new AssertionError("Pagination truncated");
            boolean rejected=false;try{api.jolpica("broken","Results");}catch(java.io.IOException expected){rejected=true;}if(!rejected)throw new AssertionError("Incomplete page accepted");
        }finally{server.stop(0);}
        HistoryRepository repository=new HistoryRepository();String key="2099:29";
        if(repository.races().stream().anyMatch(r->r.key().equals(key)))throw new AssertionError("Synthetic key unexpectedly used; stop");
        RaceInfo fixture=new RaceInfo(key,2099,29,"SYNTHETIC_TRANSACTION_TEST",LocalDate.of(2099,1,1),"TEST","TEST","TEST","TEST",null,null);
        Document invalid=new Document("MISSING_PARENT_"+UUID.randomUUID(),"TEST","SYNTHETIC","https://example.invalid/unit-test","{}",HistoryRepository.digest("{}"),Instant.now());
        boolean rolledBack=false;try{repository.append(List.of(fixture),List.of(invalid));}catch(java.sql.SQLException expected){rolledBack=true;}
        if(!rolledBack||repository.races().stream().anyMatch(r->r.key().equals(key)))throw new AssertionError("Transaction did not roll back");
        System.out.println("ARCHIVE_FAILURE_CHECKS_PASSED: complete pagination, truncated source rejected, FK failure rolls back preceding inserts; all test observations synthetic and not published");
    }
}
