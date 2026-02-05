package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.security.jwt.SurrogateAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class SurrogateService {

    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public SurrogateService(UserDetailsService userDetailsService, AuthenticationManager authenticationManager) {
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    public Authentication impersonate(Authentication currentAuth, String targetUsername) {
        UserDetails targetUser = userDetailsService.loadUserByUsername(targetUsername);

        if (currentAuth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new SecurityException("Seuls les admins peuvent faire de la substitution !");
        }

        return new SurrogateAuthenticationToken(currentAuth.getPrincipal(), targetUser, targetUser.getAuthorities());
    }
}
