package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ResponsabiliteSeance;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse;
import com.wild.corp.adhesion.models.resources.ResponsabiliteSeanceRequest;
import com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest;
import com.wild.corp.adhesion.models.Equipe;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.AffectationEquipe;
import com.wild.corp.adhesion.repository.EquipeRepository;
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
import java.util.Comparator;
import java.time.LocalDate;

@Service
@Transactional
public class ResponsabiliteSeanceServices {
    private static final List<String> STATUTS_EXCLUS = List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label);
    private final AdherentRepository adherents;
    private final TacheSeanceRepository taches;
    private final ActiviteRepository activites;
    private final EquipeRepository equipes;

    public ResponsabiliteSeanceServices(AdherentRepository adherents, TacheSeanceRepository taches,
                                        ActiviteRepository activites, EquipeRepository equipes) {
        this.adherents = adherents;
        this.taches = taches;
        this.activites = activites;
        this.equipes = equipes;
    }

    public record EquipeCandidate(Long id, String nom, String activiteNom, AdherentResponsabiliteResponse attributaire) { }

    public List<EquipeCandidate> getEquipes(Seance seance) {
        var section = seance.getActivite().getSection();
        if (section == null) return List.of();
        return equipes.findByActivite_Section_IdAndArchiveeFalseOrderByActivite_NomAscNomAsc(section.getId())
                .stream().map(equipe -> {
                    var attributaire = attributaire(equipe);
                    return new EquipeCandidate(equipe.getId(), equipe.getNom(), equipe.getActivite().getNom(),
                            attributaire == null ? null : AdherentResponsabiliteResponse.from(attributaire));
                }).toList();
    }

    // Sets have no defined order: the lowest persisted identifier defines the first person.
    private Adherent attributaire(Equipe equipe) {
        return equipe.getReferents().stream().min(Comparator.comparing(Adherent::getId))
                .orElseGet(() -> equipe.getAffectations().stream()
                        .filter(a -> TeamRoster.active(a, LocalDate.now()))
                        .map(AffectationEquipe::getAdhesion).filter(a -> TeamRoster.eligible(a, equipe.getActivite()))
                        .map(Adhesion::getAdherent).min(Comparator.comparing(Adherent::getId)).orElse(null));
    }

    public void remplacerParEquipes(Seance seance, List<ResponsabiliteEquipeRequest> requests) {
        if (requests == null) throw invalide("Liste de responsabilités obligatoire");
        var section = seance.getActivite().getSection();
        var nouvelles = new ArrayList<ResponsabiliteSeance>();
        var uniques = new HashSet<ResponsabiliteEquipeRequest>();
        for (var request : requests) {
            if (request == null || request.tacheId() == null || request.tacheId() <= 0
                    || request.equipeId() == null || request.equipeId() <= 0 || !uniques.add(request)) {
                throw invalide("Choisissez une responsabilité et une équipe pour chaque ligne, sans doublon");
            }
            var tache = taches.findById(request.tacheId()).orElseThrow(() -> invalide("La responsabilité sélectionnée n'existe plus"));
            var equipe = equipes.findById(request.equipeId()).orElseThrow(() -> invalide("L'équipe sélectionnée n'existe plus"));
            if (section == null || equipe.getActivite().getSection() == null
                    || !section.getId().equals(equipe.getActivite().getSection().getId())) {
                throw invalide("L'équipe doit appartenir à la section de l’événement");
            }
            if (equipe.isArchivee()) throw invalide("L'équipe sélectionnée est archivée");
            var adherent = attributaire(equipe);
            if (adherent == null) throw invalide("L'équipe « " + equipe.getNom() + " » n'a aucun référent ni adhérent");
            var responsabilite = new ResponsabiliteSeance(tache, adherent);
            responsabilite.setEquipe(equipe);
            nouvelles.add(responsabilite);
        }
        seance.getResponsabilites().clear();
        seance.getResponsabilites().addAll(nouvelles);
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
                    .orElseThrow(() -> invalide("L'adhérent doit appartenir à la section de l’événement"));
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
