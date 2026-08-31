package ph.stockroom.service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ph.stockroom.support.TestDatabase;
import ph.stockroom.model.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class DatabaseIntegrationTest {
    private TestDatabase fixture;private AppServices app;private AuthService.Session admin,staff;private User staffUser;
    private static final char[] SECRET="Testing-password-2026".toCharArray();
    @BeforeEach void setUp() {
        fixture=new TestDatabase();app=fixture.app;admin=app.auth.bootstrap("admin","Store Admin",SECRET);
        staffUser=app.auth.createUser(admin,"cashier","Cashier One",SECRET,"STAFF");staff=app.auth.login("cashier",SECRET);
    }
    @AfterEach void cleanUp() {if(fixture!=null)fixture.close();}
    private Product product(String name,String price,int stock) {
        Category category=app.products.categories(admin).get(0);
        return app.products.create(admin,new Product(0,name,category,new BigDecimal(price),stock,5,true,0,null,null));
    }
    private CartItem item(Product p,int qty) {return new CartItem(p.id(),p.name(),p.price(),qty);}
    private Product current(Product p) {return app.products.search(admin,"",0).stream().filter(x -> x.id()==p.id()).findFirst().orElseThrow();}
    private Sale sell(AuthService.Session actor,Product p,int qty) {return app.sales.checkout(actor,UUID.randomUUID(),List.of(item(p,qty)),new BigDecimal("10000.00"));}
    private long count(String table) {
        if(!Set.of("sales","sale_items","inventory_transactions","users").contains(table))throw new IllegalArgumentException();
        return app.database.read(c -> {try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}});
    }
    @Test void bootstrapLoginAndRoleRestrictions() {
        assertFalse(app.auth.needsSetup());assertInstanceOf(Admin.class,app.auth.current(admin));assertInstanceOf(Staff.class,app.auth.current(staff));
        assertThrows(AppException.class,() -> app.auth.bootstrap("other","Other",SECRET));
        assertThrows(AppException.class,() -> app.auth.login("admin' OR '1'='1",SECRET));
        assertThrows(AppException.class,() -> app.auth.current(new AuthService.Session(UUID.randomUUID())));
        Product p=product("Milk","25.00",10);
        assertThrows(AppException.class,() -> app.products.create(staff,p));
        assertThrows(AppException.class,() -> app.products.archive(staff,p.id()));
        assertThrows(AppException.class,() -> app.inventory.addStock(staff,p.id(),2,"Unauthorized"));
        assertThrows(AppException.class,() -> app.auth.createUser(staff,"intruder","Intruder",SECRET,"ADMIN"));
        assertThrows(AppException.class,() -> app.auth.listUsers(staff));
        assertThrows(AppException.class,() -> app.reports.generate(staff,LocalDate.now(),LocalDate.now()));
        assertEquals(1,app.products.search(staff,"",0).size());
    }
    @Test void rateLimitsWrongPasswordsAndNeverStoresPlaintext() {
        for(int i=0;i<5;i++)assertThrows(AppException.class,() -> app.auth.login("admin","wrong-password".toCharArray()));
        assertTrue(assertThrows(AppException.class,() -> app.auth.login("admin",SECRET)).getMessage().contains("10 minutes"));
        String stored=app.database.read(c -> {try(var s=c.createStatement();var r=s.executeQuery("SELECT password_hash FROM users WHERE username='admin'")){r.next();return r.getString(1);}});
        assertTrue(stored.startsWith("pbkdf2-sha256$600000$"));assertFalse(stored.contains(new String(SECRET)));
    }
    @Test void userManagementRevokesSessionsAcrossApplicationInstances() {
        AuthService other=new AuthService(app.database);AuthService.Session remote=other.login("cashier",SECRET);
        app.auth.setActive(admin,staffUser.getId(),false);
        assertThrows(AppException.class,() -> app.auth.current(staff));assertThrows(AppException.class,() -> other.current(remote));
        app.auth.setActive(admin,staffUser.getId(),true);
        AuthService.Session old=other.login("cashier",SECRET);app.auth.resetPassword(admin,staffUser.getId(),"Updated-password-2026".toCharArray());
        assertThrows(AppException.class,() -> other.current(old));assertThrows(AppException.class,() -> app.auth.login("cashier",SECRET));
        assertNotNull(app.auth.login("cashier","Updated-password-2026".toCharArray()));
        assertThrows(AppException.class,() -> app.auth.setActive(admin,app.auth.current(admin).getId(),false));
    }
    @Test void ownPasswordChangePreservesOnlyCurrentSession() {
        AuthService.Session old=app.auth.login("admin",SECRET);
        app.auth.changePassword(admin,SECRET,"Updated-password-2026".toCharArray());
        assertNotNull(app.auth.current(admin));assertThrows(AppException.class,() -> app.auth.current(old));
        assertNotNull(app.auth.login("admin","Updated-password-2026".toCharArray()));
    }
    @Test void catalogCrudSearchUniquenessAndStaleEdits() {
        Product p=product("Milk","25.00",10);
        assertThrows(AppException.class,() -> product("mILK","30.00",5));
        assertEquals(1,app.products.search(staff,"MIL",p.category().id()).size());assertTrue(app.products.search(staff,"%",0).isEmpty());
        Product edited=new Product(p.id(),"Fresh Milk",p.category(),new BigDecimal("27.50"),p.quantity(),3,true,p.version(),null,null);
        app.products.update(admin,edited);assertEquals(new BigDecimal("27.50"),current(p).price());
        assertThrows(AppException.class,() -> app.products.update(admin,edited));
        assertThrows(AppException.class,() -> app.products.archive(admin,p.id()));
        app.inventory.adjust(admin,p.id(),0,10,"Discontinued and removed from shelves");app.products.archive(admin,p.id());
        assertTrue(app.products.search(admin,"Milk",0).isEmpty());assertEquals(2,app.inventory.history(admin,p.id()).size());
    }
    @Test void recordsStockChangesAndRejectsStaleCounts() {
        Product p=product("Rice","58.00",5);app.inventory.addStock(admin,p.id(),10,"Delivery 1001");
        assertEquals(15,current(p).quantity());
        assertThrows(AppException.class,() -> app.inventory.adjust(admin,p.id(),4,5,"Stale count"));
        assertThrows(AppException.class,() -> app.inventory.addStock(admin,p.id(),0,"Zero"));
        assertThrows(AppException.class,() -> app.inventory.addStock(admin,p.id(),1,""));
        app.inventory.adjust(admin,p.id(),12,15,"Physical count");List<InventoryTransaction> history=app.inventory.history(admin,p.id());
        assertEquals(3,history.size());assertEquals(-3,history.get(0).quantity());assertEquals(12,history.get(0).newStock());assertEquals(15,history.get(0).previousStock());
    }
    @Test void completesSaleAndPreservesSnapshotAfterProductRename() {
        Product coke=product("Coke","25.00",50),bread=product("Bread","35.00",20);
        Sale sale=app.sales.checkout(staff,UUID.randomUUID(),List.of(item(coke,3),item(bread,2)),new BigDecimal("200.00"));
        assertEquals(new BigDecimal("145.00"),sale.total());assertEquals(new BigDecimal("55.00"),sale.change());
        assertEquals(47,current(coke).quantity());assertEquals(18,current(bread).quantity());assertEquals(5,sale.itemCount());
        assertEquals(1,count("sales"));assertEquals(2,count("sale_items"));assertEquals(4,count("inventory_transactions"));
        Product updated=current(coke);app.products.update(admin,new Product(updated.id(),"Renamed Coke",updated.category(),new BigDecimal("30.00"),updated.quantity(),10,true,updated.version(),null,null));
        Sale saved=app.sales.find(staff,sale.id());assertEquals("Coke",saved.items().get(0).productName());assertEquals(new BigDecimal("25.00"),saved.items().get(0).price());
    }
    @Test void rejectsInvalidPaymentsEmptyCartsAndOversellingWithoutSideEffects() {
        Product p=product("Milk","25.00",5),q=product("Bread","35.00",1);
        assertThrows(AppException.class,() -> app.sales.checkout(staff,UUID.randomUUID(),List.of(),new BigDecimal("100")));
        assertThrows(AppException.class,() -> app.sales.checkout(staff,UUID.randomUUID(),List.of(item(p,0)),new BigDecimal("100")));
        assertThrows(AppException.class,() -> app.sales.checkout(staff,UUID.randomUUID(),List.of(item(p,2)),new BigDecimal("49.99")));
        assertThrows(AppException.class,() -> app.sales.checkout(staff,UUID.randomUUID(),List.of(item(p,1),item(p,1)),new BigDecimal("100")));
        assertThrows(AppException.class,() -> app.sales.checkout(staff,UUID.randomUUID(),List.of(item(p,2),item(q,2)),new BigDecimal("200")));
        assertEquals(5,current(p).quantity());assertEquals(1,current(q).quantity());assertEquals(0,count("sales"));assertEquals(0,count("sale_items"));assertEquals(2,count("inventory_transactions"));
    }
    @Test void rejectsStalePrices() {
        Product p=product("Milk","25.00",10);app.products.update(admin,new Product(p.id(),p.name(),p.category(),new BigDecimal("30.00"),10,5,true,p.version(),null,null));
        assertThrows(AppException.class,() -> sell(staff,p,1));assertEquals(10,current(p).quantity());assertEquals(0,count("sales"));
    }
    @Test void rollsBackEvenWhenTheDatabaseFailsAfterSaleHeaderInsert() {
        Product p=product("Milk","25.00",10);
        app.database.read(c -> {try(var s=c.createStatement()){s.execute("""
            CREATE FUNCTION reject_sale_item() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'Deliberate integration test failure'; END $$;
            CREATE TRIGGER fail_test BEFORE INSERT ON sale_items FOR EACH ROW EXECUTE FUNCTION reject_sale_item();
            """);}return null;});
        assertThrows(AppException.class,() -> sell(staff,p,2));assertEquals(0,count("sales"));assertEquals(0,count("sale_items"));assertEquals(10,current(p).quantity());assertEquals(1,count("inventory_transactions"));
    }
    @Test void repeatedCheckoutKeyDoesNotDoubleSell() {
        Product p=product("Milk","25.00",10);UUID key=UUID.randomUUID();
        Sale first=app.sales.checkout(staff,key,List.of(item(p,2)),new BigDecimal("100"));
        Sale retry=app.sales.checkout(staff,key,List.of(item(p,2)),new BigDecimal("100"));
        assertEquals(first.id(),retry.id());assertEquals(8,current(p).quantity());assertEquals(1,count("sales"));
        assertThrows(AppException.class,() -> app.sales.checkout(admin,key,List.of(item(p,2)),new BigDecimal("100")));
    }
    @Test void concurrentPurchasesCannotOversell() throws Exception {
        Product p=product("Last bottles","25.00",5);var pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Boolean> checkout=() -> {start.await(5,TimeUnit.SECONDS);try{sell(staff,p,4);return true;}catch(AppException e){assertTrue(e.getMessage().contains("Insufficient stock"));return false;}};
            Future<Boolean> a=pool.submit(checkout),b=pool.submit(checkout);start.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals(1,current(p).quantity());assertEquals(1,count("sales"));
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentDuplicateRequestsCreateOneReceipt() throws Exception {
        Product p=product("Milk","25.00",10);UUID key=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Sale> checkout=() -> {start.await(5,TimeUnit.SECONDS);return app.sales.checkout(staff,key,List.of(item(p,2)),new BigDecimal("100"));};
            Future<Sale> a=pool.submit(checkout),b=pool.submit(checkout);start.countDown();
            assertEquals(a.get(15,TimeUnit.SECONDS).id(),b.get(15,TimeUnit.SECONDS).id());assertEquals(8,current(p).quantity());assertEquals(1,count("sales"));
        } finally {pool.shutdownNow();}
    }
    @Test void staffCannotViewOtherTransactions() {
        Product p=product("Milk","25.00",10);Sale own=sell(staff,p,1),other=sell(admin,p,1);LocalDate today=LocalDate.now(app.config.zone());
        assertEquals(1,app.sales.history(staff,today,today).size());assertEquals(2,app.sales.history(admin,today,today).size());
        assertEquals(own.id(),app.sales.find(staff,own.id()).id());assertThrows(AppException.class,() -> app.sales.find(staff,other.id()));
        assertEquals(1,app.reports.overview(staff).today().transactions());assertEquals(2,app.reports.overview(admin).today().transactions());
    }
    @Test void reportsRespectManilaMidnightAndUseExactTotals() {
        Product p=product("Milk","25.00",20);Sale before=sell(staff,p,1),inside=sell(staff,p,2),after=sell(staff,p,3);
        setTime(before,"2026-08-30T15:59:59Z");setTime(inside,"2026-08-30T16:00:00Z");setTime(after,"2026-08-31T16:00:00Z");
        var r=app.reports.generate(admin,LocalDate.of(2026,8,31),LocalDate.of(2026,8,31));
        assertEquals(1,r.sales().transactions());assertEquals(2,r.sales().items());assertEquals(new BigDecimal("50.00"),r.sales().revenue());
        assertEquals(LocalDate.of(2026,8,31),r.days().get(0).date());assertEquals(14,r.inventory().units());assertEquals(new BigDecimal("350.00"),r.inventory().value());
        assertEquals(1,app.sales.history(staff,LocalDate.of(2026,8,31),LocalDate.of(2026,8,31)).size());
    }
    private void setTime(Sale sale,String instant) {app.database.read(c -> {try(var s=c.prepareStatement("UPDATE sales SET created_at=? WHERE id=?")){s.setTimestamp(1,Timestamp.from(Instant.parse(instant)));s.setLong(2,sale.id());s.executeUpdate();}return null;});}
    @Test void sampleCatalogIsOptionalAndCannotBeLoadedTwice() {
        app.products.seedDemo(admin);assertEquals(12,app.products.search(admin,"",0).size());assertThrows(AppException.class,() -> app.products.seedDemo(admin));
        var overview=app.reports.overview(admin);assertEquals(2,overview.inventory().lowStock());assertEquals(1,overview.inventory().outOfStock());assertEquals(0,overview.today().transactions());
    }
}
