package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.*;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.List;
import java.util.*;
public final class TransactionsPanel extends AppPanel {
    private final JTextField from=Ui.field("YYYY-MM-DD",10),to=Ui.field("YYYY-MM-DD",10);
    private final JTable table=Ui.table("TRANSACTION","DATE & TIME","SERVED BY","ITEMS","TOTAL","PAYMENT","CHANGE");
    private final JLabel caption=Ui.label("",12,Ui.MUTED,false);private List<Sale> sales=List.of();
    public TransactionsPanel(DashboardFrame frame) {
        super(frame);LocalDate today=LocalDate.now(app.config.zone());from.setText(today.minusDays(29).toString());to.setText(today.toString());
        add(heading("Transactions",user.can(Permission.VIEW_ALL_SALES)?"A clear record of every completed sale.":"Your completed sales. Only your transactions are visible.",Ui.row(Ui.button("Refresh",this::refresh))),BorderLayout.NORTH);
        Ui.columnWidths(table,110,200,150,60,100,100,100);
        JPanel card=Ui.card();card.add(Ui.stack(12,Ui.row(Ui.label("From",12,Ui.MUTED,false),from,Ui.label("To",12,Ui.MUTED,false),to,Ui.primary("Apply dates",this::refresh),
            Ui.button("Today",() -> {from.setText(LocalDate.now(app.config.zone()).toString());to.setText(from.getText());refresh();})),caption),BorderLayout.NORTH);
        card.add(Ui.scroll(table));card.add(Ui.row(Ui.primary("View receipt",this::open),Ui.button("Export transactions",this::export)),BorderLayout.SOUTH);add(card);
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)Ui.guarded(table,TransactionsPanel.this::open);}});
    }
    @Override public void refresh() {
        LocalDate first=Formats.parseDate(from.getText()),last=Formats.parseDate(to.getText());
        Ui.async(this,() -> app.sales.history(session,first,last),values -> {
            sales=values;Ui.rows(table,values.stream().map(s -> new Object[]{s.reference(),Formats.date(s.createdAt(),app.config.zone()),s.staffName(),Formats.number(s.itemCount()),
                Formats.currency(s.total()),Formats.currency(s.payment()),Formats.currency(s.change())}).toList());
            caption.setText(values.size()+" transactions shown · "+first+" to "+last+" · latest 500 maximum · "+app.config.zone());
        });
    }
    private void open() {
        int index=table.getSelectedRow();if(index<0 || index>=sales.size())throw new AppException("Select a transaction first.");
        long id=sales.get(index).id();Ui.async(this,() -> app.sales.find(session,id),sale -> ReceiptDialog.show(this,app,sale));
    }
    private void export() {
        List<List<?>> rows=new ArrayList<>();
        for(Sale s:sales)rows.add(List.of(s.reference(),Formats.date(s.createdAt(),app.config.zone()),s.staffName(),s.itemCount(),s.total(),s.payment(),s.change()));
        Ui.export(this,"transactions.csv",List.of("Transaction","Date "+app.config.zone(),"Staff","Items","Total PHP","Payment PHP","Change PHP"),rows);
    }
}
