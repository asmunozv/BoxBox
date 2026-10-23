package model.history;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import model.db.DatabaseConnection;
import static model.history.RaceData.*;

/** Immutable provider archive: insert-only, with a foreign key and provenance per document. */
public final class HistoryRepository {
    public List<RaceInfo> races() throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM bb_races ORDER BY season DESC,round_number");ResultSet r=p.executeQuery()){
            List<RaceInfo> out=new ArrayList<>();while(r.next())out.add(new RaceInfo(r.getString("race_key"),r.getInt("season"),r.getInt("round_number"),r.getString("name"),r.getDate("race_date").toLocalDate(),r.getString("circuit_id"),r.getString("circuit_name"),r.getString("country"),r.getString("locality"),nullable(r,"latitude"),nullable(r,"longitude")));return out;
        }
    }
    private Double nullable(ResultSet r,String key)throws SQLException{double n=r.getDouble(key);return r.wasNull()?null:n;}
    public List<Document> documents(String key)throws SQLException{
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM bb_documents WHERE race_key=? ORDER BY kind")){
            p.setString(1,key);List<Document> out=new ArrayList<>();try(ResultSet r=p.executeQuery()){while(r.next()){String payload=r.getString("payload");if(!digest(payload).equals(r.getString("sha256")))throw new SQLException("La instantánea local no coincide con su huella. Se conserva para revisión.");out.add(new Document(key,r.getString("kind"),r.getString("provider"),r.getString("source_url"),payload,r.getString("sha256"),r.getTimestamp("fetched_at").toInstant()));}}return out;
        }
    }
    public int append(List<RaceInfo> races,List<Document> docs)throws SQLException{
        try(Connection c=DatabaseConnection.getConnection()){
            c.setAutoCommit(false);int inserted=0;
            try{
                for(RaceInfo race:races)if(!exists(c,"SELECT 1 FROM bb_races WHERE race_key=?",race.key())){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO bb_races (race_key,season,round_number,name,race_date,circuit_id,circuit_name,country,locality,latitude,longitude) VALUES (?,?,?,?,?,?,?,?,?,?,?)")){
                        p.setString(1,race.key());p.setInt(2,race.season());p.setInt(3,race.round());p.setString(4,race.name());p.setDate(5,java.sql.Date.valueOf(race.date()));p.setString(6,race.circuitId());p.setString(7,race.circuit());p.setString(8,race.country());p.setString(9,race.locality());p.setObject(10,race.latitude());p.setObject(11,race.longitude());p.executeUpdate();
                    }
                }
                for(Document d:docs)if(!exists(c,"SELECT 1 FROM bb_documents WHERE race_key=? AND kind=?",d.raceKey(),d.kind())){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO bb_documents (race_key,kind,provider,source_url,payload,sha256,fetched_at) VALUES (?,?,?,?,?,?,?)")){
                        p.setString(1,d.raceKey());p.setString(2,d.kind());p.setString(3,d.provider());p.setString(4,d.url());p.setString(5,d.payload());p.setString(6,d.sha256());p.setTimestamp(7,Timestamp.from(d.fetchedAt()));p.executeUpdate();inserted++;
                    }
                }
                c.commit();return inserted;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private boolean exists(Connection c,String sql,String...args)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setString(i+1,args[i]);try(ResultSet r=p.executeQuery()){return r.next();}}}
    public static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
