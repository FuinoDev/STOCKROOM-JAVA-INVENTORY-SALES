package ph.stockroom.dao;
import ph.stockroom.model.*;
import java.sql.*;
import java.util.*;
public final class UserDAO {
    public record Credentials(User user,String hash,int sessionVersion) { }
    private User map(ResultSet r) throws SQLException {
        return switch(r.getString("role")) {
            case "ADMIN" -> new Admin(r.getLong("id"),r.getString("username"),r.getString("display_name"),r.getBoolean("active"),r.getTimestamp("created_at").toInstant());
            case "STAFF" -> new Staff(r.getLong("id"),r.getString("username"),r.getString("display_name"),r.getBoolean("active"),r.getTimestamp("created_at").toInstant());
            default -> throw new SQLException("Unknown role");
        };
    }
    public Optional<Credentials> credentials(Connection c,String username) throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM users WHERE LOWER(username)=LOWER(?)")) {
            s.setString(1,username); try(var r=s.executeQuery()) { return r.next()?Optional.of(new Credentials(map(r),r.getString("password_hash"),r.getInt("session_version"))):Optional.empty(); }
        }
    }
    public Optional<User> findById(Connection c,long id) throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            s.setLong(1,id); try(var r=s.executeQuery()) { return r.next()?Optional.of(map(r)):Optional.empty(); }
        }
    }
    public List<User> findAll(Connection c) throws SQLException {
        List<User> users=new ArrayList<>();
        try(var s=c.prepareStatement("SELECT * FROM users ORDER BY id");var r=s.executeQuery()) { while(r.next()) users.add(map(r)); }
        return List.copyOf(users);
    }
    public boolean isEmpty(Connection c) throws SQLException {
        try(var s=c.prepareStatement("SELECT NOT EXISTS(SELECT 1 FROM users)");var r=s.executeQuery()) { r.next(); return r.getBoolean(1); }
    }
    public User insert(Connection c,String username,String name,String hash,String role) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO users(username,display_name,password_hash,role) VALUES (?,?,?,?) RETURNING id")) {
            s.setString(1,username);s.setString(2,name);s.setString(3,hash);s.setString(4,role);
            try(var r=s.executeQuery()) { r.next();return findById(c,r.getLong(1)).orElseThrow(); }
        }
    }
    public void setActive(Connection c,long id,boolean active) throws SQLException {
        try(var s=c.prepareStatement("UPDATE users SET active=?,session_version=session_version+1 WHERE id=?")) { s.setBoolean(1,active);s.setLong(2,id);s.executeUpdate(); }
    }
    public void setPassword(Connection c,long id,String hash) throws SQLException {
        try(var s=c.prepareStatement("UPDATE users SET password_hash=?,session_version=session_version+1 WHERE id=?")) { s.setString(1,hash);s.setLong(2,id);s.executeUpdate(); }
    }
}
