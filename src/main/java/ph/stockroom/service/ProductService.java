package ph.stockroom.service;
import ph.stockroom.dao.*;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.model.*;
import java.math.BigDecimal;
import java.util.*;
public final class ProductService {
    private final DatabaseConnection db;private final AuthService auth;
    private final ProductDAO products=new ProductDAO();private final CategoryDAO categories=new CategoryDAO();private final InventoryDAO movements=new InventoryDAO();
    public ProductService(DatabaseConnection db,AuthService auth) { this.db=db;this.auth=auth; }
    public List<Product> search(AuthService.Session session,String query,long category) {
        return db.read(c -> { auth.require(c,session,Permission.VIEW_PRODUCTS);return products.search(c,query==null?"":query.strip(),category); });
    }
    public List<Category> categories(AuthService.Session session) { return db.read(c -> { auth.require(c,session,Permission.VIEW_PRODUCTS);return categories.findAll(c); }); }
    public Category addCategory(AuthService.Session session,String name) {
        String clean=Validation.text(name,"Category",80);
        return db.transaction(c -> { auth.require(c,session,Permission.MANAGE_PRODUCTS);return categories.insert(c,clean); });
    }
    private Product validated(Product p) {
        String name=Validation.text(p.name(),"Product name",120);BigDecimal price=Validation.money(p.price(),"Price",true);
        if(price.compareTo(new BigDecimal("999999999.99"))>0) throw new AppException("Price cannot exceed 999,999,999.99.");
        if(p.category()==null || p.category().id()<=0) throw new AppException("Choose a category.");
        Validation.stock(p.quantity(),"Quantity");Validation.stock(p.minimumStock(),"Minimum stock");
        return new Product(p.id(),name,p.category(),price,p.quantity(),p.minimumStock(),p.active(),p.version(),p.createdAt(),p.updatedAt());
    }
    public Product create(AuthService.Session session,Product value) {
        Product p=validated(value);
        return db.transaction(c -> {
            User actor=auth.require(c,session,Permission.MANAGE_PRODUCTS);
            Product saved=products.insert(c,p);movements.record(c,saved.id(),"STOCK_IN",0,p.quantity(),actor.getId(),null,"Opening stock");return saved;
        });
    }
    public void update(AuthService.Session session,Product value) {
        Product p=validated(value);
        db.transaction(c -> { auth.require(c,session,Permission.MANAGE_PRODUCTS);products.update(c,p);return null; });
    }
    public void archive(AuthService.Session session,long id) {
        db.transaction(c -> { auth.require(c,session,Permission.MANAGE_PRODUCTS);products.delete(c,id);return null; });
    }
    public void seedDemo(AuthService.Session session) {
        db.transaction(c -> {
            User actor=auth.require(c,session,Permission.MANAGE_PRODUCTS);
            try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(716042003)")) { s.execute(); }
            try(var s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM products)");var r=s.executeQuery()) {
                r.next();if(r.getBoolean(1)) throw new AppException("Sample products can only be loaded into an empty catalog.");
            }
            var cats=categories.findAll(c);
            String[][] samples={
                {"Coca-Cola Original 330 ml","Beverages","25.00","48","10"},
                {"Absolute Drinking Water 500 ml","Beverages","15.00","72","15"},
                {"Alaska Fresh Milk 1 L","Beverages","95.00","6","10"},
                {"Gardenia Classic Bread","Food & pantry","85.00","8","10"},
                {"Lucky Me Pancit Canton","Food & pantry","18.50","60","12"},
                {"Argentina Corned Beef 150 g","Food & pantry","42.00","24","8"},
                {"Jasmine Rice 1 kg","Food & pantry","58.00","35","10"},
                {"Pilot Ballpen — Black","School supplies","12.00","40","10"},
                {"Spiral Notebook 80 leaves","School supplies","35.00","18","5"},
                {"USB-C Cable 1 m","Electronics","120.00","0","5"},
                {"Safeguard Pure White 135 g","Household","48.00","16","5"},
                {"Surf Powder Detergent 80 g","Household","8.50","90","20"}
            };
            for(String[] row:samples) {
                Category cat=cats.stream().filter(x -> x.name().equals(row[1])).findFirst().orElseThrow();
                Product p=products.insert(c,new Product(0,row[0],cat,new BigDecimal(row[2]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),true,0,null,null));
                movements.record(c,p.id(),"STOCK_IN",0,p.quantity(),actor.getId(),null,"Sample opening stock");
            }
            return null;
        });
    }
}
