package ph.stockroom.util;
import ph.stockroom.config.AppConfig;
import ph.stockroom.model.Sale;
import java.util.Locale;
public final class ReceiptFormatter {
    private ReceiptFormatter() { }
    public static String format(Sale sale,AppConfig config) {
        StringBuilder b=new StringBuilder(config.businessName()+"\nINVENTORY & SALES\n");
        b.append("Sales receipt · ").append(sale.reference()).append("\n").append(Formats.date(sale.createdAt(),config.zone())).append("\nServed by ").append(sale.staffName());
        b.append("\n\n------------------------------------------------\n");
        for(var item:sale.items()) b.append(item.productName()).append("\n  ").append(item.quantity()).append(" × ").append(Formats.currency(item.price())).append("  =  ").append(Formats.currency(item.subtotal())).append("\n");
        b.append("------------------------------------------------\nTOTAL       ").append(Formats.currency(sale.total())).append("\nCASH        ").append(Formats.currency(sale.payment())).append("\nCHANGE      ").append(Formats.currency(sale.change()));
        b.append("\n\n").append(String.format(Locale.US,"%,d item(s)",sale.itemCount())).append("\nThank you for shopping with us.\n\nInternal sales receipt. Not a tax invoice.\n");
        return b.toString();
    }
}
