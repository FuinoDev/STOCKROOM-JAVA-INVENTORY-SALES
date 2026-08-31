package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.AppException;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
public final class InventoryPanel extends AppPanel {
    private final JTextField search=Ui.field("Search inventory…",24);
    private final JComboBox<String> status=new JComboBox<>(new String[]{"All stock","Low stock","Out of stock","In stock"});
    private final JTable stockTable=Ui.table("PRODUCT","CATEGORY","ON HAND","MINIMUM","VALUE","STATUS");
    private final JTable historyTable=Ui.table("WHEN","PRODUCT","MOVEMENT","CHANGE","BEFORE","AFTER","BY","REFERENCE");
    private List<Product> products=List.of(),visible=List.of();private List<InventoryTransaction> history=List.of();
    private final JLabel count=Ui.label("",12,Ui.MUTED,false);private final JTabbedPane tabs=new JTabbedPane();
    private record Data(List<Product> products,List<InventoryTransaction> history) { }
    public InventoryPanel(DashboardFrame frame) {
        super(frame);add(heading("Inventory","Every unit accounted for. Every change recorded.",Ui.row(Ui.button("Refresh",this::refresh))),BorderLayout.NORTH);
        Ui.columnWidths(stockTable,280,160,80,80,115,110);Ui.columnWidths(historyTable,180,220,100,60,60,60,110,180);
        JPanel stock=Ui.card();stock.add(Ui.row(search,status,count),BorderLayout.NORTH);stock.add(Ui.scroll(stockTable));Ui.statusColumn(stockTable,5);
        JPanel actions=Ui.row();
        if(user.can(Permission.MANAGE_STOCK)){actions.add(Ui.primary("+  Receive stock",() -> StockDialog.show(this,selected(),false,this::refresh)));actions.add(Ui.button("Adjust count",() -> StockDialog.show(this,selected(),true,this::refresh)));}
        actions.add(Ui.button("View product history",this::productHistory));stock.add(actions,BorderLayout.SOUTH);
        JPanel movements=Ui.card();movements.add(Ui.label("Latest 500 movements · includes archived products",12,Ui.MUTED,false),BorderLayout.NORTH);movements.add(Ui.scroll(historyTable));
        movements.add(Ui.row(Ui.button("Show all movements",this::refresh),Ui.button("Export movements",this::export)),BorderLayout.SOUTH);
        tabs.addTab("Stock on hand",stock);tabs.addTab("Movement history",movements);add(tabs);
        Ui.onChange(search,this::filter);status.addActionListener(e -> filter());
    }
    @Override public void refresh() { Ui.async(this,() -> new Data(app.products.search(session,"",0),app.inventory.history(session,0)),data -> {products=data.products();filter();renderHistory(data.history());}); }
    private void filter() {
        String query=search.getText().strip().toLowerCase(Locale.ROOT);String filter=(String)status.getSelectedItem();
        visible=products.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query) && ("All stock".equals(filter) || p.status().equals(filter))).toList();
        Ui.rows(stockTable,visible.stream().map(p -> new Object[]{p.name(),p.category().name(),Formats.number(p.quantity()),Formats.number(p.minimumStock()),Formats.currency(p.inventoryValue()),p.status()}).toList());
        count.setText(visible.size()+" products");
    }
    private Product selected() {int i=stockTable.getSelectedRow();if(i<0 || i>=visible.size())throw new AppException("Select a product first.");return visible.get(i);}
    private void renderHistory(List<InventoryTransaction> values) {
        history=values;Ui.rows(historyTable,values.stream().map(t -> new Object[]{Formats.date(t.createdAt(),app.config.zone()),t.productName(),t.type().replace('_',' '),
            (t.quantity()>0?"+":"")+t.quantity(),t.previousStock(),t.newStock(),t.staffName(),t.note()}).toList());
    }
    private void productHistory() {Product p=selected();Ui.async(this,() -> app.inventory.history(session,p.id()),values -> {renderHistory(values);tabs.setSelectedIndex(1);});}
    private void export() {
        List<List<?>> rows=new ArrayList<>();
        for(var t:history)rows.add(List.of(Formats.date(t.createdAt(),app.config.zone()),t.productName(),t.type(),t.quantity(),t.previousStock(),t.newStock(),t.staffName(),t.note()));
        Ui.export(this,"inventory-movements.csv",List.of("Date "+app.config.zone(),"Product","Type","Change","Before","After","Staff","Reference"),rows);
    }
}
