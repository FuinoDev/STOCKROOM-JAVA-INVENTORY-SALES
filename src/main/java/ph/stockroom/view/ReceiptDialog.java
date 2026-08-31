package ph.stockroom.view;
import ph.stockroom.model.Sale;
import ph.stockroom.service.AppServices;
import ph.stockroom.util.ReceiptFormatter;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class ReceiptDialog {
    private ReceiptDialog() { }
    public static void show(Component owner,AppServices app,Sale sale) {
        String receipt=ReceiptFormatter.format(sale,app.config);
        JTextArea text=new JTextArea(receipt,24,45);text.setEditable(false);text.setFont(new Font("Consolas",Font.PLAIN,14));text.setLineWrap(true);text.setWrapStyleWord(true);
        text.setMargin(new Insets(20,20,20,20));text.setCaretPosition(0);JDialog[] d=new JDialog[1];
        JButton save=Ui.button("Save receipt",() -> {
            Path path=Ui.chooseSave(owner,sale.reference()+".txt");if(path==null)return;
            Ui.async(d[0].getContentPane(),() -> {Files.writeString(path,receipt,StandardCharsets.UTF_8);return true;},ok -> Ui.info(d[0],"Receipt saved."));
        });
        JButton print=Ui.button("Print",() -> {
            try{text.print();}catch(java.awt.print.PrinterException e){Ui.error(owner,new ph.stockroom.service.AppException("Printing failed. Check the printer and try again.",e));}
        });
        d[0]=Ui.dialog(owner,sale.reference()+" · Receipt",Ui.scroll(text),Ui.row(save,print,Ui.primary("Done",() -> d[0].dispose())),580);d[0].setVisible(true);
    }
}
