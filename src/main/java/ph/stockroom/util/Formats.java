package ph.stockroom.util;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
public final class Formats {
    private Formats() { }
    public static String money(BigDecimal value) { return "₱"+NumberFormat.getNumberInstance(Locale.US).format(value.setScale(2))+(value.stripTrailingZeros().scale()<=0?".00":value.stripTrailingZeros().scale()==1?"0":""); }
    public static String currency(BigDecimal value) { return "₱"+String.format(Locale.US,"%,.2f",value); }
    public static String number(long value) { return String.format(Locale.US,"%,d",value); }
    public static String date(Instant value,ZoneId zone) { return DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a",Locale.ENGLISH).withZone(zone).format(value); }
    public static LocalDate parseDate(String value) {
        try { return LocalDate.parse(value.strip()); } catch(Exception e) { throw new ph.stockroom.service.AppException("Enter dates as YYYY-MM-DD (for example, 2026-08-31)."); }
    }
}
