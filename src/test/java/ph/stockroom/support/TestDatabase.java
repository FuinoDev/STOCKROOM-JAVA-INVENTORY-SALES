package ph.stockroom.support;
import ph.stockroom.config.AppConfig;
import ph.stockroom.database.DatabaseConnection;
import ph.stockroom.service.AppServices;
import java.time.ZoneId;
import java.util.UUID;
/** Allocates a unique schema only in explicitly named *_test databases. Never reads production credentials. */
public final class TestDatabase implements AutoCloseable {
    private final DatabaseConnection base;private final String schema;
    public final AppServices app;
    public TestDatabase() {
        String url=System.getenv("TEST_DB_URL");
        if(url==null || !url.split("\\?",2)[0].matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test"))throw new IllegalStateException("TEST_DB_URL must point to a dedicated database whose name ends in _test.");
        String username=System.getenv("TEST_DB_USERNAME"),password=System.getenv("TEST_DB_PASSWORD");
        if(username==null || password==null)throw new IllegalStateException("Set TEST_DB_USERNAME and TEST_DB_PASSWORD.");
        AppConfig config=new AppConfig(url,username,password,"Stockroom",ZoneId.of("Asia/Manila"));base=new DatabaseConnection(config);
        schema="stockroom_test_"+UUID.randomUUID().toString().replace("-","");
        base.read(c -> {try(var s=c.createStatement()){s.execute("CREATE SCHEMA "+schema);}return null;});
        app=new AppServices(new AppConfig(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,username,password,"Stockroom",ZoneId.of("Asia/Manila")));
        app.database.initialize();
    }
    @Override public void close() {
        if(!schema.matches("stockroom_test_[a-f0-9]{32}"))throw new IllegalStateException("Unsafe test schema.");
        base.read(c -> {try(var s=c.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}return null;});
    }
}
