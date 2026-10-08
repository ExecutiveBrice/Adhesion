package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Paiement;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record ReportingAdhesionExport(
        String nomActivite,
        String nomAdherent,
        String prenomAdherent,
        String emailAdherent,
        String statutActuel,
        LocalDate dateReglement1,
        LocalDate dateReglement2,
        LocalDate dateReglement3,
        Integer paiement1,
        Integer paiement2,
        Integer paiement3,
        String typeReglement1,
        String typeReglement2,
        String typeReglement3,
        String remarqueSecretariat
) {
    public static ReportingAdhesionExport from(Adhesion adhesion) {
        Adherent adherent = adhesion.getAdherent();
        Adherent destinataire = Boolean.TRUE.equals(adherent.getEmailRepresentant()) && adherent.getRepresentant() != null
                ? adherent.getRepresentant() : adherent;
        String email = destinataire.getUser() != null ? destinataire.getUser().getUsername() : null;
        List<Paiement> paiements = adhesion.getPaiements().stream()
                .sorted(Comparator.comparing(Paiement::getDateReglement, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Paiement::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Paiement premier = paiement(paiements, 0);
        Paiement deuxieme = paiement(paiements, 1);
        Paiement troisieme = paiement(paiements, 2);

        return new ReportingAdhesionExport(
                adhesion.getActivite().getNom(), adherent.getNom(), adherent.getPrenom(), email,
                adhesion.getStatutActuel(),
                date(premier), date(deuxieme), date(troisieme),
                montant(premier), montant(deuxieme), montant(troisieme),
                type(premier), type(deuxieme), type(troisieme),
                adhesion.getRemarqueSecretariat());
    }

    private static Paiement paiement(List<Paiement> paiements, int index) {
        return index < paiements.size() ? paiements.get(index) : null;
    }

    private static LocalDate date(Paiement paiement) {
        return paiement != null ? paiement.getDateReglement() : null;
    }

    private static Integer montant(Paiement paiement) {
        return paiement != null ? paiement.getMontant() : null;
    }

    private static String type(Paiement paiement) {
        return paiement != null ? paiement.getTypeReglement() : null;
    }
}
