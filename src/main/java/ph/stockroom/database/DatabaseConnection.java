package ph.stockroom.database;

import ph.stockroom.config.AppConfig;
import ph.stockroom.service.AppException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

/** Opens short-lived JDBC connections; every transaction commits or rolls back as a unit. */
public final class DatabaseConnection {
    @FunctionalInterface public interface SqlWork<T> { T run(Connection connection) throws SQLException; }
    private final AppConfig config;
    public DatabaseConnection(AppConfig config) { this.config = config; }
    public Connection open() throws SQLException {
        Properties p = new Properties();
        p.setProperty("user",config.databaseUser()); p.setProperty("password",config.databasePassword());
        p.setProperty("connectTimeout","5"); p.setProperty("socketTimeout","30");
        p.setProperty("ApplicationName","Stockroom");
        p.setProperty("options","-c timezone=UTC -c lock_timeout=10000 -c statement_timeout=20000");
        return DriverManager.getConnection(config.databaseUrl(),p);
    }
    public <T> T read(SqlWork<T> work) { try (Connection c = open()) { return work.run(c); } catch (SQLException e) { throw translate(e); } }
    public <T> T transaction(SqlWork<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try { T result = work.run(c); c.commit(); return result; }
            catch (SQLException | RuntimeException e) { try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); } throw e; }
        } catch (SQLException e) { throw translate(e); }
    }
    public void initialize() {
        final String sql;
        try (var in = DatabaseConnection.class.getResourceAsStream("/database/schema.sql")) {
            if (in == null) throw new IOException("Missing database schema.");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new AppException("The application package is incomplete. Please rebuild it.",e); }
        transaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(716042000)");
                s.execute(sql);
            }
            return null;
        });
    }
    private AppException translate(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String message = switch(state) {
            case "23505" -> "That name or username already exists. Please choose another.";
            case "23503" -> "This record is used elsewhere or no longer exists. Refresh and try again.";
            case "23514", "22003" -> "A value is outside the allowed range. Check your entries.";
            case "40P01", "40001", "55P03" -> "Another transaction is updating these records. Please try again.";
            case "57014" -> "The database took too long to respond. Refresh before trying again.";
            default -> state.startsWith("08") || state.startsWith("28")
                ? "Cannot connect to PostgreSQL. Run Start Stockroom or check config/local.properties. If this happened during checkout, retry the unchanged cart to safely recover the receipt."
                : "The database could not complete this operation. Please refresh and try again.";
        };
        return new AppException(message,e);
    }
}
