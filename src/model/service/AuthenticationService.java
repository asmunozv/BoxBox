package model.service;

import java.util.Optional;
import model.dao.*;
import model.security.PasswordHasher;

/** Reusable authentication policy; no JavaFX/session state and no in-place legacy migration. */
public final class AuthenticationService {
    public record User(int id,String username){}
    private final UserAccountDAO users=new UserAccountDAO();
    private final PasswordHashDAO hashes=new PasswordHashDAO();
    private final PasswordHasher hasher=new PasswordHasher();
    public Optional<User> authenticate(String username,String password){
        if(username==null||password==null||username.isBlank()||password.isBlank()||username.trim().length()>80||password.length()>1024)return Optional.empty();
        return users.findByUsername(username.trim()).filter(user->{
            if(hasher.encoded(user.getPassword()))return hasher.verify(password,user.getPassword());
            var protectedHash=hashes.find(user.getIdUser());
            if(protectedHash.isPresent())return hasher.verify(password,protectedHash.get());
            if(!hasher.legacyMatches(password,user.getPassword()))return false;
            hashes.append(user.getIdUser(),hasher.encode(password));return true;
        }).map(user->new User(user.getIdUser(),user.getUsername()));
    }
    public Optional<User> register(String username,String password){
        if(username==null||password==null||username.trim().isEmpty()||username.trim().length()>80||password.length()<8||password.length()>1024)return Optional.empty();
        String name=username.trim();if(users.findByUsername(name).isPresent())return Optional.empty();
        return Optional.of(new User(users.createUser(name,hasher.encode(password)),name));
    }
}
