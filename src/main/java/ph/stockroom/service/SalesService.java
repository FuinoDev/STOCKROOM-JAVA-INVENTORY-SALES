package ph.stockroom.service;
import ph.stockroom.dao.*;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class SalesService {
    private final DatabaseConnection db;private final AuthService auth;private final ZoneId zone;
    private final ProductDAO products=new ProductDAO();private final SaleDAO sales=new SaleDAO();private final InventoryDAO movements=new InventoryDAO();
    public SalesService(DatabaseConnection db,AuthService auth,ZoneId zone) { this.db=db;this.auth=auth;this.zone=zone; }
    public Sale checkout(AuthService.Session session,UUID requestId,List<CartItem> cart,BigDecimal tendered) {
        if(requestId==null) throw new AppException("A checkout reference is required.");
        if(cart==null || cart.isEmpty()) throw new AppException("Add at least one product to the cart.");
        List<CartItem> snapshot=List.copyOf(cart);
        if(snapshot.size()>200) throw new AppException("A sale can contain at most 200 different products.");
        BigDecimal payment=Validation.money(tendered,"Payment",true);
        Set<Long> ids=new HashSet<>();
        for(CartItem item:snapshot) {
            if(!ids.add(item.productId())) throw new AppException("Combine duplicate products in the cart.");
            if(item.quantity()<=0) throw new AppException("Sale quantities must be greater than zero.");
            Validation.stock(item.quantity(),"Sale quantity");Validation.money(item.price(),"Item price",true);
        }
        return db.transaction(c -> {
            User actor=auth.require(c,session,Permission.SELL);
            // A request key is retained by the UI after uncertain failures; retrying never charges twice.
            try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) { s.setString(1,requestId.toString());s.execute(); }
            var existing=sales.findByRequest(c,requestId);
            if(existing.isPresent()) {
                if(existing.get().userId()!=actor.getId()) throw new AppException("This checkout reference belongs to another user.");
                return existing.get();
            }
            List<SaleItem> items=new ArrayList<>();Map<Long,Product> locked=new LinkedHashMap<>();
            BigDecimal total=new BigDecimal("0.00");
            for(CartItem item:snapshot.stream().sorted(Comparator.comparingLong(CartItem::productId)).toList()) {
                Product p=products.lock(c,item.productId());locked.put(p.id(),p);
                if(p.quantity()<item.quantity()) throw new AppException("Insufficient stock for "+p.name()+". Only "+p.quantity()+" available.");
                if(p.price().compareTo(item.price())!=0) throw new AppException("The price of "+p.name()+" changed. Remove it and add it again to confirm the new price.");
                BigDecimal subtotal=p.price().multiply(BigDecimal.valueOf(item.quantity()));total=total.add(subtotal);
                items.add(new SaleItem(p.id(),p.name(),item.quantity(),p.price(),subtotal));
            }
            Validation.money(total,"Sale total",true);
            if(payment.compareTo(total)<0) throw new AppException("Payment is less than the sale total.");
            long id=sales.insert(c,actor.getId(),requestId,total,payment);
            for(SaleItem item:items) {
                Product p=locked.get(item.productId());int next=p.quantity()-item.quantity();
                sales.insertItem(c,id,item);products.setStock(c,p.id(),next);movements.record(c,p.id(),"SALE",p.quantity(),next,actor.getId(),id,"Sale TRX-%06d".formatted(id));
            }
            return sales.findById(c,id).orElseThrow();
        });
    }
    public List<Sale> history(AuthService.Session session,LocalDate from,LocalDate to) {
        validateDates(from,to);
        return db.read(c -> {
            User user=auth.require(c,session,Permission.VIEW_OWN_SALES);
            return sales.list(c,user.can(Permission.VIEW_ALL_SALES)?null:user.getId(),from.atStartOfDay(zone).toInstant(),to.plusDays(1).atStartOfDay(zone).toInstant());
        });
    }
    public Sale find(AuthService.Session session,long id) {
        return db.read(c -> {
            User user=auth.require(c,session,Permission.VIEW_OWN_SALES);
            Sale sale=sales.findById(c,id).orElseThrow(() -> new AppException("Transaction not found."));
            if(!user.can(Permission.VIEW_ALL_SALES) && sale.userId()!=user.getId()) throw new AppException("You can only view your own transactions.");
            return sale;
        });
    }
    public static void validateDates(LocalDate from,LocalDate to) {
        if(from==null || to==null || from.isAfter(to)) throw new AppException("The start date must be on or before the end date.");
        if(from.isBefore(LocalDate.of(2000,1,1)) || to.isAfter(LocalDate.of(2100,12,31))) throw new AppException("Use dates between 2000 and 2100.");
    }
}
