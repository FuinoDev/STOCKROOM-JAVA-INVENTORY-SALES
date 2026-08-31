package ph.stockroom.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
public final class Validation {
    public static final int MAX_STOCK=1_000_000;
    private Validation() { }
    public static String text(String value,String label,int max) {
        if(value==null || value.strip().isEmpty()) throw new AppException(label+" is required.");
        String result=value.strip();
        if(result.length()>max) throw new AppException(label+" must be "+max+" characters or fewer.");
        if(result.chars().anyMatch(Character::isISOControl)) throw new AppException(label+" cannot contain control characters.");
        return result;
    }
    public static String username(String value) {
        String result=text(value,"Username",50);
        if(!result.matches("[A-Za-z0-9._-]{3,50}")) throw new AppException("Username must be 3–50 letters, numbers, dots, dashes or underscores.");
        return result;
    }
    public static int stock(int value,String label) {
        if(value<0 || value>MAX_STOCK) throw new AppException(label+" must be between 0 and 1,000,000.");
        return value;
    }
    public static int integer(String value,String label) {
        try { return stock(Integer.parseInt(value.strip()),label); }
        catch(NumberFormatException e) { throw new AppException(label+" must be a whole number."); }
    }
    public static BigDecimal money(String value,String label,boolean positive) {
        try { return money(new BigDecimal(value.strip()),label,positive); }
        catch(NumberFormatException e) { throw new AppException("Please enter a valid "+label.toLowerCase()+" (for example, 125.50)."); }
    }
    public static BigDecimal money(BigDecimal value,String label,boolean positive) {
        if(value==null) throw new AppException(label+" is required.");
        BigDecimal result;
        try { result=value.setScale(2,RoundingMode.UNNECESSARY); }
        catch(ArithmeticException e) { throw new AppException(label+" can have at most two decimal places."); }
        if(result.signum()<0 || (positive && result.signum()==0)) throw new AppException(label+(positive?" must be greater than zero.":" cannot be negative."));
        if(result.compareTo(new BigDecimal("999999999999.99"))>0) throw new AppException(label+" is too large.");
        return result;
    }
    public static void password(char[] value) {
        if(value==null || value.length<10 || value.length>128) throw new AppException("Use a password between 10 and 128 characters.");
    }
}
