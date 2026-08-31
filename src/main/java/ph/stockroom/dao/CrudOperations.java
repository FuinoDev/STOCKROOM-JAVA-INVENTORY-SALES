package ph.stockroom.dao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
/** A small generic interface demonstrates reusable DAO contracts. */
public interface CrudOperations<T> {
    Optional<T> findById(Connection c,long id) throws SQLException;
    List<T> findAll(Connection c) throws SQLException;
    T insert(Connection c,T value) throws SQLException;
    void update(Connection c,T value) throws SQLException;
    void delete(Connection c,long id) throws SQLException;
}
