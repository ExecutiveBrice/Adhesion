package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

/** The first active team is the default destination for an activity's registrations. */
@Service @Transactional
public class ActivityTeamService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final EquipeRepository teams;
    private final ActiviteRepository activities;
    private final AdhesionRepository memberships;
    private final ChatRepository chats;
    public ActivityTeamService(EquipeRepository teams, ActiviteRepository activities, AdhesionRepository memberships,
                               ChatRepository chats) {
        this.teams = teams; this.activities = activities; this.memberships = memberships; this.chats = chats;
    }
    public Equipe ensureFirstTeam(Activite activity) {
        activities.findForTeamUpdate(activity.getId()).orElseThrow();
        List<Equipe> existing = teams.findByActivite_IdOrderByIdAsc(activity.getId());
        Equipe first = existing.stream().filter(t -> !t.isArchivee()).findFirst().orElse(null);
        if (first != null) {
            ensureTeamChat(first);
            return first;
        }
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); existing.forEach(t -> names.add(t.getNom()));
        int suffix = existing.size() + 1;
        String name = "Groupe " + suffix;
        while (names.contains(name)) name = "Groupe " + ++suffix;
        first = new Equipe(); first.setActivite(activity); first.setNom(name);
        first.getEncadrants().addAll(activity.getProfs()); first.getReferents().addAll(activity.getReferents());
        teams.saveAndFlush(first);
        ensureTeamChat(first);
        return first;
    }
    /** Called with the activity lock held, so concurrent team updates cannot create duplicate chats. */
    public void ensureTeamChat(Equipe team) {
        Chat chat = chats.findByEquipeId(team.getId()).orElseGet(() -> {
            Chat created = new Chat(); created.setCible(ChatTarget.EQUIPE); created.setEquipe(team);
            return created;
        });
        chat.setNom(team.getNom());
        chats.save(chat);
    }
    public void assignNewMembership(Adhesion input) {
        if (input == null || input.getId() == null) return;
        Adhesion membership = memberships.findById(input.getId()).orElseThrow();
        assign(membership, membership.getActivite());
        if (membership.getSurClassement() != null) assign(membership, membership.getSurClassement());
    }
    public Adhesion saveMembership(Adhesion input) {
        Adhesion saved = memberships.saveAndFlush(input);
        assignNewMembership(saved);
        return saved;
    }
    public void deleteMembership(Long id) {
        Adhesion membership = memberships.findById(id).orElseThrow();
        activities.findForTeamUpdate(membership.getActivite().getId()).orElseThrow();
        // Detach the inverse collections so their cascades cannot restore a deleted registration.
        for (AffectationEquipe assignment : entityManager.createQuery("select a from AffectationEquipe a where a.adhesion.id = :id", AffectationEquipe.class).setParameter("id", id).getResultList()) {
            assignment.getEquipe().getAffectations().remove(assignment); entityManager.remove(assignment);
        }
        for (ConvocationEquipe invitation : entityManager.createQuery("select c from ConvocationEquipe c where c.adhesion.id = :id", ConvocationEquipe.class).setParameter("id", id).getResultList()) {
            invitation.getSeance().getConvocationsEquipe().remove(invitation); entityManager.remove(invitation);
        }
        for (Presence presence : entityManager.createQuery("select p from Presence p where p.adhesion.id = :id", Presence.class).setParameter("id", id).getResultList()) {
            presence.getSeance().getPresences().remove(presence);
            membership.getPresences().remove(presence); entityManager.remove(presence);
        }
        membership.getAdherent().getAdhesions().remove(membership);
        memberships.delete(membership);
    }
    private void assign(Adhesion membership, Activite activity) {
        Equipe first = ensureFirstTeam(activity);
        // A manual redistribution must not be undone by a later file/status update.
        boolean previouslyAssigned = teams.findByActivite_IdOrderByIdAsc(activity.getId()).stream()
                .flatMap(t -> t.getAffectations().stream())
                .anyMatch(a -> a.getAdhesion().getAdherent().getId().equals(membership.getAdherent().getId()));
        if (previouslyAssigned) return;
        AffectationEquipe assignment = new AffectationEquipe(); assignment.setEquipe(first);
        assignment.setAdhesion(membership); assignment.setDebut(LocalDate.now());
        first.getAffectations().add(assignment); teams.save(first);
    }
}
