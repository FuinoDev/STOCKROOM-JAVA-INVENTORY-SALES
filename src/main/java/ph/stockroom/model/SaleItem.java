package ph.stockroom.model;
import java.math.BigDecimal;
public record SaleItem(long productId,String productName,int quantity,BigDecimal price,BigDecimal subtotal) { }
