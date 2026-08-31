package ph.stockroom.model;
import java.time.Instant;
import java.util.Set;
public final class Staff extends User {
    public Staff(long id,String username,String name,boolean active,Instant createdAt) { super(id,username,name,active,createdAt); }
    @Override public String getRole() { return "STAFF"; }
    @Override public Set<Permission> getPermissions() { return Set.of(Permission.VIEW_PRODUCTS,Permission.VIEW_INVENTORY,Permission.SELL,Permission.VIEW_OWN_SALES); }
}
