package ph.stockroom.dao;
import ph.stockroom.model.*;
import ph.stockroom.service.AppException;
import java.sql.*;
import java.util.*;
public final class ProductDAO implements CrudOperations<Product> {
    private static final String SELECT="SELECT p.*,c.name AS category_name FROM products p JOIN categories c ON c.id=p.category_id ";
    private Product map(ResultSet r) throws SQLException {
        return new Product(r.getLong("id"),r.getString("name"),new Category(r.getLong("category_id"),r.getString("category_name")),
            r.getBigDecimal("price"),r.getInt("quantity"),r.getInt("minimum_stock"),r.getBoolean("active"),r.getInt("version"),
            r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());
    }
    @Override public Optional<Product> findById(Connection c,long id) throws SQLException {
        try(var s=c.prepareStatement(SELECT+"WHERE p.id=?")) {
            s.setLong(1,id); try(var r=s.executeQuery()) { return r.next()?Optional.of(map(r)):Optional.empty(); }
        }
    }
    public Product lock(Connection c,long id) throws SQLException {
        try(var s=c.prepareStatement(SELECT+"WHERE p.id=? FOR UPDATE OF p")) {
            s.setLong(1,id); try(var r=s.executeQuery()) {
                if(!r.next()) throw new AppException("This product no longer exists.");
                Product p=map(r); if(!p.active()) throw new AppException(p.name()+" has been archived. Remove it from the cart.");
                return p;
            }
        }
    }
    @Override public List<Product> findAll(Connection c) throws SQLException { return search(c,"",0); }
    public List<Product> search(Connection c,String query,long category) throws SQLException {
        List<Product> result=new ArrayList<>();
        try(var s=c.prepareStatement(SELECT+"WHERE p.active=TRUE AND POSITION(LOWER(?) IN LOWER(p.name)) > 0 AND (?=0 OR p.category_id=?) ORDER BY LOWER(p.name)")) {
            s.setString(1,query); s.setLong(2,category); s.setLong(3,category);
            try(var r=s.executeQuery()) { while(r.next()) result.add(map(r)); }
        }
        return List.copyOf(result);
    }
    @Override public Product insert(Connection c,Product p) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO products(name,category_id,price,quantity,minimum_stock) VALUES (?,?,?,?,?) RETURNING id")) {
            s.setString(1,p.name()); s.setLong(2,p.category().id()); s.setBigDecimal(3,p.price()); s.setInt(4,p.quantity()); s.setInt(5,p.minimumStock());
            try(var r=s.executeQuery()) { r.next(); return findById(c,r.getLong(1)).orElseThrow(); }
        }
    }
    @Override public void update(Connection c,Product p) throws SQLException {
        try(var s=c.prepareStatement("UPDATE products SET name=?,category_id=?,price=?,minimum_stock=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=? AND active=TRUE")) {
            s.setString(1,p.name()); s.setLong(2,p.category().id()); s.setBigDecimal(3,p.price()); s.setInt(4,p.minimumStock()); s.setLong(5,p.id()); s.setInt(6,p.version());
            if(s.executeUpdate()!=1) throw new AppException("This product changed while you were editing. Refresh and open it again.");
        }
    }
    @Override public void delete(Connection c,long id) throws SQLException {
        Product p=lock(c,id);
        if(p.quantity()!=0) throw new AppException("Adjust this product's stock to zero before archiving it. Its history will be kept.");
        try(var s=c.prepareStatement("UPDATE products SET active=FALSE,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
    }
    public void setStock(Connection c,long id,int quantity) throws SQLException {
        try(var s=c.prepareStatement("UPDATE products SET quantity=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            s.setInt(1,quantity); s.setLong(2,id); s.executeUpdate();
        }
    }
}
