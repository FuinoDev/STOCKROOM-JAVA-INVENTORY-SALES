package ph.stockroom.view;
import com.formdev.flatlaf.FlatLightLaf;
import ph.stockroom.service.AppException;
import ph.stockroom.util.CsvExporter;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
public final class Ui {
    public static final Color BG=new Color(244,247,246),INK=new Color(28,46,41),MUTED=new Color(109,125,119),
        GREEN=new Color(29,113,87),DARK=new Color(20,53,44),LINE=new Color(225,233,229),MINT=new Color(226,242,233);
    private Ui() { }
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont",new Font("Segoe UI",Font.PLAIN,14));
        UIManager.put("Panel.background",BG);UIManager.put("Label.foreground",INK);
        UIManager.put("Component.arc",12);UIManager.put("Button.arc",12);UIManager.put("TextComponent.arc",10);
        UIManager.put("Component.focusColor",GREEN);UIManager.put("Component.focusedBorderColor",GREEN);
        UIManager.put("Table.selectionBackground",MINT);UIManager.put("Table.selectionForeground",INK);
        UIManager.put("Table.showVerticalLines",false);UIManager.put("Table.showHorizontalLines",true);
        UIManager.put("Table.gridColor",LINE);UIManager.put("TableHeader.background",new Color(248,250,249));
        UIManager.put("ScrollBar.width",10);UIManager.put("Button.margin",new Insets(10,16,10,16));
        UIManager.put("TextField.margin",new Insets(9,10,9,10));UIManager.put("PasswordField.margin",new Insets(9,10,9,10));
        UIManager.put("ComboBox.padding",new Insets(7,8,7,8));UIManager.put("TabbedPane.selectedBackground",Color.WHITE);
    }
    public static JLabel label(String text,int size,Color color,boolean bold) {
        JLabel l=new JLabel(text);l.putClientProperty("html.disable",true);l.setFont(l.getFont().deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size));l.setForeground(color);return l;
    }
    public static JPanel row(Component... children) {
        JPanel panel=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)) {
            @Override public Dimension getMaximumSize() {return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };panel.setOpaque(false);
        for(Component c:children) panel.add(c);return panel;
    }
    public static JPanel stack(int gap,Component... children) {
        JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        for(int i=0;i<children.length;i++) { if(i>0)p.add(Box.createVerticalStrut(gap));if(children[i] instanceof JComponent j)j.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(children[i]); }return p;
    }
    public static JPanel card() { JPanel p=new JPanel(new BorderLayout(0,14));p.setBackground(Color.WHITE);p.setBorder(new CompoundBorder(new LineBorder(LINE,1,true),new EmptyBorder(22,22,22,22)));return p; }
    public static JButton button(String text,Runnable action) {
        JButton b=new JButton(text);b.setFocusPainted(false);b.addActionListener(e -> guarded(b,action));return b;
    }
    public static JButton primary(String text,Runnable action) { JButton b=button(text,action);b.setBackground(GREEN);b.setForeground(Color.WHITE);b.setFont(b.getFont().deriveFont(Font.BOLD));return b; }
    public static JTextField field(String hint,int columns) { JTextField f=new JTextField(columns);f.putClientProperty("JTextField.placeholderText",hint);return f; }
    public static JPanel fieldRow(String label,JComponent field) {
        JPanel panel=new JPanel(new BorderLayout(0,7));panel.setOpaque(false);panel.add(Ui.label(label,12,MUTED,true),BorderLayout.NORTH);panel.add(field);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,75));panel.setAlignmentX(Component.LEFT_ALIGNMENT);return panel;
    }
    public static JTable table(String... columns) {
        JTable table=new JTable(new DefaultTableModel(columns,0) { @Override public boolean isCellEditable(int r,int c) { return false; } }) {
            @Override public String getToolTipText(java.awt.event.MouseEvent event) {
                int row=rowAtPoint(event.getPoint()),column=columnAtPoint(event.getPoint());
                return row<0 || column<0?null:"Value: "+getValueAt(row,column);
            }
        };
        table.setRowHeight(44);table.setIntercellSpacing(new Dimension(0,1));table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setPreferredSize(new Dimension(0,40));table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer renderer=new DefaultTableCellRenderer();
        renderer.putClientProperty("html.disable",true);renderer.setBorder(new EmptyBorder(0,10,0,10));
        table.setDefaultRenderer(Object.class,renderer);
        table.setFillsViewportHeight(true);table.setBackground(Color.WHITE);
        return table;
    }
    public static void rows(JTable table,List<Object[]> rows) {
        DefaultTableModel model=(DefaultTableModel)table.getModel();model.setRowCount(0);for(Object[] row:rows)model.addRow(row);
    }
    public static JScrollPane scroll(JComponent content) { JScrollPane s=new JScrollPane(content);s.setBorder(new LineBorder(LINE));s.getVerticalScrollBar().setUnitIncrement(24);return s; }
    public static void statusColumn(JTable table,int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            { putClientProperty("html.disable",true);setBorder(new EmptyBorder(0,10,0,10)); }
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int r,int c) {
                super.getTableCellRendererComponent(t,value,selected,focus,r,c);
                if(!selected) setForeground("Out of stock".equals(value)?new Color(177,65,64):"Low stock".equals(value)?new Color(172,111,31):GREEN);
                setFont(getFont().deriveFont(Font.BOLD));return this;
            }
        });
    }
    public static boolean confirm(Component owner,String title,String message) { return JOptionPane.showConfirmDialog(owner,message,title,JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION; }
    public static void info(Component owner,String message) { JOptionPane.showMessageDialog(owner,message,"Stockroom",JOptionPane.INFORMATION_MESSAGE); }
    public static void error(Component owner,Throwable error) {
        Throwable cause=error;
        while(cause instanceof java.util.concurrent.ExecutionException && cause.getCause()!=null)cause=cause.getCause();
        String message=cause instanceof AppException?cause.getMessage():cause instanceof java.io.IOException?"The file could not be saved. Check the folder and try again.":"This operation could not be completed. Please try again.";
        if(!(cause instanceof AppException)) cause.printStackTrace(System.err);
        JOptionPane.showMessageDialog(owner,message,"Please check",JOptionPane.ERROR_MESSAGE);
    }
    public static void guarded(Component owner,Runnable action) { try { action.run(); } catch(Exception e) { error(owner,e); } }
    private static void enabled(Component c,Map<Component,Boolean> state) {
        state.put(c,c.isEnabled());c.setEnabled(false);if(c instanceof Container p)for(Component child:p.getComponents())enabled(child,state);
    }
    public static <T> void async(Component owner,Callable<T> task,Consumer<T> success) {
        if(!owner.isEnabled())return;
        Map<Component,Boolean> state=new IdentityHashMap<>();enabled(owner,state);owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                state.forEach(Component::setEnabled);owner.setCursor(Cursor.getDefaultCursor());
                try { success.accept(get()); } catch(Exception e) { error(owner,e); }
            }
        }.execute();
    }
    public static JDialog dialog(Component owner,String title,JComponent body,JComponent actions,int width) {
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(owner),title,Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel=card();panel.add(body);panel.add(actions,BorderLayout.SOUTH);dialog.setContentPane(panel);
        dialog.setMinimumSize(new Dimension(width,100));dialog.pack();dialog.setSize(width,dialog.getHeight());dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);return dialog;
    }
    public static void onChange(JTextField field,Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){action.run();}public void removeUpdate(DocumentEvent e){action.run();}public void changedUpdate(DocumentEvent e){action.run();}
        });
    }
    public static Path chooseSave(Component owner,String filename) {
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File(filename));
        if(chooser.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return null;
        Path path=chooser.getSelectedFile().toPath();
        if(java.nio.file.Files.exists(path) && !confirm(owner,"Replace file?","This file already exists. Replace it?"))return null;
        return path;
    }
    public static void export(Component owner,String filename,List<String> headings,List<? extends List<?>> rows) {
        Path path=chooseSave(owner,filename);if(path==null)return;
        async(owner,() -> {CsvExporter.write(path,headings,rows);return path;},p -> info(owner,"Export saved to "+p));
    }
    public static Image iconImage() {
        var image=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(GREEN);g.fillRoundRect(0,0,64,64,18,18);g.setColor(Color.WHITE);
        g.fillRoundRect(13,30,17,20,3,3);g.fillRoundRect(34,30,17,20,3,3);g.fillRoundRect(23,10,18,16,3,3);g.dispose();return image;
    }

    public static JLabel brand() {
        JLabel brand=label("STOCKROOM",19,Color.WHITE,true);brand.setIcon(new ImageIcon(iconImage().getScaledInstance(25,25,Image.SCALE_SMOOTH)));brand.setIconTextGap(9);return brand;
    }
    public static void columnWidths(JTable table,int... widths) {
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }
    public static JLabel fittedValue(String text,int size,Color color) {
        JLabel label=label(text,size,color,true);label.setToolTipText(text);
        label.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int fontSize=size;while(fontSize>11 && label.getFontMetrics(label.getFont().deriveFont((float)fontSize)).stringWidth(text)>label.getWidth())fontSize--;
                label.setFont(label.getFont().deriveFont((float)fontSize));
            }
        });
        return label;
    }
    public static Icon navigationIcon(String name) {
        return new Icon() {
            public int getIconWidth(){return 17;}public int getIconHeight(){return 17;}
            public void paintIcon(Component c,Graphics graphics,int x,int y) {
                Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(c.getForeground());g.setStroke(new BasicStroke(1.4f));
                switch(name) {
                    case "Overview" -> {g.drawRect(1,1,5,5);g.drawRect(10,1,5,5);g.drawRect(1,10,5,5);g.drawRect(10,10,5,5);}
                    case "Products" -> {g.drawRect(1,3,14,12);g.drawLine(1,7,15,7);g.drawLine(7,3,7,7);}
                    case "Inventory" -> {for(int i=3;i<16;i+=5){g.drawLine(1,i,3,i);g.drawLine(6,i,15,i);}}
                    case "New sale" -> {g.drawLine(8,1,8,15);g.drawLine(1,8,15,8);}
                    case "Transactions" -> {g.drawLine(2,13,14,2);g.drawLine(7,2,14,2);g.drawLine(14,2,14,9);}
                    case "Reports" -> {g.drawLine(1,16,16,16);g.drawRect(2,9,3,7);g.drawRect(7,5,3,11);g.drawRect(12,1,3,15);}
                    default -> {g.drawOval(5,1,6,6);g.drawArc(1,9,14,13,0,180);}
                }
                g.dispose();
            }
        };
    }
}
