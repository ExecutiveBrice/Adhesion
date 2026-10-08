package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Paiement;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.resources.ReportingAdhesionExport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
class ReportingAdhesionExportTest {

    @Test
    void exportsOneRowPerAdhesionWithPaymentsInDateOrder() {
        Adherent adherent = new Adherent();
        adherent.setNom("Martin");
        adherent.setPrenom("Alice");
        User user = new User();
        user.setUsername("alice@example.test");
        adherent.setUser(user);

        Activite activite = new Activite();
        activite.setNom("Basket");
        Adhesion adhesion = new Adhesion();
        adhesion.setAdherent(adherent);
        adhesion.setActivite(activite);
        adhesion.setStatutActuel("Validée");
        adhesion.setRemarqueSecretariat("À vérifier");
        adhesion.setPaiements(List.of(
                paiement(2L, LocalDate.of(2026, 9, 10), 25, "Chèque"),
                paiement(1L, LocalDate.of(2026, 8, 10), 50, "CB")
        ));
        var ligne = ReportingAdhesionExport.from(adhesion);

        assertThat(ligne.nomActivite()).isEqualTo("Basket");
        assertThat(ligne.nomAdherent()).isEqualTo("Martin");
        assertThat(ligne.prenomAdherent()).isEqualTo("Alice");
        assertThat(ligne.emailAdherent()).isEqualTo("alice@example.test");
        assertThat(ligne.statutActuel()).isEqualTo("Validée");
        assertThat(ligne.dateReglement1()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(ligne.paiement1()).isEqualTo(50);
        assertThat(ligne.typeReglement1()).isEqualTo("CB");
        assertThat(ligne.dateReglement2()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(ligne.paiement2()).isEqualTo(25);
        assertThat(ligne.typeReglement2()).isEqualTo("Chèque");
        assertThat(ligne.dateReglement3()).isNull();
        assertThat(ligne.paiement3()).isNull();
        assertThat(ligne.typeReglement3()).isNull();
        assertThat(ligne.remarqueSecretariat()).isEqualTo("À vérifier");
    }

    @Test
    void usesRepresentativesEmailWhenRequested() {
        Adherent adherent = new Adherent();
        adherent.setEmailRepresentant(true);
        Adherent representant = new Adherent();
        User user = new User();
        user.setUsername("parent@example.test");
        representant.setUser(user);
        adherent.setRepresentant(representant);
        Adhesion adhesion = new Adhesion();
        adhesion.setAdherent(adherent);
        Activite activite = new Activite();
        activite.setNom("Danse");
        adhesion.setActivite(activite);
        assertThat(ReportingAdhesionExport.from(adhesion).emailAdherent())
                .isEqualTo("parent@example.test");
    }

    private Paiement paiement(Long id, LocalDate date, int montant, String type) {
        Paiement paiement = new Paiement();
        paiement.setId(id);
        paiement.setDateReglement(date);
        paiement.setMontant(montant);
        paiement.setTypeReglement(type);
        return paiement;
    }
}
