package ph.stockroom.service;
import ph.stockroom.dao.*;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.model.*;
import java.util.List;
public final class InventoryService {
    private final DatabaseConnection db;private final AuthService auth;private final ProductDAO products=new ProductDAO();private final InventoryDAO movements=new InventoryDAO();
    public InventoryService(DatabaseConnection db,AuthService auth) { this.db=db;this.auth=auth; }
    public List<InventoryTransaction> history(AuthService.Session session,long productId) {
        return db.read(c -> { auth.require(c,session,Permission.VIEW_INVENTORY);return movements.recent(c,productId); });
    }
    public void addStock(AuthService.Session session,long id,int amount,String note) {
        Validation.stock(amount,"Stock to add");if(amount==0) throw new AppException("Stock to add must be greater than zero.");
        change(session,id,amount,-1,note);
    }
    public void adjust(AuthService.Session session,long id,int newStock,int expectedStock,String note) {
        Validation.stock(newStock,"New stock");Validation.stock(expectedStock,"Previous stock");change(session,id,newStock,expectedStock,note);
    }
    private void change(AuthService.Session session,long id,int value,int expectedStock,String note) {
        String reason=Validation.text(note,"Reason / reference",300);
        db.transaction(c -> {
            User actor=auth.require(c,session,Permission.MANAGE_STOCK);Product p=products.lock(c,id);
            boolean adding=expectedStock<0;
            if(!adding && p.quantity()!=expectedStock) throw new AppException("Stock changed while this form was open. Refresh before adjusting.");
            int next=Validation.stock(adding?p.quantity()+value:value,"New stock");
            if(next==p.quantity()) throw new AppException("The new stock is unchanged.");
            products.setStock(c,id,next);movements.record(c,id,adding?"STOCK_IN":"ADJUSTMENT",p.quantity(),next,actor.getId(),null,reason);return null;
        });
    }
}
