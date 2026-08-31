package ph.stockroom.service;
import ph.stockroom.dao.UserDAO;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.model.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class AuthService {
    public record Session(UUID token) { }
    private record Login(long userId,Instant expires,int version) { }
    private record Attempts(int count,Instant since) { }
    private final DatabaseConnection db;
    private final UserDAO users=new UserDAO();
    private final PasswordHasher hasher=new PasswordHasher();
    private final Map<UUID,Login> sessions=new ConcurrentHashMap<>();
    private final Map<String,Attempts> attempts=new HashMap<>();
    private final String dummyHash=hasher.hash("Timing-padding-only".toCharArray());
    public AuthService(DatabaseConnection db) { this.db=db; }
    public boolean needsSetup() { return db.read(users::isEmpty); }
    public Session bootstrap(String username,String displayName,char[] password) {
        String login=Validation.username(username), name=Validation.text(displayName,"Display name",100), hash=hasher.hash(password);
        User u=db.transaction(c -> {
            try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(716042001)")) { s.execute(); }
            if(!users.isEmpty(c)) throw new AppException("An administrator already exists. Sign in instead.");
            return users.insert(c,login,name,hash,"ADMIN");
        });
        return session(u,0);
    }
    public synchronized Session login(String username,char[] password) {
        String name=Validation.text(username,"Username",50).toLowerCase(Locale.ROOT);
        if(password==null || password.length==0) throw new AppException("Password is required.");
        Instant now=Instant.now();
        attempts.entrySet().removeIf(e -> e.getValue().since().plusSeconds(600).isBefore(now));
        Attempts a=attempts.get(name);
        if(a!=null && a.count()>=5) throw new AppException("Too many unsuccessful attempts. Please wait 10 minutes before trying this username again.");
        var credentials=db.read(c -> users.credentials(c,name));
        boolean valid=hasher.verify(password,credentials.map(UserDAO.Credentials::hash).orElse(dummyHash));
        if(!valid || credentials.isEmpty() || !credentials.get().user().isActive()) {
            attempts.put(name,new Attempts(a==null?1:a.count()+1,a==null?now:a.since()));
            throw new AppException("Incorrect username or password, or the account is inactive.");
        }
        attempts.remove(name);
        return session(credentials.get().user(),credentials.get().sessionVersion());
    }
    private Session session(User u,int version) {
        sessions.entrySet().removeIf(e -> e.getValue().expires().isBefore(Instant.now()));
        Session session=new Session(UUID.randomUUID());sessions.put(session.token(),new Login(u.getId(),Instant.now().plusSeconds(28800),version));return session;
    }
    public User current(Session session) { return db.read(c -> current(c,session)); }
    public User current(Connection c,Session session) throws SQLException {
        Login login=session==null?null:sessions.get(session.token());
        if(login==null || login.expires().isBefore(Instant.now())) {
            logout(session);throw new AppException("Your session has ended. Please sign in again.");
        }
        User user=users.findById(c,login.userId()).orElseThrow(() -> new AppException("This account no longer exists."));
        if(!user.isActive()) { logout(session);throw new AppException("This account is inactive. Please contact an administrator."); }
        int version=users.credentials(c,user.getUsername()).orElseThrow().sessionVersion();
        if(version!=login.version()) { logout(session);throw new AppException("Your password or account access changed. Please sign in again."); }
        return user;
    }
    public User require(Connection c,Session session,Permission permission) throws SQLException {
        User user=current(c,session);
        if(!user.can(permission)) throw new AppException("Your account does not have permission to perform this action.");
        return user;
    }
    public void logout(Session session) { if(session!=null) sessions.remove(session.token()); }
    private void revoke(long userId) { sessions.entrySet().removeIf(e -> e.getValue().userId()==userId); }
    public List<User> listUsers(Session session) { return db.read(c -> { require(c,session,Permission.MANAGE_USERS);return users.findAll(c); }); }
    public User createUser(Session session,String username,String displayName,char[] password,String role) {
        String login=Validation.username(username),name=Validation.text(displayName,"Display name",100);
        if(!Set.of("ADMIN","STAFF").contains(role)) throw new AppException("Choose ADMIN or STAFF.");
        current(session);
        String hash=hasher.hash(password);
        return db.transaction(c -> { require(c,session,Permission.MANAGE_USERS);return users.insert(c,login,name,hash,role); });
    }
    public void setActive(Session session,long id,boolean active) {
        db.transaction(c -> {
            User actor=require(c,session,Permission.MANAGE_USERS);
            if(actor.getId()==id) throw new AppException("You cannot deactivate your own account.");
            User target=users.findById(c,id).orElseThrow(() -> new AppException("User not found."));
            if(target.getRole().equals("ADMIN")) {
                try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(716042002)")) { s.execute(); }
                // Serialize admin deactivation so simultaneous requests cannot remove every administrator.
                long admins=users.findAll(c).stream().filter(u -> u.isActive() && u.getRole().equals("ADMIN")).count();
                if(!active && target.isActive() && admins<=1) throw new AppException("At least one active administrator is required.");
            }
            users.setActive(c,id,active);return null;
        });
        if(!active) revoke(id);
    }
    public void resetPassword(Session session,long id,char[] password) {
        String hash=hasher.hash(password);
        db.transaction(c -> { require(c,session,Permission.MANAGE_USERS);users.findById(c,id).orElseThrow(() -> new AppException("User not found."));users.setPassword(c,id,hash);return null; });
        revoke(id);
    }
    public void changePassword(Session session,char[] oldPassword,char[] newPassword) {
        String hash=hasher.hash(newPassword);
        long userId=db.transaction(c -> {
            User u=current(c,session);
            var credentials=users.credentials(c,u.getUsername()).orElseThrow();
            if(!hasher.verify(oldPassword,credentials.hash())) throw new AppException("Your current password is incorrect.");
            users.setPassword(c,u.getId(),hash);return u.getId();
        });
        sessions.entrySet().removeIf(e -> e.getValue().userId()==userId && !e.getKey().equals(session.token()));
        sessions.computeIfPresent(session.token(),(key,value) -> new Login(value.userId(),value.expires(),value.version()+1));
    }
}
