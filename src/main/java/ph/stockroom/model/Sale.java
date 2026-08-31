package ph.stockroom.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record Sale(long id,long userId,String staffName,UUID requestId,BigDecimal total,BigDecimal payment,
                   BigDecimal change,Instant createdAt,List<SaleItem> items) {
    public Sale { items = List.copyOf(items); }
    public String reference() { return "TRX-%06d".formatted(id); }
    public int itemCount() { return items.stream().mapToInt(SaleItem::quantity).sum(); }
}
