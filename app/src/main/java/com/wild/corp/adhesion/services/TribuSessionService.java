package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.security.payload.response.JwtResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TribuSessionService {
    private final UserRepository users;
    private final AdherentRepository adherents;
    private final JwtUtils jwtUtils;
    private final PwaSessionService pwaSessions;

    public TribuSessionService(UserRepository users, AdherentRepository adherents,
                              JwtUtils jwtUtils, PwaSessionService pwaSessions) {
        this.users = users;
        this.adherents = adherents;
        this.jwtUtils = jwtUtils;
        this.pwaSessions = pwaSessions;
    }

    @Transactional
    public JwtResponse switchMember(UserDetails actor, Long adherentId, boolean rememberSession) {
        User current = users.findById(actor.getId()).orElseThrow(TribuSessionService::forbidden);
        Adherent source = current.getAdherent();
        Adherent target = adherents.findById(adherentId).orElseThrow(TribuSessionService::forbidden);
        if (current.getSessionVersion() != actor.getSessionVersion()
                || source == null || source.getTribu() == null || source.getTribu().getId() == null
                || target.getTribu() == null
                || !source.getTribu().getId().equals(target.getTribu().getId())) {
            throw forbidden();
        }
        if (target.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet adhérent ne possède pas de compte de connexion");
        }
        UserDetails principal = UserDetails.build(target.getUser());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        JwtResponse response = new JwtResponse(jwtUtils.generateJwtToken(authentication), principal.getId(),
                principal.getUsername(), principal.isEnabled(),
                principal.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
        if (rememberSession) response.setRefreshToken(pwaSessions.issue(principal));
        return response;
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Le changement est limité aux adhérents de votre tribu");
    }
}
