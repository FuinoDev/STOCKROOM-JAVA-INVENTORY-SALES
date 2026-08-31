package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.*;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.*;
public final class ProductPanel extends AppPanel {
    private final JTextField search=Ui.field("Search product name…",24);private final JComboBox<Category> category=new JComboBox<>();
    private final JTable table=Ui.table("ID","PRODUCT","CATEGORY","PRICE","STOCK","MINIMUM","STATUS");
    private final JLabel count=Ui.label("Loading products…",12,Ui.MUTED,false);
    private List<Product> products=List.of(),visible=List.of();private List<Category> categories=List.of();private boolean refreshing;
    private record Data(List<Product> products,List<Category> categories) { }
    public ProductPanel(DashboardFrame frame) {
        super(frame);boolean admin=user.can(Permission.MANAGE_PRODUCTS);
        add(heading("Products","A well-organized catalog is a good place to start.",Ui.row(Ui.button("Refresh",this::refresh),admin?Ui.primary("+  Add product",() -> edit(null)):new JLabel(""))),BorderLayout.NORTH);
        Ui.columnWidths(table,85,270,150,95,65,80,100);
        JPanel card=Ui.card();category.setPreferredSize(new Dimension(175,39));JPanel toolbar=new JPanel(new BorderLayout());toolbar.setOpaque(false);
        toolbar.add(Ui.row(search,category));toolbar.add(count,BorderLayout.EAST);card.add(toolbar,BorderLayout.NORTH);card.add(Ui.scroll(table));Ui.statusColumn(table,6);
        JPanel actions=Ui.row();
        if(admin){actions.add(Ui.button("Edit product",() -> edit(selected())));actions.add(Ui.button("Add stock",() -> StockDialog.show(this,selected(),false,this::refresh)));
            actions.add(Ui.button("Archive product",this::archive));actions.add(Ui.button("+ Category",this::addCategory));}
        actions.add(Ui.button("Export catalog",this::export));card.add(actions,BorderLayout.SOUTH);add(card);
        Ui.onChange(search,this::filter);category.addActionListener(e -> {if(!refreshing)filter();});
        if(admin)table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)Ui.guarded(table,() -> edit(selected()));}});
    }
    @Override public void refresh() {
        Ui.async(this,() -> new Data(app.products.search(session,"",0),app.products.categories(session)),d -> {
            products=d.products();categories=d.categories();Category selected=(Category)category.getSelectedItem();refreshing=true;
            category.removeAllItems();category.addItem(new Category(0,"All categories"));categories.forEach(category::addItem);
            if(selected!=null)for(int i=0;i<category.getItemCount();i++)if(category.getItemAt(i).id()==selected.id())category.setSelectedIndex(i);
            refreshing=false;filter();
        });
    }
    private void filter() {
        String q=search.getText().strip().toLowerCase(Locale.ROOT);Category c=(Category)category.getSelectedItem();
        visible=products.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q) && (c==null || c.id()==0 || p.category().id()==c.id())).toList();
        Ui.rows(table,visible.stream().map(p -> new Object[]{"PRD-%04d".formatted(p.id()),p.name(),p.category().name(),Formats.currency(p.price()),Formats.number(p.quantity()),Formats.number(p.minimumStock()),p.status()}).toList());
        count.setText(visible.size()+" products");
    }
    private Product selected() { int index=table.getSelectedRow();if(index<0 || index>=visible.size())throw new AppException("Select a product first.");return visible.get(index); }
    private void edit(Product existing) {
        boolean creating=existing==null;
        JTextField name=Ui.field("e.g. Coca-Cola Original 330 ml",25),price=Ui.field("0.00",12),stock=Ui.field("0",12),minimum=Ui.field("10",12);
        JComboBox<Category> cats=new JComboBox<>(categories.toArray(Category[]::new));
        stock.setText("0");minimum.setText("10");
        if(!creating){name.setText(existing.name());price.setText(existing.price().toPlainString());stock.setText(""+existing.quantity());minimum.setText(""+existing.minimumStock());
            for(int i=0;i<cats.getItemCount();i++)if(cats.getItemAt(i).id()==existing.category().id())cats.setSelectedIndex(i);stock.setEnabled(false);}
        JPanel body=Ui.stack(14,Ui.fieldRow("PRODUCT NAME",name),Ui.fieldRow("CATEGORY",cats),Ui.fieldRow("SELLING PRICE · PHP",price),
            Ui.fieldRow(creating?"OPENING QUANTITY":"CURRENT STOCK · USE INVENTORY TO CHANGE",stock),Ui.fieldRow("MINIMUM STOCK ALERT",minimum),
            Ui.label("Stock at or below the minimum triggers an alert.",11,Ui.MUTED,false));
        JDialog[] dialog=new JDialog[1];
        JButton save=Ui.primary(creating?"Add product":"Save changes",() -> {
            Product value=new Product(creating?0:existing.id(),name.getText(),(Category)cats.getSelectedItem(),Validation.money(price.getText(),"Price",true),
                Validation.integer(stock.getText(),"Quantity"),Validation.integer(minimum.getText(),"Minimum stock"),true,creating?0:existing.version(),null,null);
            Ui.async(dialog[0].getContentPane(),() -> {if(creating)app.products.create(session,value);else app.products.update(session,value);return true;},ok -> {dialog[0].dispose();refresh();});
        });
        dialog[0]=Ui.dialog(this,creating?"Add product":"Edit product",body,Ui.row(Ui.button("Cancel",() -> dialog[0].dispose()),save),480);dialog[0].getRootPane().setDefaultButton(save);dialog[0].setVisible(true);
    }
    private void archive() {
        Product p=selected();
        if(!Ui.confirm(this,"Archive product?","Archive "+p.name()+"?\nPast receipts and stock history will be preserved.\nThe stock must be zero before archiving."))return;
        Ui.async(this,() -> {app.products.archive(session,p.id());return true;},ok -> refresh());
    }
    private void addCategory() {
        String name=JOptionPane.showInputDialog(this,"Category name:","Add category",JOptionPane.PLAIN_MESSAGE);
        if(name==null)return;Ui.async(this,() -> app.products.addCategory(session,name),c -> refresh());
    }
    private void export() {
        List<List<?>> rows=new ArrayList<>();
        for(Product p:visible)rows.add(List.of(p.id(),p.name(),p.category().name(),p.price(),p.quantity(),p.minimumStock(),p.status(),p.createdAt()));
        Ui.export(this,"products.csv",List.of("ID","Product","Category","Price PHP","Quantity","Minimum stock","Status","Created UTC"),rows);
    }
}
