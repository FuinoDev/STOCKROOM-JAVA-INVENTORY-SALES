package ph.stockroom.model;
import java.math.BigDecimal;
import java.time.Instant;
public record Product(long id,String name,Category category,BigDecimal price,int quantity,int minimumStock,
                      boolean active,int version,Instant createdAt,Instant updatedAt) {
    public String status() { return quantity == 0 ? "Out of stock" : quantity <= minimumStock ? "Low stock" : "In stock"; }
    public BigDecimal inventoryValue() { return price.multiply(BigDecimal.valueOf(quantity)); }
    @Override public String toString() { return name; }
}
