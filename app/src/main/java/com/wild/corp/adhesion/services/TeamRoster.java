package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.utils.Status;
import java.time.LocalDate;
import java.util.*;

/** Shared population rules for generation, selection and attendance. */
public final class TeamRoster {
    private TeamRoster() { }
    public static String contactAddress(Adherent person) {
        Adherent contact = Boolean.TRUE.equals(person.getEmailRepresentant()) && person.getRepresentant() != null ? person.getRepresentant() : person;
        return contact.getUser() == null ? "" : contact.getUser().getUsername().trim().toLowerCase(Locale.ROOT);
    }
    public static boolean eligible(Adhesion a, Activite activity) {
        return a != null && a.getAdherent() != null
                && !List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label).contains(a.getStatutActuel() == null ? "" : a.getStatutActuel())
                && (sameActivity(a.getActivite(), activity) || sameActivity(a.getSurClassement(), activity));
    }
    private static boolean sameActivity(Activite a, Activite b) {
        return a != null && b != null && (a == b || (a.getId() != null && a.getId().equals(b.getId())));
    }
    public static boolean active(AffectationEquipe a, LocalDate date) {
        return !a.getDebut().isAfter(date) && (a.getFin() == null || a.getFin().isAfter(date));
    }
    public static List<Adhesion> participants(Seance session) {
        Map<Long, Adhesion> people = new LinkedHashMap<>();
        LocalDate date = session.getDebut().toLocalDate();
        session.getEquipes().stream().filter(t -> !t.isArchivee()).forEach(team ->
                team.getAffectations().stream().filter(a -> active(a, date))
                    .map(AffectationEquipe::getAdhesion).filter(a -> eligible(a, session.getActivite()))
                    .sorted(Comparator.comparing(Adhesion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(a -> people.putIfAbsent(a.getAdherent().getId(), a)));
        return List.copyOf(people.values());
    }
}
