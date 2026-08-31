package ph.stockroom.view;
import ph.stockroom.model.*;
import ph.stockroom.service.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
public final class DashboardFrame extends JFrame {
    final AppServices app;final AuthService.Session session;final User user;
    private final JPanel pages=new JPanel(new CardLayout()),nav=new JPanel();private final Map<String,AppPanel> panels=new LinkedHashMap<>();
    private final Map<String,JButton> links=new LinkedHashMap<>();private String active="";
    private final JLabel breadcrumb=Ui.label("WORKSPACE / OVERVIEW",11,Ui.MUTED,true);
    private final javax.swing.Timer refreshTimer;
    public DashboardFrame(AppServices app,AuthService.Session session,User user) {
        super(app.config.businessName()+" · Inventory & Sales");this.app=app;this.session=session;this.user=user;
        setIconImage(Ui.iconImage());setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){close();}});
        JPanel shell=new JPanel(new BorderLayout());
        JPanel sidebar=new JPanel(new BorderLayout(0,28));sidebar.setBackground(Ui.DARK);sidebar.setBorder(BorderFactory.createEmptyBorder(28,20,20,20));sidebar.setPreferredSize(new Dimension(225,0));
        sidebar.add(Ui.stack(8,Ui.brand(),Ui.label("INVENTORY & SALES",10,new Color(151,184,166),true)),BorderLayout.NORTH);
        nav.setOpaque(false);nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));
        addPage("Overview",new OverviewPanel(this));addPage("Products",new ProductPanel(this));addPage("Inventory",new InventoryPanel(this));
        addPage("New sale",new SalesPanel(this));addPage("Transactions",new TransactionsPanel(this));
        if(user.can(Permission.VIEW_REPORTS))addPage("Reports",new ReportsPanel(this));
        if(user.can(Permission.MANAGE_USERS))addPage("Users",new UsersPanel(this));
        sidebar.add(nav);
        JButton account=Ui.button("Change password",this::changePassword);account.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton logout=Ui.button("Sign out",this::logout);logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(Ui.stack(8,Ui.label(user.getDisplayName(),15,Color.WHITE,true),Ui.label(user.getRole()+"  ·  @"+user.getUsername(),11,new Color(161,193,175),false),Box.createVerticalStrut(5),account,logout),BorderLayout.SOUTH);
        JPanel workspace=new JPanel(new BorderLayout());JPanel top=new JPanel(new BorderLayout());top.setBackground(Color.WHITE);top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,Ui.LINE),BorderFactory.createEmptyBorder(18,30,18,30)));
        top.add(breadcrumb);top.add(Ui.label(LocalDate.now(app.config.zone()).format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy",Locale.ENGLISH))+"   ·   PHP",12,Ui.MUTED,false),BorderLayout.EAST);
        workspace.add(top,BorderLayout.NORTH);workspace.add(pages);shell.add(sidebar,BorderLayout.WEST);shell.add(workspace);setContentPane(shell);
        setSize(1380,870);setMinimumSize(new Dimension(1160,730));setLocationRelativeTo(null);navigate("Overview");
        refreshTimer=new javax.swing.Timer(60_000,e -> { if(active.equals("Overview") && isActive())panels.get(active).refresh(); });refreshTimer.start();
    }
    private void addPage(String name,AppPanel panel) {
        panels.put(name,panel);pages.add(panel,name);
        String prefix=switch(name){case "Overview"->"◫";case "Products"->"▦";case "Inventory"->"≡";case "New sale"->"+";case "Transactions"->"↗";case "Reports"->"▥";default->"○";};
        JButton link=Ui.button(name,() -> navigate(name));link.setIcon(Ui.navigationIcon(name));link.setIconTextGap(12);
        link.setHorizontalAlignment(SwingConstants.LEFT);link.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));link.setAlignmentX(Component.LEFT_ALIGNMENT);
        link.setBackground(Ui.DARK);link.setForeground(new Color(197,216,205));link.setBorderPainted(false);nav.add(link);nav.add(Box.createVerticalStrut(7));links.put(name,link);
    }
    public void navigate(String name) {
        if(!panels.containsKey(name))return;active=name;((CardLayout)pages.getLayout()).show(pages,name);breadcrumb.setText("WORKSPACE / "+name.toUpperCase(Locale.ROOT));
        links.forEach((n,b) -> {b.setBackground(n.equals(name)?new Color(49,83,67):Ui.DARK);b.setForeground(n.equals(name)?Color.WHITE:new Color(197,216,205));});
        panels.get(name).refresh();
    }
    private boolean leaveAllowed(String message) {
        boolean cart=panels.values().stream().anyMatch(AppPanel::hasUnsavedChanges);
        return Ui.confirm(this,cart?"Discard the unfinished sale?":"Leave workspace?",message+(cart?"\nYour unsaved cart will be discarded.":""));
    }
    private void logout() {
        if(!leaveAllowed("Are you sure you want to sign out?"))return;
        app.auth.logout(session);refreshTimer.stop();dispose();new LoginFrame(app,false).setVisible(true);
    }
    private void close() { if(leaveAllowed("Are you sure you want to close Stockroom?")){app.auth.logout(session);refreshTimer.stop();dispose();} }
    private void changePassword() {
        JPasswordField old=new JPasswordField(24),next=new JPasswordField(24),repeat=new JPasswordField(24);
        JPanel body=Ui.stack(14,Ui.fieldRow("CURRENT PASSWORD",old),Ui.fieldRow("NEW PASSWORD · AT LEAST 10 CHARACTERS",next),Ui.fieldRow("CONFIRM NEW PASSWORD",repeat));
        JDialog[] d=new JDialog[1];JButton save=Ui.primary("Update password",() -> {
            char[] a=old.getPassword(),b=next.getPassword(),c=repeat.getPassword();
            if(!Arrays.equals(b,c)){Arrays.fill(a,'\0');Arrays.fill(b,'\0');Arrays.fill(c,'\0');throw new AppException("Passwords do not match.");}
            Arrays.fill(c,'\0');Ui.async(d[0].getContentPane(),() -> {try{app.auth.changePassword(session,a,b);return true;}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');}},ok -> {d[0].dispose();Ui.info(this,"Your password has been updated.");});
        });d[0]=Ui.dialog(this,"Change password",body,Ui.row(save),460);d[0].setVisible(true);
    }
}
