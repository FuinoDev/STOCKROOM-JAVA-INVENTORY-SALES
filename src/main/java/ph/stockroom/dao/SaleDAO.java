package ph.stockroom.dao;
import ph.stockroom.model.*;
import java.sql.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
public final class SaleDAO {
    private static final String SELECT="SELECT s.*,u.display_name AS staff_name FROM sales s JOIN users u ON u.id=s.user_id ";
    public long insert(Connection c,long userId,UUID request,BigDecimal total,BigDecimal payment) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO sales(user_id,request_id,total,payment,change_amount) VALUES (?,?,?,?,?) RETURNING id")) {
            s.setLong(1,userId);s.setObject(2,request);s.setBigDecimal(3,total);s.setBigDecimal(4,payment);s.setBigDecimal(5,payment.subtract(total));
            try(var r=s.executeQuery()) { r.next();return r.getLong(1); }
        }
    }
    public void insertItem(Connection c,long saleId,SaleItem i) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO sale_items(sale_id,product_id,product_name,quantity,price,subtotal) VALUES (?,?,?,?,?,?)")) {
            s.setLong(1,saleId);s.setLong(2,i.productId());s.setString(3,i.productName());s.setInt(4,i.quantity());s.setBigDecimal(5,i.price());s.setBigDecimal(6,i.subtotal());s.executeUpdate();
        }
    }
    private Sale map(Connection c,ResultSet r) throws SQLException {
        long id=r.getLong("id");
        return new Sale(id,r.getLong("user_id"),r.getString("staff_name"),r.getObject("request_id",UUID.class),r.getBigDecimal("total"),
            r.getBigDecimal("payment"),r.getBigDecimal("change_amount"),r.getTimestamp("created_at").toInstant(),items(c,id));
    }
    public Optional<Sale> findById(Connection c,long id) throws SQLException {
        try(var s=c.prepareStatement(SELECT+"WHERE s.id=?")) { s.setLong(1,id); try(var r=s.executeQuery()) { return r.next()?Optional.of(map(c,r)):Optional.empty(); } }
    }
    public Optional<Sale> findByRequest(Connection c,UUID request) throws SQLException {
        try(var s=c.prepareStatement(SELECT+"WHERE s.request_id=?")) { s.setObject(1,request); try(var r=s.executeQuery()) { return r.next()?Optional.of(map(c,r)):Optional.empty(); } }
    }
    private List<SaleItem> items(Connection c,long id) throws SQLException {
        List<SaleItem> result=new ArrayList<>();
        try(var s=c.prepareStatement("SELECT * FROM sale_items WHERE sale_id=? ORDER BY id")) {
            s.setLong(1,id);try(var r=s.executeQuery()) { while(r.next()) result.add(new SaleItem(r.getLong("product_id"),r.getString("product_name"),r.getInt("quantity"),r.getBigDecimal("price"),r.getBigDecimal("subtotal"))); }
        }
        return result;
    }
    public List<Sale> list(Connection c,Long userId,Instant from,Instant to) throws SQLException {
        List<Sale> result=new ArrayList<>();
        try(var s=c.prepareStatement(SELECT+"WHERE s.created_at>=? AND s.created_at<? AND (?=0 OR s.user_id=?) ORDER BY s.created_at DESC,s.id DESC LIMIT 500")) {
            s.setTimestamp(1,Timestamp.from(from));s.setTimestamp(2,Timestamp.from(to));s.setLong(3,userId==null?0:userId);s.setLong(4,userId==null?0:userId);
            try(var r=s.executeQuery()) { while(r.next()) result.add(map(c,r)); }
        }
        return List.copyOf(result);
    }
}
