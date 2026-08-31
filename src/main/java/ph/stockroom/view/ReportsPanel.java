package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.ReportService;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.List;
import java.util.*;
public final class ReportsPanel extends AppPanel {
    private final JTextField from=Ui.field("YYYY-MM-DD",10),to=Ui.field("YYYY-MM-DD",10);
    private final JPanel metrics=new JPanel(new GridLayout(1,4,14,0));
    private final JTable daily=Ui.table("DATE","TRANSACTIONS","ITEMS SOLD","REVENUE"),top=Ui.table("PRODUCT","UNITS SOLD","REVENUE"),
        stock=Ui.table("PRODUCT","CATEGORY","QUANTITY","PRICE","INVENTORY VALUE","STATUS");
    private final JLabel caption=Ui.label("",12,Ui.MUTED,false),inventoryCaption=Ui.label("",12,Ui.MUTED,false);
    private ReportService.Report report;
    public ReportsPanel(DashboardFrame frame) {
        super(frame);LocalDate today=LocalDate.now(app.config.zone());from.setText(today.toString());to.setText(today.toString());
        add(heading("Reports","Turn your daily activity into a clearer picture.",Ui.row(Ui.button("Export daily sales",this::exportDaily),Ui.button("Export inventory",this::exportStock))),BorderLayout.NORTH);
        Ui.columnWidths(stock,260,150,80,100,130,110);Ui.columnWidths(top,400,100,120);
        JPanel center=new JPanel(new BorderLayout(0,20));center.setOpaque(false);metrics.setOpaque(false);
        JPanel filters=Ui.stack(14,Ui.row(Ui.label("From",12,Ui.MUTED,false),from,Ui.label("To",12,Ui.MUTED,false),to,Ui.primary("Generate report",this::refresh),
            Ui.button("Today",() -> period(0)),Ui.button("Last 7 days",() -> period(6)),Ui.button("This month",() -> {from.setText(LocalDate.now(app.config.zone()).withDayOfMonth(1).toString());to.setText(LocalDate.now(app.config.zone()).toString());refresh();})),caption,metrics);
        center.add(filters,BorderLayout.NORTH);JTabbedPane tabs=new JTabbedPane();
        JPanel days=Ui.card();days.add(Ui.label("Daily sales · dates with completed transactions",14,Ui.INK,true),BorderLayout.NORTH);days.add(Ui.scroll(daily));tabs.addTab("Daily sales",days);
        JPanel best=Ui.card();best.add(Ui.label("Top 10 products · ranked by units sold",14,Ui.INK,true),BorderLayout.NORTH);best.add(Ui.scroll(top));tabs.addTab("Best sellers",best);
        JPanel inventory=Ui.card();inventory.add(inventoryCaption,BorderLayout.NORTH);inventory.add(Ui.scroll(stock));Ui.statusColumn(stock,5);tabs.addTab("Inventory snapshot",inventory);
        center.add(tabs);add(center);
    }
    private void period(int days) {LocalDate today=LocalDate.now(app.config.zone());from.setText(today.minusDays(days).toString());to.setText(today.toString());refresh();}
    @Override public void refresh() {
        LocalDate start=Formats.parseDate(from.getText()),end=Formats.parseDate(to.getText());
        Ui.async(this,() -> app.reports.generate(session,start,end),this::render);
    }
    private JPanel metric(String label,String value) {JPanel card=Ui.card();card.add(Ui.stack(10,Ui.label(label.toUpperCase(Locale.ROOT),10,Ui.MUTED,true),Ui.fittedValue(value,25,Ui.INK)));return card;}
    private void render(ReportService.Report r) {
        report=r;metrics.removeAll();metrics.add(metric("Sales revenue",Formats.currency(r.sales().revenue())));metrics.add(metric("Transactions",Formats.number(r.sales().transactions())));
        metrics.add(metric("Items sold",Formats.number(r.sales().items())));metrics.add(metric("Current inventory value",Formats.currency(r.inventory().value())));
        caption.setText("Sales period: "+r.from()+" through "+r.to()+" · "+app.config.zone()+" · revenue, not profit");
        inventoryCaption.setText(r.inventory().products()+" products · "+Formats.number(r.inventory().units())+" units · "+r.inventory().lowStock()+" low stock · "+r.inventory().outOfStock()+" out of stock · current quantities");
        Ui.rows(daily,r.days().stream().map(d -> new Object[]{d.date(),Formats.number(d.transactions()),Formats.number(d.items()),Formats.currency(d.revenue())}).toList());
        Ui.rows(top,r.top().stream().map(p -> new Object[]{p.name(),Formats.number(p.units()),Formats.currency(p.revenue())}).toList());
        Ui.rows(stock,r.products().stream().map(p -> new Object[]{p.name(),p.category().name(),Formats.number(p.quantity()),Formats.currency(p.price()),Formats.currency(p.inventoryValue()),p.status()}).toList());
        metrics.revalidate();metrics.repaint();
    }
    private void exportDaily() {
        if(report==null)throw new ph.stockroom.service.AppException("Generate a report first.");
        List<List<?>> rows=new ArrayList<>();for(var d:report.days())rows.add(List.of(d.date(),d.transactions(),d.items(),d.revenue()));
        Ui.export(this,"daily-sales-"+report.from()+"-to-"+report.to()+".csv",List.of("Date "+app.config.zone(),"Transactions","Items sold","Revenue PHP"),rows);
    }
    private void exportStock() {
        if(report==null)throw new ph.stockroom.service.AppException("Generate a report first.");
        List<List<?>> rows=new ArrayList<>();for(Product p:report.products())rows.add(List.of(p.id(),p.name(),p.category().name(),p.quantity(),p.minimumStock(),p.price(),p.inventoryValue(),p.status()));
        Ui.export(this,"inventory-snapshot.csv",List.of("Product ID","Product","Category","Quantity","Minimum","Price PHP","Value PHP","Status"),rows);
    }
}
