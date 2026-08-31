package ph.stockroom.model;
import java.time.Instant;
import java.util.Set;

/** Encapsulation and abstraction: callers ask what a user can do, not how roles work. */
public abstract sealed class User permits Admin, Staff {
    private final long id;
    private final String username;
    private final String displayName;
    private final boolean active;
    private final Instant createdAt;
    protected User(long id,String username,String displayName,boolean active,Instant createdAt) {
        this.id=id; this.username=username; this.displayName=displayName; this.active=active; this.createdAt=createdAt;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public abstract String getRole();
    public abstract Set<Permission> getPermissions();
    public final boolean can(Permission permission) { return active && getPermissions().contains(permission); }
    @Override public String toString() { return displayName; }
}
