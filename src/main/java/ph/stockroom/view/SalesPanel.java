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
public final class SalesPanel extends AppPanel {
    private final JTextField search=Ui.field("Find a product…",21),payment=Ui.field("Cash received",12);
    private final JComboBox<Category> category=new JComboBox<>();
    private final JSpinner quantity=new JSpinner(new SpinnerNumberModel(1,1,1_000_000,1));
    private final JTable catalog=Ui.table("PRODUCT","PRICE","AVAILABLE"),cartTable=Ui.table("PRODUCT","QTY","PRICE","TOTAL");
    private final JLabel total=Ui.label("₱0.00",30,Ui.INK,true),change=Ui.label("₱0.00",18,Ui.GREEN,true),cartCount=Ui.label("0 items",12,Ui.MUTED,false);
    private List<Product> products=List.of(),visible=List.of();private final LinkedHashMap<Long,CartItem> cart=new LinkedHashMap<>();
    private UUID requestId=UUID.randomUUID();private boolean working=false;private boolean filtering=false;
    private record Data(List<Product> products,List<Category> categories) { }
    public SalesPanel(DashboardFrame frame) {
        super(frame);add(heading("New sale","A quick checkout. A happy customer.",Ui.row(Ui.button("Refresh stock",this::refresh))),BorderLayout.NORTH);
        JPanel productCard=Ui.card();category.setPreferredSize(new Dimension(158,38));
        JPanel filters=new JPanel(new BorderLayout(10,0));filters.setOpaque(false);filters.add(search);filters.add(category,BorderLayout.EAST);
        productCard.add(Ui.stack(14,Ui.label("Choose your products",17,Ui.INK,true),filters),BorderLayout.NORTH);
        Ui.columnWidths(catalog,270,90,105);Ui.columnWidths(cartTable,220,45,80,90);
        productCard.add(Ui.scroll(catalog));quantity.setPreferredSize(new Dimension(80,38));
        productCard.add(Ui.row(Ui.label("Quantity",12,Ui.MUTED,true),quantity,Ui.primary("+  Add to cart",this::addToCart)),BorderLayout.SOUTH);
        JPanel cartCard=Ui.card();cartCard.add(Ui.row(Ui.label("Current sale",18,Ui.INK,true),cartCount),BorderLayout.NORTH);
        JPanel cartCenter=new JPanel(new BorderLayout(0,10));cartCenter.setOpaque(false);cartCenter.add(Ui.scroll(cartTable));
        cartCenter.add(Ui.row(Ui.button("Change quantity",this::changeQuantity),Ui.button("Remove",this::remove)),BorderLayout.SOUTH);cartCard.add(cartCenter);
        JPanel totalRow=new JPanel(new BorderLayout());totalRow.setOpaque(false);totalRow.add(Ui.label("TOTAL DUE",11,Ui.MUTED,true));totalRow.add(total,BorderLayout.EAST);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        JPanel payActions=new JPanel(new BorderLayout(8,0));payActions.setOpaque(false);payActions.add(Ui.primary("Complete sale  →",this::checkout));payActions.add(Ui.button("Clear",this::clear),BorderLayout.EAST);
        payActions.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        JPanel checkout=Ui.stack(10,totalRow,Ui.fieldRow("PAYMENT RECEIVED · PHP",payment),Ui.row(Ui.label("Change",13,Ui.MUTED,false),change),
            payActions,Ui.label("Cash sales · PHP · No additional tax or discounts",10,Ui.MUTED,false));
        cartCard.add(checkout,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,productCard,cartCard);split.setResizeWeight(.52);split.setBorder(null);split.setDividerSize(18);
        productCard.setMinimumSize(new Dimension(420,100));cartCard.setMinimumSize(new Dimension(405,100));add(split);
        Ui.onChange(search,this::filter);category.addActionListener(e -> {if(!filtering)filter();});Ui.onChange(payment,this::calculate);
        catalog.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)Ui.guarded(catalog,SalesPanel.this::addToCart);}});
    }
    @Override public boolean hasUnsavedChanges() {return !cart.isEmpty();}
    @Override public void refresh() {
        if(working)return;
        Ui.async(this,() -> new Data(app.products.search(session,"",0),app.products.categories(session)),data -> {
            products=data.products();Category selected=(Category)category.getSelectedItem();filtering=true;
            category.removeAllItems();category.addItem(new Category(0,"All categories"));data.categories().forEach(category::addItem);
            if(selected!=null)for(int i=0;i<category.getItemCount();i++)if(category.getItemAt(i).id()==selected.id())category.setSelectedIndex(i);
            filtering=false;filter();renderCart();
        });
    }
    private void filter() {
        String q=search.getText().strip().toLowerCase(Locale.ROOT);Category c=(Category)category.getSelectedItem();
        visible=products.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q) && (c==null||c.id()==0||p.category().id()==c.id())).toList();
        Ui.rows(catalog,visible.stream().map(p -> new Object[]{p.name(),Formats.currency(p.price()),p.quantity()==0?"Out of stock":p.quantity()+" units"}).toList());
    }
    private int quantity() {
        try{quantity.commitEdit();}catch(java.text.ParseException e){throw new AppException("Enter a whole-number quantity.");}
        return ((Number)quantity.getValue()).intValue();
    }
    private void addToCart() {
        if(working)return;int index=catalog.getSelectedRow();
        if(index<0 || index>=visible.size())throw new AppException("Select a product to add.");
        Product p=visible.get(index);int qty=quantity()+(cart.containsKey(p.id())?cart.get(p.id()).quantity():0);
        if(qty>p.quantity())throw new AppException("Only "+p.quantity()+" units of "+p.name()+" are available.");
        CartItem previous=cart.get(p.id());
        if(previous!=null && previous.price().compareTo(p.price())!=0)throw new AppException("This product's price changed. Remove it from the cart and add it again.");
        cart.put(p.id(),new CartItem(p.id(),p.name(),p.price(),qty));requestId=UUID.randomUUID();renderCart();
    }
    private CartItem selectedItem() {
        int i=cartTable.getSelectedRow();List<CartItem> list=List.copyOf(cart.values());
        if(i<0 || i>=list.size())throw new AppException("Select an item in the cart.");return list.get(i);
    }
    private void changeQuantity() {
        CartItem item=selectedItem();String value=JOptionPane.showInputDialog(this,"New quantity for "+item.name()+":",item.quantity());
        if(value==null)return;int qty=Validation.integer(value,"Quantity");if(qty==0)throw new AppException("Quantity must be greater than zero. Use Remove to delete an item.");
        Product product=products.stream().filter(p -> p.id()==item.productId()).findFirst().orElseThrow(() -> new AppException("Product is unavailable. Refresh and remove it."));
        if(qty>product.quantity())throw new AppException("Only "+product.quantity()+" units are available.");
        cart.put(item.productId(),new CartItem(item.productId(),item.name(),item.price(),qty));requestId=UUID.randomUUID();renderCart();
    }
    private void remove() {CartItem item=selectedItem();cart.remove(item.productId());requestId=UUID.randomUUID();renderCart();}
    private void clear() {
        if(cart.isEmpty())return;if(!Ui.confirm(this,"Clear current sale?","Remove all items from the cart?"))return;
        cart.clear();payment.setText("");requestId=UUID.randomUUID();renderCart();
    }
    private BigDecimal total() {return cart.values().stream().map(CartItem::subtotal).reduce(new BigDecimal("0.00"),BigDecimal::add);}
    private void renderCart() {
        Ui.rows(cartTable,cart.values().stream().map(i -> new Object[]{i.name(),i.quantity(),Formats.currency(i.price()),Formats.currency(i.subtotal())}).toList());
        cartCount.setText(cart.values().stream().mapToLong(CartItem::quantity).sum()+" items");total.setText(Formats.currency(total()));calculate();
    }
    private void calculate() {
        try{BigDecimal tender=Validation.money(payment.getText(),"Payment",false),balance=tender.subtract(total());change.setText(Formats.currency(balance));change.setForeground(balance.signum()<0?new Color(177,65,64):Ui.GREEN);}
        catch(AppException e){change.setText("—");change.setForeground(Ui.MUTED);}
    }
    private void checkout() {
        if(working)return;if(cart.isEmpty())throw new AppException("Add at least one product to the cart.");
        BigDecimal tender=Validation.money(payment.getText(),"Payment",true);
        if(tender.compareTo(total())<0)throw new AppException("Payment is less than the sale total.");
        List<CartItem> snapshot=List.copyOf(cart.values());UUID key=requestId;working=true;
        Ui.async(this,() -> {
            try{return app.sales.checkout(session,key,snapshot,tender);}
            finally{SwingUtilities.invokeLater(() -> working=false);}
        },sale -> {
            cart.clear();requestId=UUID.randomUUID();payment.setText("");renderCart();refresh();ReceiptDialog.show(this,app,sale);
        });
    }
}
