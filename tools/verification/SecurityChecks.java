package verification;

import model.security.PasswordHasher;
import java.util.UUID;

public final class SecurityChecks {
    public static void main(String[] args){
        PasswordHasher h=new PasswordHasher();String input=UUID.randomUUID()+"-"+UUID.randomUUID();String first=h.encode(input),second=h.encode(input);
        require(h.encoded(first)&&!first.equals(second));require(h.verify(input,first));require(!h.verify(UUID.randomUUID().toString(),first));require(!h.verify(null,first));require(!h.verify(input,"bb$pbkdf2$1$999999999$x$y"));require(!h.verify(input,"malformed"));require(h.legacyMatches(input,input));require(!h.legacyMatches(input,second));
        System.out.println("PASSWORD_SECURITY_CHECKS_PASSED: salted PBKDF2, valid/invalid input, malformed encodings and bounded work; no passwords printed");
    }
    static void require(boolean b){if(!b)throw new AssertionError("Password check failed");}
}
