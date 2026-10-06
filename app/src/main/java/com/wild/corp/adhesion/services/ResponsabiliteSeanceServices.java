package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ResponsabiliteSeance;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse;
import com.wild.corp.adhesion.models.resources.ResponsabiliteSeanceRequest;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.TacheSeanceRepository;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class ResponsabiliteSeanceServices {
    private static final List<String> STATUTS_EXCLUS = List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label);
    private final AdherentRepository adherents;
    private final TacheSeanceRepository taches;
    private final ActiviteRepository activites;

    public ResponsabiliteSeanceServices(AdherentRepository adherents, TacheSeanceRepository taches,
                                        ActiviteRepository activites) {
        this.adherents = adherents;
        this.taches = taches;
        this.activites = activites;
    }

    public List<AdherentResponsabiliteResponse> getCandidats(Long activiteId) {
        var activite = activites.findById(activiteId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
        if (activite.getSection() == null) return List.of();
        return adherents.findResponsabiliteCandidates(activite.getSection().getId(), STATUTS_EXCLUS)
                .stream().map(AdherentResponsabiliteResponse::from).toList();
    }

    public void remplacer(Seance seance, List<ResponsabiliteSeanceRequest> requests) {
        if (requests == null) return;
        var section = seance.getActivite().getSection();
        var nouvelles = new ArrayList<ResponsabiliteSeance>();
        var uniques = new HashSet<ResponsabiliteSeanceRequest>();
        for (var request : requests) {
            if (request == null || request.tacheId() == null || request.tacheId() <= 0
                    || request.adherentId() == null || request.adherentId() <= 0 || !uniques.add(request)) {
                throw invalide("Chaque responsabilité doit associer une tâche et un adhérent, sans doublon");
            }
            var tache = taches.findById(request.tacheId()).orElseThrow(() ->
                    invalide("La tâche sélectionnée n'existe plus"));
            if (section == null) throw invalide("L'activité doit être rattachée à une section");
            var adherent = adherents.findResponsabiliteCandidate(request.adherentId(), section.getId(), STATUTS_EXCLUS)
                    .orElseThrow(() -> invalide("L'adhérent doit appartenir à la section de la séance"));
            nouvelles.add(new ResponsabiliteSeance(tache, adherent));
        }
        // Validate the entire list before changing the managed collection.
        seance.getResponsabilites().clear();
        seance.getResponsabilites().addAll(nouvelles);
    }

    private ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
