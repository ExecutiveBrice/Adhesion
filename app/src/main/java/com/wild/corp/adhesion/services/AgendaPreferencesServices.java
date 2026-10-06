package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.resources.AgendaPreferences;
import com.wild.corp.adhesion.repository.AdherentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AgendaPreferencesServices {
    private final AdherentRepository adherents;

    @Transactional(readOnly = true)
    public AgendaPreferences get(String username) {
        return preferences(adherentConnecte(username));
    }

    public AgendaPreferences update(String username, AgendaPreferences preferences) {
        var adherent = adherentConnecte(username);
        adherent.getAgendaSectionsMasquees().clear();
        adherent.getAgendaSectionsMasquees().addAll(preferences.sectionsMasquees());
        adherent.getAgendaGoogleMasques().clear();
        adherent.getAgendaGoogleMasques().addAll(preferences.agendasGoogleMasques());
        adherents.save(adherent);
        return preferences(adherent);
    }

    private Adherent adherentConnecte(String username) {
        return adherents.findByUserUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun adhérent associé au compte connecté."));
    }

    private AgendaPreferences preferences(Adherent adherent) {
        return new AgendaPreferences(Set.copyOf(adherent.getAgendaSectionsMasquees()),
                Set.copyOf(adherent.getAgendaGoogleMasques()));
    }
}
