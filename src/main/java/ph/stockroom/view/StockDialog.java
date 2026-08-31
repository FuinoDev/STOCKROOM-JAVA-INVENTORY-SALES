package ph.stockroom.view;
import ph.stockroom.model.Product;
import ph.stockroom.service.Validation;
import javax.swing.*;
public final class StockDialog {
    private StockDialog() { }
    public static void show(AppPanel panel,Product product,boolean adjusting,Runnable after) {
        JTextField amount=Ui.field(adjusting?"New counted quantity":"Quantity received",24),note=Ui.field(adjusting?"e.g. Stock count correction":"e.g. Delivery reference",24);
        JPanel body=Ui.stack(12,Ui.label(product.name(),19,Ui.INK,true),Ui.label("Current stock: "+product.quantity()+" units",14,Ui.MUTED,false),
            Ui.fieldRow(adjusting?"NEW STOCK COUNT":"QUANTITY TO ADD",amount),Ui.fieldRow("REASON / REFERENCE",note),
            Ui.label("This change is recorded in the inventory history.",11,Ui.MUTED,false));
        JDialog[] dialog=new JDialog[1];JButton save=Ui.primary(adjusting?"Save adjustment":"Add stock",() -> {
            int quantity=Validation.integer(amount.getText(),"Quantity");String reason=note.getText();
            Ui.async(dialog[0].getContentPane(),() -> {
                if(adjusting)panel.app.inventory.adjust(panel.session,product.id(),quantity,product.quantity(),reason);
                else panel.app.inventory.addStock(panel.session,product.id(),quantity,reason);return true;
            },ok -> {dialog[0].dispose();after.run();});
        });
        dialog[0]=Ui.dialog(panel,adjusting?"Adjust stock":"Receive stock",body,Ui.row(Ui.button("Cancel",() -> dialog[0].dispose()),save),460);
        dialog[0].getRootPane().setDefaultButton(save);dialog[0].setVisible(true);
    }
}
