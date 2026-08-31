package ph.stockroom.view;
import ph.stockroom.model.User;
import ph.stockroom.service.AppException;
import ph.stockroom.util.Formats;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
public final class UsersPanel extends AppPanel {
    private final JTable table=Ui.table("NAME","USERNAME","ROLE","STATUS","CREATED");private List<User> users=List.of();
    public UsersPanel(DashboardFrame frame) {
        super(frame);add(heading("Your team","The right access for the people you trust.",Ui.row(Ui.button("Refresh",this::refresh),Ui.primary("+  Add user",this::create))),BorderLayout.NORTH);
        JPanel card=Ui.card();card.add(Ui.label("Admin: full access · Staff: products, inventory and their own sales",12,Ui.MUTED,false),BorderLayout.NORTH);
        card.add(Ui.scroll(table));card.add(Ui.row(Ui.button("Activate / deactivate",this::toggle),Ui.button("Reset password",this::reset)),BorderLayout.SOUTH);add(card);
    }
    @Override public void refresh() {
        Ui.async(this,() -> app.auth.listUsers(session),values -> {users=values;Ui.rows(table,users.stream().map(u -> new Object[]{u.getDisplayName(),u.getUsername(),u.getRole(),u.isActive()?"Active":"Inactive",Formats.date(u.getCreatedAt(),app.config.zone())}).toList());});
    }
    private User selected() {int index=table.getSelectedRow();if(index<0 || index>=users.size())throw new AppException("Select a user first.");return users.get(index);}
    private void create() {
        JTextField name=Ui.field("Full name",24),username=Ui.field("username",24);
        JPasswordField password=new JPasswordField(24),repeat=new JPasswordField(24);JComboBox<String> role=new JComboBox<>(new String[]{"STAFF","ADMIN"});
        JPanel body=Ui.stack(14,Ui.fieldRow("DISPLAY NAME",name),Ui.fieldRow("USERNAME",username),Ui.fieldRow("ROLE",role),Ui.fieldRow("PASSWORD · AT LEAST 10 CHARACTERS",password),Ui.fieldRow("CONFIRM PASSWORD",repeat));
        JDialog[] d=new JDialog[1];JButton save=Ui.primary("Create user",() -> {
            String display=name.getText(),login=username.getText(),chosen=(String)role.getSelectedItem();char[] secret=password.getPassword(),confirmation=repeat.getPassword();
            if(!Arrays.equals(secret,confirmation)){Arrays.fill(secret,'\0');Arrays.fill(confirmation,'\0');throw new AppException("Passwords do not match.");}
            Arrays.fill(confirmation,'\0');
            Ui.async(d[0].getContentPane(),() -> {try{return app.auth.createUser(session,login,display,secret,chosen);}finally{Arrays.fill(secret,'\0');}},u -> {d[0].dispose();refresh();});
        });
        d[0]=Ui.dialog(this,"Add team member",body,Ui.row(Ui.button("Cancel",() -> d[0].dispose()),save),460);d[0].getRootPane().setDefaultButton(save);d[0].setVisible(true);
    }
    private void toggle() {
        User target=selected();if(!Ui.confirm(this,"Change account access?",(target.isActive()?"Deactivate ":"Activate ")+target.getDisplayName()+"?"))return;
        Ui.async(this,() -> {app.auth.setActive(session,target.getId(),!target.isActive());return true;},ok -> refresh());
    }
    private void reset() {
        User target=selected();
        if(target.getId()==user.getId())throw new AppException("Use Change password in the sidebar to update your own password.");
        JPasswordField password=new JPasswordField(24),repeat=new JPasswordField(24);
        JPanel body=Ui.stack(14,Ui.label("Reset password for "+target.getDisplayName(),16,Ui.INK,true),Ui.fieldRow("NEW PASSWORD · AT LEAST 10 CHARACTERS",password),Ui.fieldRow("CONFIRM PASSWORD",repeat),
            Ui.label("Existing sessions for this user will be signed out.",11,Ui.MUTED,false));
        JDialog[] d=new JDialog[1];JButton save=Ui.primary("Reset password",() -> {
            char[] secret=password.getPassword(),confirmation=repeat.getPassword();
            if(!Arrays.equals(secret,confirmation)){Arrays.fill(secret,'\0');Arrays.fill(confirmation,'\0');throw new AppException("Passwords do not match.");}Arrays.fill(confirmation,'\0');
            Ui.async(d[0].getContentPane(),() -> {try{app.auth.resetPassword(session,target.getId(),secret);return true;}finally{Arrays.fill(secret,'\0');}},ok -> {d[0].dispose();Ui.info(this,"Password reset. Share the new password with the user securely.");});
        });
        d[0]=Ui.dialog(this,"Reset user password",body,Ui.row(Ui.button("Cancel",() -> d[0].dispose()),save),500);d[0].setVisible(true);
    }
}
