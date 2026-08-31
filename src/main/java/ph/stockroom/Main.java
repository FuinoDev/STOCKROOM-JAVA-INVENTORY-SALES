package ph.stockroom;
import ph.stockroom.config.AppConfig;
import ph.stockroom.service.*;
import ph.stockroom.view.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        boolean check=Arrays.asList(args).contains("--check-db");
        try {
            AppServices app=new AppServices(AppConfig.load());app.database.initialize();boolean setup=app.auth.needsSetup();
            if(check) {System.out.println("PostgreSQL connected. Schema ready. First administrator required: "+setup);return;}
            Ui.install();SwingUtilities.invokeLater(() -> new LoginFrame(app,setup).setVisible(true));
        } catch(Exception e) {
            String message=e instanceof AppException?e.getMessage():"Could not start Stockroom. Check your database configuration and Java installation.";
            if(check || GraphicsEnvironment.isHeadless()){System.err.println(message);System.exit(1);}
            else {Ui.install();JOptionPane.showMessageDialog(null,message+"\n\nSee README.md for setup instructions.","Stockroom could not start",JOptionPane.ERROR_MESSAGE);}
        }
    }
}
