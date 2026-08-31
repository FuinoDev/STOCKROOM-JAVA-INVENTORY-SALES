package ph.stockroom.model;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
public final class Admin extends User {
    public Admin(long id,String username,String name,boolean active,Instant createdAt) { super(id,username,name,active,createdAt); }
    @Override public String getRole() { return "ADMIN"; }
    @Override public Set<Permission> getPermissions() { return Set.copyOf(EnumSet.allOf(Permission.class)); }
}
