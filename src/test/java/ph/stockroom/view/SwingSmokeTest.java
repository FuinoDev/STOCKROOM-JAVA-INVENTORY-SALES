package ph.stockroom.view;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ph.stockroom.support.TestDatabase;
import ph.stockroom.service.*;
import ph.stockroom.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class SwingSmokeTest {
    private TestDatabase fixture;private DashboardFrame frame;private LoginFrame login;
    private static final Path SCREENSHOTS=Path.of("target","screenshots");
    private static <T> T edt(Callable<T> work) throws Exception {
        AtomicReference<T> result=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {try{result.set(work.call());}catch(Throwable e){failure.set(e);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());return result.get();
    }
    private static <T> T field(Object instance,String name,Class<T> type) throws Exception {
        Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(instance));
    }
    private static void waitReady(JComponent panel) throws Exception {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(12);
        while(System.nanoTime()<deadline) {
            if(edt(panel::isEnabled)) {edt(() -> {panel.revalidate();return null;});return;}
            Thread.sleep(40);
        }
        fail("Screen did not finish loading.");
    }
    private static void capture(JFrame window,String name) throws Exception {
        Files.createDirectories(SCREENSHOTS);
        edt(() -> {
            if(!window.isDisplayable())window.addNotify();
            window.validate();
            Container content=window.getContentPane();content.setSize(window.getWidth(),window.getHeight());layout(content);
            BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics();content.printAll(graphics);graphics.dispose();
            Set<Integer> colors=new HashSet<>();for(int y=0;y<image.getHeight();y+=10)for(int x=0;x<image.getWidth();x+=10)colors.add(image.getRGB(x,y));
            assertTrue(colors.size()>10,"Screen capture must contain rendered interface content, not a blank image.");
            ImageIO.write(image,"png",SCREENSHOTS.resolve(name+".png").toFile());return null;
        });
    }
    private static void layout(Container container) {container.doLayout();for(Component child:container.getComponents())if(child instanceof Container nested)layout(nested);}
    @AfterEach void cleanup() throws Exception {
        edt(() -> {
            for(Window w:Window.getWindows())w.dispose();
            if(frame!=null) {try{field(frame,"refreshTimer",javax.swing.Timer.class).stop();}catch(Exception ignored){}}
            return null;
        });
        if(fixture!=null)fixture.close();
    }
    @Test void rendersAllScreensAndCalculatesTheSwingCart() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),"Swing screenshots require a desktop session.");
        fixture=new TestDatabase();AppServices app=fixture.app;
        edt(() -> {Ui.install();login=new LoginFrame(app,true);return null;});capture(login,"01-first-run");
        edt(() -> {login.dispose();login=new LoginFrame(app,false);return null;});capture(login,"02-login");
        AuthService.Session session=app.auth.bootstrap("admin","Alex Santos","Testing-password-2026".toCharArray());
        app.products.seedDemo(session);app.auth.createUser(session,"cashier","Sam Reyes","Testing-password-2026".toCharArray(),"STAFF");
        Product water=app.products.search(session,"Absolute",0).get(0);
        for(int i=0;i<7;i++) {
            Sale sale=app.sales.checkout(session,UUID.randomUUID(),List.of(new CartItem(water.id(),water.name(),water.price(),i+1)),new BigDecimal("200.00"));
            final int day=i;
            app.database.read(c -> {try(var s=c.prepareStatement("UPDATE sales SET created_at=? WHERE id=?")){s.setTimestamp(1,java.sql.Timestamp.from(LocalDate.now(app.config.zone()).minusDays(6-day).atTime(12,0).atZone(app.config.zone()).toInstant()));s.setLong(2,sale.id());s.executeUpdate();}return null;});
        }
        User user=app.auth.current(session);edt(() -> {frame=new DashboardFrame(app,session,user);return null;});
        Map<String,AppPanel> panels=field(frame,"panels",Map.class);
        String[] pages={"Overview","Products","Inventory","New sale","Transactions","Reports","Users"};
        for(int i=0;i<pages.length;i++) {
            String page=pages[i];edt(() -> {frame.navigate(page);return null;});
            waitReady(panels.get(page));capture(frame,"%02d-%s".formatted(i+3,page.toLowerCase(Locale.ROOT).replace(' ','-')));
            if(page.equals("Products"))assertEquals(12,edt(() -> field(panels.get(page),"table",JTable.class).getRowCount()));
            if(page.equals("Transactions"))assertEquals(7,edt(() -> field(panels.get(page),"table",JTable.class).getRowCount()));
        }
        AppPanel sales=panels.get("New sale");edt(() -> {frame.navigate("New sale");return null;});waitReady(sales);
        edt(() -> {
            JTextField search=field(sales,"search",JTextField.class);search.setText("Coca-Cola");
            JTable catalog=field(sales,"catalog",JTable.class);assertEquals(1,catalog.getRowCount());catalog.setRowSelectionInterval(0,0);
            field(sales,"quantity",JSpinner.class).setValue(3);
            var add=sales.getClass().getDeclaredMethod("addToCart");add.setAccessible(true);add.invoke(sales);
            field(sales,"payment",JTextField.class).setText("100.00");
            assertEquals("₱75.00",field(sales,"total",JLabel.class).getText());assertEquals("₱25.00",field(sales,"change",JLabel.class).getText());
            assertEquals(1,field(sales,"cartTable",JTable.class).getRowCount());assertTrue(sales.hasUnsavedChanges());
            return null;
        });
        capture(frame,"10-sale-cart");edt(() -> {frame.setSize(1160,730);frame.validate();return null;});capture(frame,"11-minimum-window");
        edt(() -> {JComboBox<?> category=field(sales,"category",JComboBox.class);assertTrue(category.getX()+category.getWidth()<=category.getParent().getWidth());JTable cart=field(sales,"cartTable",JTable.class);assertTrue(cart.getParent().getHeight()>=cart.getRowHeight()*2,"The compact window must show at least two cart rows.");return null;});
        AuthService.Session staff=app.auth.login("cashier","Testing-password-2026".toCharArray());User staffUser=app.auth.current(staff);
        edt(() -> {field(frame,"refreshTimer",javax.swing.Timer.class).stop();frame.dispose();frame=new DashboardFrame(app,staff,staffUser);return null;});
        Map<String,AppPanel> staffPages=field(frame,"panels",Map.class);assertFalse(staffPages.containsKey("Users"));assertFalse(staffPages.containsKey("Reports"));
        waitReady(staffPages.get("Overview"));capture(frame,"12-staff-overview");
    }
}
