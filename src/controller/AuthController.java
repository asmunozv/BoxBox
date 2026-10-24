package controller;

import model.service.AuthenticationService;

/** Bridges reusable authentication to the desktop session. */
public class AuthController {
    private final AuthenticationService service=new AuthenticationService();
    public boolean authenticate(String username,String password){return login(service.authenticate(username,password));}
    public boolean register(String username,String password){return login(service.register(username,password));}
    private boolean login(java.util.Optional<AuthenticationService.User> user){
        return user.map(u->{UserSession.login(u.id(),u.username());return true;}).orElse(false);
    }
}
