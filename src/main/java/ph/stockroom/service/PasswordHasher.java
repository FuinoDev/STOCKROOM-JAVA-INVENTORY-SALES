package ph.stockroom.service;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;
/** Per-password salt and adaptive PBKDF2 hashing; no plaintext passwords are stored. */
public final class PasswordHasher {
    private static final int ITERATIONS=600_000;
    private final SecureRandom random=new SecureRandom();
    public String hash(char[] password) {
        Validation.password(password);
        byte[] salt=new byte[16];random.nextBytes(salt);
        byte[] key=derive(password,salt,ITERATIONS);
        return "pbkdf2-sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(key);
    }
    public boolean verify(char[] password,String encoded) {
        if(password==null || password.length>128) return false;
        try {
            String[] parts=encoded.split("\\$");
            if(parts.length!=4 || !parts[0].equals("pbkdf2-sha256")) return false;
            int iterations=Integer.parseInt(parts[1]);
            if(iterations<100_000 || iterations>2_000_000) return false;
            byte[] salt=Base64.getDecoder().decode(parts[2]), expected=Base64.getDecoder().decode(parts[3]);
            if(salt.length!=16 || expected.length!=32) return false;
            return MessageDigest.isEqual(expected,derive(password,salt,iterations));
        } catch(IllegalArgumentException e) { return false; }
    }
    private byte[] derive(char[] password,byte[] salt,int iterations) {
        PBEKeySpec spec=new PBEKeySpec(password,salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException e) { throw new IllegalStateException("Password hashing is unavailable.",e); }
        finally { spec.clearPassword(); }
    }
}
