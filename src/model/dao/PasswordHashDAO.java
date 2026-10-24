package model.dao;

import java.sql.*;
import java.util.Optional;
import model.db.DatabaseConnection;

/** Additive migration: legacy password values are never overwritten. */
public final class PasswordHashDAO {
    public Optional<String> find(int id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT password_hash FROM bb_password_hashes WHERE id_user=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}}catch(SQLException e){throw new DataAccessException("No se pudo consultar la credencial protegida.",e);}}
    public void append(int id,String hash){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO bb_password_hashes (id_user,password_hash) VALUES (?,?)")){p.setInt(1,id);p.setString(2,hash);p.executeUpdate();}
        catch(SQLException e){if(e.getErrorCode()!=1062)throw new DataAccessException("No se pudo registrar la credencial protegida.",e);}
    }
}
