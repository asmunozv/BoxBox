package model.security;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Versioned PBKDF2-HMAC-SHA256; no external crypto dependencies. */
public final class PasswordHasher {
    private static final int ITERATIONS=600_000;
    private static final String PREFIX="bb$pbkdf2$1$";
    public boolean encoded(String stored){return stored!=null&&stored.startsWith(PREFIX);}
    public String encode(String password){
        if(password==null||password.isEmpty()||password.length()>1024)throw new IllegalArgumentException("Contraseña fuera del tamaño admitido.");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);byte[] key=derive(password,salt,ITERATIONS);
        return PREFIX+ITERATIONS+"$"+Base64.getUrlEncoder().withoutPadding().encodeToString(salt)+"$"+Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }
    public boolean verify(String password,String stored){
        if(password==null||password.length()>1024||!encoded(stored))return false;
        try{String[] parts=stored.split("\\$");if(parts.length!=6)return false;int iterations=Integer.parseInt(parts[3]);if(iterations<10_000||iterations>2_000_000)return false;byte[] salt=Base64.getUrlDecoder().decode(parts[4]),key=Base64.getUrlDecoder().decode(parts[5]);if(salt.length<16||key.length!=32)return false;return MessageDigest.isEqual(key,derive(password,salt,iterations));}catch(IllegalArgumentException e){return false;}
    }
    public boolean legacyMatches(String password,String stored){return password!=null&&stored!=null&&MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8),stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private byte[] derive(String password,byte[] salt,int iterations){char[] chars=password.toCharArray();PBEKeySpec spec=new PBEKeySpec(chars,salt,iterations,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}catch(GeneralSecurityException e){throw new IllegalStateException("No se pudo verificar el acceso.",e);}finally{spec.clearPassword();Arrays.fill(chars,'\0');}}
}
