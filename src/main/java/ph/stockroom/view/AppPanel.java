package ph.stockroom.view;
import ph.stockroom.model.User;
import ph.stockroom.service.*;
import javax.swing.*;
import java.awt.*;
public abstract class AppPanel extends JPanel {
    protected final AppServices app;protected final AuthService.Session session;protected final User user;protected final DashboardFrame frame;
    protected AppPanel(DashboardFrame frame) {
        super(new BorderLayout(0,20));this.frame=frame;this.app=frame.app;this.session=frame.session;this.user=frame.user;
        setBorder(BorderFactory.createEmptyBorder(26,30,26,30));setBackground(Ui.BG);
    }
    protected JPanel heading(String title,String subtitle,JComponent actions) {
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(Ui.stack(7,Ui.label(title,28,Ui.INK,true),Ui.label(subtitle,13,Ui.MUTED,false)));
        if(actions!=null)header.add(actions,BorderLayout.EAST);return header;
    }
    public abstract void refresh();
    public boolean hasUnsavedChanges() { return false; }
}
