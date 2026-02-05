package com.wild.corp.adhesion.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class SurrogateAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;       // l'utilisateur impersonné
    private final Object realPrincipal;   // l'utilisateur réel (admin)

    public SurrogateAuthenticationToken(Object realPrincipal, Object principal,
                                        Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.realPrincipal = realPrincipal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Object getRealPrincipal() {
        return realPrincipal;
    }
}
