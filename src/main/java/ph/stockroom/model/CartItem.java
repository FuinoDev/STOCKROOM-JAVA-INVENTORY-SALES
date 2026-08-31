package ph.stockroom.model;
import java.math.BigDecimal;
public record CartItem(long productId,String name,BigDecimal price,int quantity) {
    public BigDecimal subtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
}
