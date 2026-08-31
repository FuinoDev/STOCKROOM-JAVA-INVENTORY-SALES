package ph.stockroom.service;
import org.junit.jupiter.api.Test;
import ph.stockroom.model.*;
import ph.stockroom.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class ValidationTest {
    @Test void validatesMoneyWithoutFloatingPointRounding() {
        assertEquals(new BigDecimal("125.50"),Validation.money("125.50","Price",true));
        assertEquals(new BigDecimal("0.30"),Validation.money("0.10","Price",true).add(Validation.money("0.20","Price",true)));
        assertThrows(AppException.class,() -> Validation.money("1.999","Price",true));
        assertThrows(AppException.class,() -> Validation.money("NaN","Price",true));
        assertThrows(AppException.class,() -> Validation.money("-1","Payment",false));
        assertThrows(AppException.class,() -> Validation.money("0","Price",true));
        assertThrows(AppException.class,() -> Validation.money("1000000000000","Payment",true));
    }
    @Test void validatesNamesQuantitiesAndPasswords() {
        assertEquals("Milk",Validation.text("  Milk  ","Name",120));
        assertThrows(AppException.class,() -> Validation.text(" ","Name",120));
        assertThrows(AppException.class,() -> Validation.text("bad\nname","Name",120));
        assertThrows(AppException.class,() -> Validation.integer("1.5","Quantity"));
        assertThrows(AppException.class,() -> Validation.integer("-1","Quantity"));
        assertThrows(AppException.class,() -> Validation.stock(1_000_001,"Stock"));
        assertThrows(AppException.class,() -> Validation.username("ab"));
        assertThrows(AppException.class,() -> Validation.username("admin' OR 1=1"));
        assertThrows(AppException.class,() -> Validation.password("short".toCharArray()));
    }
    @Test void saltsHashesAndVerifiesPasswords() {
        PasswordHasher hasher=new PasswordHasher();char[] secret="Testing-password-2026".toCharArray();
        String first=hasher.hash(secret),second=hasher.hash(secret);
        assertNotEquals(first,second);assertFalse(first.contains("Testing"));assertTrue(hasher.verify(secret,first));
        assertFalse(hasher.verify("incorrect-password".toCharArray(),first));
        assertFalse(hasher.verify(secret,"broken"));assertFalse(hasher.verify(secret,"pbkdf2-sha256$1$a$b"));
    }
    @Test void rolesDemonstratePolymorphicPermissions() {
        User admin=new Admin(1,"admin","Administrator",true,Instant.now()),staff=new Staff(2,"staff","Staff",true,Instant.now());
        assertTrue(admin.can(Permission.MANAGE_USERS));assertTrue(staff.can(Permission.SELL));assertFalse(staff.can(Permission.MANAGE_PRODUCTS));
        assertFalse(new Staff(3,"inactive","Inactive",false,Instant.now()).can(Permission.SELL));
        assertThrows(UnsupportedOperationException.class,() -> staff.getPermissions().add(Permission.MANAGE_USERS));
    }
    @Test void statusBoundariesAndValuationAreExact() {
        Product p=new Product(1,"Milk",new Category(1,"Food"),new BigDecimal("25.50"),10,10,true,0,null,null);
        assertEquals("Low stock",p.status());assertEquals(new BigDecimal("255.00"),p.inventoryValue());
        assertEquals("Out of stock",new Product(2,"Water",p.category(),p.price(),0,0,true,0,null,null).status());
        assertEquals("In stock",new Product(3,"Bread",p.category(),p.price(),11,10,true,0,null,null).status());
    }
    @Test void csvEscapesQuotesAndFormulaPrefixes() {
        assertEquals("\"' =HYPERLINK(\"\"bad\"\")\"",CsvExporter.cell(" =HYPERLINK(\"bad\")"));
        assertEquals("\"Milk, 1 L\"",CsvExporter.cell("Milk, 1 L"));
        assertEquals("\"-3\"",CsvExporter.cell(-3));
        assertEquals("\"'@SUM(A1)\"",CsvExporter.cell("@SUM(A1)"));
    }
    @Test void formatsPesoAmountsAndRejectsInvalidDates() {
        assertEquals("₱1,250.50",Formats.currency(new BigDecimal("1250.50")));
        assertThrows(AppException.class,() -> Formats.parseDate("2026-02-30"));
    }
}
