package ph.stockroom.service;
import ph.stockroom.dao.ProductDAO;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class ReportService {
    public record InventorySummary(int products,long units,int lowStock,int outOfStock,BigDecimal value) { }
    public record SalesSummary(long transactions,long items,BigDecimal revenue) { }
    public record DailySales(LocalDate date,long transactions,long items,BigDecimal revenue) { }
    public record TopProduct(String name,long units,BigDecimal revenue) { }
    public record Overview(InventorySummary inventory,SalesSummary today,List<Product> alerts,List<DailySales> week) { }
    public record Report(LocalDate from,LocalDate to,InventorySummary inventory,SalesSummary sales,List<DailySales> days,List<TopProduct> top,List<Product> products) { }
    private final DatabaseConnection db;private final AuthService auth;private final ZoneId zone;
    private final ProductDAO products=new ProductDAO();
    public ReportService(DatabaseConnection db,AuthService auth,ZoneId zone) { this.db=db;this.auth=auth;this.zone=zone; }
    private InventorySummary summarize(List<Product> list) {
        return new InventorySummary(list.size(),list.stream().mapToLong(Product::quantity).sum(),
            (int)list.stream().filter(p -> p.quantity()>0 && p.quantity()<=p.minimumStock()).count(),
            (int)list.stream().filter(p -> p.quantity()==0).count(),
            list.stream().map(Product::inventoryValue).reduce(new BigDecimal("0.00"),BigDecimal::add));
    }
    private void bounds(PreparedStatement s,LocalDate from,LocalDate to,long userId) throws SQLException {
        s.setTimestamp(1,Timestamp.from(from.atStartOfDay(zone).toInstant()));
        s.setTimestamp(2,Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));s.setLong(3,userId);s.setLong(4,userId);
    }
    private SalesSummary totals(Connection c,LocalDate from,LocalDate to,long userId) throws SQLException {
        try(var s=c.prepareStatement("""
            SELECT COUNT(*) AS transactions,COALESCE(SUM(s.total),0) AS revenue,COALESCE(SUM(i.units),0) AS units FROM sales s
            LEFT JOIN (SELECT sale_id,SUM(quantity) AS units FROM sale_items GROUP BY sale_id) i ON i.sale_id=s.id
            WHERE s.created_at>=? AND s.created_at<? AND (?=0 OR s.user_id=?)
            """)) {
            bounds(s,from,to,userId);try(var r=s.executeQuery()) { r.next();return new SalesSummary(r.getLong("transactions"),r.getLong("units"),r.getBigDecimal("revenue")); }
        }
    }
    private List<DailySales> days(Connection c,LocalDate from,LocalDate to,long userId) throws SQLException {
        List<DailySales> list=new ArrayList<>();
        try(var s=c.prepareStatement("""
            SELECT (s.created_at AT TIME ZONE ?)::date AS day,COUNT(*) AS transactions,COALESCE(SUM(s.total),0) AS revenue,COALESCE(SUM(i.units),0) AS units
            FROM sales s LEFT JOIN (SELECT sale_id,SUM(quantity) AS units FROM sale_items GROUP BY sale_id) i ON i.sale_id=s.id
            WHERE s.created_at>=? AND s.created_at<? AND (?=0 OR s.user_id=?) GROUP BY day ORDER BY day
            """)) {
            s.setString(1,zone.getId());s.setTimestamp(2,Timestamp.from(from.atStartOfDay(zone).toInstant()));s.setTimestamp(3,Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
            s.setLong(4,userId);s.setLong(5,userId);
            try(var r=s.executeQuery()) { while(r.next()) list.add(new DailySales(r.getObject("day",LocalDate.class),r.getLong("transactions"),r.getLong("units"),r.getBigDecimal("revenue"))); }
        }
        return List.copyOf(list);
    }
    public Overview overview(AuthService.Session session) {
        return db.transaction(c -> {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);
            User user=auth.require(c,session,Permission.VIEW_INVENTORY);
            List<Product> list=products.findAll(c);LocalDate today=LocalDate.now(zone);long scope=user.can(Permission.VIEW_ALL_SALES)?0:user.getId();
            return new Overview(summarize(list),totals(c,today,today,scope),list.stream().filter(p -> p.quantity()<=p.minimumStock())
                .sorted(Comparator.comparingInt(Product::quantity)).toList(),days(c,today.minusDays(6),today,scope));
        });
    }
    public Report generate(AuthService.Session session,LocalDate from,LocalDate to) {
        SalesService.validateDates(from,to);
        return db.transaction(c -> {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);
            auth.require(c,session,Permission.VIEW_REPORTS);
            List<Product> list=products.findAll(c);List<TopProduct> top=new ArrayList<>();
            try(var s=c.prepareStatement("""
                SELECT i.product_name,SUM(i.quantity) AS units,SUM(i.subtotal) AS revenue FROM sale_items i
                JOIN sales s ON s.id=i.sale_id WHERE s.created_at>=? AND s.created_at<?
                GROUP BY i.product_id,i.product_name ORDER BY units DESC,i.product_name LIMIT 10
                """)) {
                s.setTimestamp(1,Timestamp.from(from.atStartOfDay(zone).toInstant()));s.setTimestamp(2,Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
                try(var r=s.executeQuery()) { while(r.next()) top.add(new TopProduct(r.getString("product_name"),r.getLong("units"),r.getBigDecimal("revenue"))); }
            }
            return new Report(from,to,summarize(list),totals(c,from,to,0),days(c,from,to,0),List.copyOf(top),list);
        });
    }
}
