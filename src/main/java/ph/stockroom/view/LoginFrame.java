package ph.stockroom.view;
import ph.stockroom.service.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
public final class LoginFrame extends JFrame {
    private final AppServices app;private final boolean setup;
    private final JTextField username=Ui.field("Your username",22),name=Ui.field("Your full name",22);
    private final JPasswordField password=new JPasswordField(22),confirmation=new JPasswordField(22);
    private final JCheckBox samples=new JCheckBox("Start with 12 sample products",true);
    public LoginFrame(AppServices app,boolean setup) {
        super("Stockroom · "+(setup?"Create your administrator":"Welcome back"));this.app=app;this.setup=setup;
        setIconImage(Ui.iconImage());setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root=new JPanel(new GridLayout(1,2));root.setBackground(Color.WHITE);
        JPanel brand=new JPanel(new BorderLayout());brand.setBackground(Ui.DARK);brand.setBorder(BorderFactory.createEmptyBorder(42,40,38,35));
        brand.add(Ui.brand(),BorderLayout.NORTH);
        JPanel pitch=Ui.stack(18,Ui.label("A little more order.",31,Color.WHITE,true),Ui.label("A lot more possibility.",27,new Color(179,220,197),true),
            Ui.label("Your products. Your sales. All together.",15,new Color(196,214,204),false),Box.createVerticalStrut(20),
            feature("01","Know what's on your shelves"),feature("02","Make every sale count"),feature("03","See your business clearly"));
        brand.add(pitch);brand.add(Ui.label("JAVA + POSTGRESQL  /  MADE FOR SMALL BUSINESS",10,new Color(151,184,166),true),BorderLayout.SOUTH);
        JPanel form=Ui.card();form.setBorder(BorderFactory.createEmptyBorder(45,40,35,40));
        password.putClientProperty("JTextField.placeholderText",setup?"At least 10 characters":"Your password");
        password.putClientProperty("JPasswordField.showRevealButton",true);
        confirmation.putClientProperty("JTextField.placeholderText","Repeat your password");
        JPanel fields=Ui.stack(15,Ui.label(setup?"LET'S GET YOU SET UP":"YOUR BUSINESS, AT A GLANCE",11,Ui.GREEN,true),
            Ui.label(setup?"Welcome to Stockroom":"Welcome back.",29,Ui.INK,true),
            Ui.label(setup?"Create the first administrator account.":"Sign in to your workspace.",14,Ui.MUTED,false),Box.createVerticalStrut(5));
        if(setup)fields.add(Ui.fieldRow("DISPLAY NAME",name));
        fields.add(Box.createVerticalStrut(12));fields.add(Ui.fieldRow("USERNAME",username));
        fields.add(Box.createVerticalStrut(12));fields.add(Ui.fieldRow("PASSWORD",password));
        if(setup) { fields.add(Box.createVerticalStrut(12));fields.add(Ui.fieldRow("CONFIRM PASSWORD",confirmation));fields.add(Box.createVerticalStrut(10));samples.setOpaque(false);samples.setAlignmentX(Component.LEFT_ALIGNMENT);fields.add(samples); }
        JButton submit=Ui.primary(setup?"Create workspace  →":"Sign in  →",this::submit);submit.setAlignmentX(Component.LEFT_ALIGNMENT);submit.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        fields.add(Box.createVerticalStrut(22));fields.add(submit);fields.add(Box.createVerticalStrut(16));
        fields.add(Ui.label("A secure, local workspace. No cloud account required.",11,Ui.MUTED,false));
        form.add(fields,BorderLayout.NORTH);root.add(brand);root.add(form);setContentPane(root);
        getRootPane().setDefaultButton(submit);setMinimumSize(new Dimension(950,setup?740:620));pack();setLocationRelativeTo(null);
    }
    private JPanel feature(String number,String text) { return Ui.row(Ui.label(number,12,new Color(142,199,164),true),Ui.label(text,15,Color.WHITE,false)); }
    private void submit() {
        String login=username.getText(),displayName=name.getText();char[] secret=password.getPassword(),repeat=confirmation.getPassword();
        boolean seed=samples.isSelected();
        if(setup && !Arrays.equals(secret,repeat)) { Arrays.fill(secret,'\0');Arrays.fill(repeat,'\0');throw new AppException("Passwords do not match."); }
        Arrays.fill(repeat,'\0');
        Ui.async(getContentPane(),() -> {
            try { return setup?app.auth.bootstrap(login,displayName,secret):app.auth.login(login,secret); }
            finally { Arrays.fill(secret,'\0'); }
        },session -> {
            password.setText("");confirmation.setText("");
            if(setup && seed) {
                Ui.async(getContentPane(),() -> {
                    String warning=null;
                    try {app.products.seedDemo(session);} catch(AppException error) {warning=error.getMessage();}
                    return new SetupResult(app.auth.current(session),warning);
                },result -> {open(session,result.user());if(result.warning()!=null)Ui.info(null,"Your account was created, but sample products could not be loaded. "+result.warning());});
            } else {
                Ui.async(getContentPane(),() -> app.auth.current(session),u -> open(session,u));
            }
        });
    }
    private record SetupResult(ph.stockroom.model.User user,String warning) { }
    private void open(AuthService.Session session,ph.stockroom.model.User user) { dispose();new DashboardFrame(app,session,user).setVisible(true); }
}
