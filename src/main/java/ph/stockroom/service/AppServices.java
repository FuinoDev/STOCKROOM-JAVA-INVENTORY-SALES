package ph.stockroom.service;
import ph.stockroom.config.AppConfig;
import ph.stockroom.database.DatabaseConnection;
public final class AppServices {
    public final AppConfig config;
    public final DatabaseConnection database;
    public final AuthService auth;
    public final ProductService products;
    public final InventoryService inventory;
    public final SalesService sales;
    public final ReportService reports;
    public AppServices(AppConfig config) {
        this.config=config;database=new DatabaseConnection(config);auth=new AuthService(database);
        products=new ProductService(database,auth);inventory=new InventoryService(database,auth);
        sales=new SalesService(database,auth,config.zone());reports=new ReportService(database,auth,config.zone());
    }
}
