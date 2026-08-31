package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.ReportService;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
public final class OverviewPanel extends AppPanel {
    private final JPanel content=new JPanel(new BorderLayout(0,22));private boolean loading;
    public OverviewPanel(DashboardFrame frame) {
        super(frame);content.setOpaque(false);
        add(heading("Your store, at a glance.","Keep the shelves moving and the day running smoothly.",Ui.row(Ui.button("Refresh",this::refresh),Ui.primary("+  New sale",() -> frame.navigate("New sale")))),BorderLayout.NORTH);
        add(content);content.add(Ui.label("Loading your workspace…",15,Ui.MUTED,false));
    }
    @Override public void refresh() {
        if(loading)return;loading=true;
        Ui.async(this,() -> { try{return app.reports.overview(session);}finally{SwingUtilities.invokeLater(() -> loading=false);} },this::render);
    }
    private JPanel metric(String title,String value,String note,boolean accent) {
        JPanel card=Ui.card();card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Ui.LINE),BorderFactory.createEmptyBorder(19,19,19,19)));
        if(accent)card.setBackground(Ui.GREEN);
        card.add(Ui.stack(10,Ui.label(title.toUpperCase(Locale.ROOT),10,accent?new Color(195,226,207):Ui.MUTED,true),
            Ui.fittedValue(value,25,accent?Color.WHITE:Ui.INK),Ui.label(note,11,accent?new Color(198,224,209):Ui.MUTED,false)));
        return card;
    }
    private void render(ReportService.Overview overview) {
        content.removeAll();var inventory=overview.inventory();var sales=overview.today();
        JPanel metrics=new JPanel(new GridLayout(1,5,14,0));metrics.setOpaque(false);
        metrics.add(metric("Total products",Formats.number(inventory.products()),"Active in your catalog",false));
        metrics.add(metric("Stock on hand",Formats.number(inventory.units()),"Units across all products",false));
        metrics.add(metric("Needs attention",Formats.number(inventory.lowStock()+inventory.outOfStock()),inventory.outOfStock()+" out of stock",false));
        metrics.add(metric("Inventory value",Formats.currency(inventory.value()),"At current selling prices",false));
        metrics.add(metric(user.can(Permission.VIEW_ALL_SALES)?"Today's sales":"Your sales today",Formats.currency(sales.revenue()),sales.transactions()+" transactions · "+sales.items()+" items",true));
        content.add(metrics,BorderLayout.NORTH);
        JPanel lower=new JPanel(new GridLayout(2,1,0,20));lower.setOpaque(false);
        JPanel chartCard=Ui.card();chartCard.add(Ui.row(Ui.label("Sales over the last 7 days",17,Ui.INK,true),Ui.label(user.can(Permission.VIEW_ALL_SALES)?"All staff":"Your sales only",11,Ui.MUTED,false)),BorderLayout.NORTH);
        chartCard.add(new WeekChart(overview.week(),LocalDate.now(app.config.zone())));
        lower.add(chartCard);
        JPanel alerts=Ui.card();JPanel alertHead=new JPanel(new BorderLayout());alertHead.setOpaque(false);
        alertHead.add(Ui.stack(5,Ui.label("Time to restock",17,Ui.INK,true),Ui.label("Products at or below their minimum stock level.",12,Ui.MUTED,false)));
        alertHead.add(Ui.button("View inventory  →",() -> frame.navigate("Inventory")),BorderLayout.EAST);alerts.add(alertHead,BorderLayout.NORTH);
        JTable table=Ui.table("PRODUCT","CATEGORY","ON HAND","MINIMUM","STATUS");Ui.columnWidths(table,330,180,90,90,150);Ui.statusColumn(table,4);
        Ui.rows(table,overview.alerts().stream().map(p -> new Object[]{p.name(),p.category().name(),Formats.number(p.quantity()),Formats.number(p.minimumStock()),p.status()}).toList());
        if(overview.alerts().isEmpty())alerts.add(Ui.label("All stocked up. No low-stock products right now.",15,Ui.GREEN,false));else alerts.add(Ui.scroll(table));
        lower.add(alerts);content.add(lower);content.revalidate();content.repaint();
    }
    static final class WeekChart extends JComponent {
        private final List<ReportService.DailySales> days;private final LocalDate today;
        WeekChart(List<ReportService.DailySales> days,LocalDate today) {this.days=days;this.today=today;setPreferredSize(new Dimension(700,190));}
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int left=88,right=22,top=28,bottom=32,w=getWidth()-left-right,h=Math.max(30,getHeight()-top-bottom);
            Map<LocalDate,BigDecimal> values=new HashMap<>();days.forEach(d -> values.put(d.date(),d.revenue()));
            double max=Math.max(100,values.values().stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0)*1.18);
            g.setFont(getFont()==null?new Font("Segoe UI",Font.PLAIN,11):getFont().deriveFont(11f));
            for(int i=0;i<=3;i++){int y=top+h-h*i/3;g.setColor(Ui.LINE);g.drawLine(left,y,getWidth()-right,y);g.setColor(Ui.MUTED);g.drawString("₱"+String.format(Locale.US,"%,.0f",max*i/3),3,y+4);}
            for(int i=0;i<7;i++){
                LocalDate day=today.minusDays(6-i);double value=values.getOrDefault(day,BigDecimal.ZERO).doubleValue();
                int step=Math.max(1,w/7),bar=Math.min(54,step-20),x=left+i*step+(step-bar)/2,height=(int)(value/max*h);
                g.setColor(i==6?Ui.GREEN:new Color(163,203,178));g.fillRoundRect(x,top+h-height,bar,Math.max(value>0?3:0,height),7,7);
                String label=day.format(DateTimeFormatter.ofPattern("EEE",Locale.ENGLISH));g.setColor(Ui.MUTED);g.drawString(label,x+(bar-g.getFontMetrics().stringWidth(label))/2,top+h+23);
                if(value>0){String amount=String.format(Locale.US,"%,.2f",value);g.setColor(Ui.INK);g.drawString(amount,x+(bar-g.getFontMetrics().stringWidth(amount))/2,top+h-height-7);}
            }
            if(values.isEmpty()){g.setColor(Ui.MUTED);String empty="Your first sale will appear here.";g.drawString(empty,left+(w-g.getFontMetrics().stringWidth(empty))/2,top+h/2);}
            g.dispose();
        }
    }
}
