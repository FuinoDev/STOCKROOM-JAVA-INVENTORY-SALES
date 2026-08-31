package ph.stockroom.model;
import java.time.Instant;
public record InventoryTransaction(long id,long productId,String productName,String type,int quantity,
    int previousStock,int newStock,String staffName,String note,Instant createdAt) { }
