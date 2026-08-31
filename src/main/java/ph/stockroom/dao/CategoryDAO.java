package ph.stockroom.dao;
import ph.stockroom.model.Category;
import java.sql.*;
import java.util.*;
public final class CategoryDAO {
    public List<Category> findAll(Connection c) throws SQLException {
        List<Category> result=new ArrayList<>();
        try (var s=c.prepareStatement("SELECT id,name FROM categories ORDER BY name"); var r=s.executeQuery()) {
            while(r.next()) result.add(new Category(r.getLong(1),r.getString(2)));
        }
        return List.copyOf(result);
    }
    public Category insert(Connection c,String name) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO categories(name) VALUES (?) RETURNING id")) {
            s.setString(1,name); try(var r=s.executeQuery()) { r.next(); return new Category(r.getLong(1),name); }
        }
    }
}
