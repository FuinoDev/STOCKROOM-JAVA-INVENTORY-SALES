package ph.stockroom.dao;
import ph.stockroom.model.InventoryTransaction;
import java.sql.*;
import java.util.*;
public final class InventoryDAO {
    public void record(Connection c,long productId,String type,int previous,int next,long userId,Long saleId,String note) throws SQLException {
        if(previous==next) return;
        try(var s=c.prepareStatement("INSERT INTO inventory_transactions(product_id,type,quantity,previous_stock,new_stock,user_id,sale_id,note) VALUES (?,?,?,?,?,?,?,?)")) {
            s.setLong(1,productId);s.setString(2,type);s.setInt(3,next-previous);s.setInt(4,previous);s.setInt(5,next);s.setLong(6,userId);
            if(saleId==null) s.setNull(7,Types.BIGINT); else s.setLong(7,saleId);
            s.setString(8,note);s.executeUpdate();
        }
    }
    public List<InventoryTransaction> recent(Connection c,long productId) throws SQLException {
        List<InventoryTransaction> list=new ArrayList<>();
        try(var s=c.prepareStatement("""
            SELECT t.*,p.name AS product_name,u.display_name AS staff_name FROM inventory_transactions t
            JOIN products p ON p.id=t.product_id JOIN users u ON u.id=t.user_id
            WHERE (?=0 OR t.product_id=?) ORDER BY t.created_at DESC,t.id DESC LIMIT 500
            """)) {
            s.setLong(1,productId);s.setLong(2,productId);
            try(var r=s.executeQuery()) { while(r.next()) list.add(new InventoryTransaction(r.getLong("id"),r.getLong("product_id"),r.getString("product_name"),
                r.getString("type"),r.getInt("quantity"),r.getInt("previous_stock"),r.getInt("new_stock"),r.getString("staff_name"),r.getString("note"),r.getTimestamp("created_at").toInstant())); }
        }
        return List.copyOf(list);
    }
}
