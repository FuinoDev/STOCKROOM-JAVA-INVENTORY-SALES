package ph.stockroom.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Properties;

public record AppConfig(String databaseUrl, String databaseUser, String databasePassword,
                        String businessName, ZoneId zone) {
    public static AppConfig load() throws IOException {
        Properties p = new Properties();
        Path file = Path.of(System.getProperty("stockroom.config", "config/local.properties"));
        if (Files.exists(file)) try (var reader = Files.newBufferedReader(file)) { p.load(reader); }
        return new AppConfig(value(p,"DB_URL","db.url","jdbc:postgresql://127.0.0.1:55432/inventory_sales"),
            value(p,"DB_USERNAME","db.username","inventory_app"), value(p,"DB_PASSWORD","db.password",""),
            value(p,"BUSINESS_NAME","business.name","Stockroom"),
            ZoneId.of(value(p,"BUSINESS_TIMEZONE","business.timezone","Asia/Manila")));
    }
    private static String value(Properties p, String env, String key, String fallback) {
        String v = System.getenv(env);
        return v == null ? p.getProperty(key, fallback) : v;
    }
    @Override public String toString() { return "AppConfig[credentials redacted, businessName=" + businessName + "]"; }
}
