package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaterielServicesTest {
    private MaterielRepository materiels;
    private ZonageRepository zonages;
    private UserRepository users;
    private EmpruntMaterielRepository emprunts;
    private MaterielServices service;
    private Materiel materiel;
    private User user;

    @BeforeEach
    void setUp() {
        materiels = mock(MaterielRepository.class);
        zonages = mock(ZonageRepository.class);
        users = mock(UserRepository.class);
        emprunts = mock(EmpruntMaterielRepository.class);
        java.util.List<EmpruntMateriel> journal = new java.util.ArrayList<>();
        when(emprunts.save(any())).thenAnswer(invocation -> {
            EmpruntMateriel entree = invocation.getArgument(0);
            entree.setId((long) journal.size() + 1);
            journal.add(entree);
            return entree;
        });
        when(emprunts.findAllByMaterielIdOrderByDateEmpruntDescIdDesc(any())).thenAnswer(invocation -> journal);
        when(emprunts.findFirstByMaterielIdAndHorsServiceIsNullAndDateRetourIsNullOrderByDateEmpruntDescIdDesc(any()))
                .thenAnswer(invocation -> journal.stream().filter(e -> e.getHorsService() == null && e.getDateRetour() == null).findFirst());
        service = new MaterielServices(materiels, zonages, users, emprunts);
        Zonage zone = Zonage.builder().id(1L).nom("Étagère A").couleur("#0F9D58").build();
        materiel = Materiel.builder().id(2L).nom("Vidéoprojecteur").zonage(zone).build();
        user = new User("camille@example.org", "password");
        user.setId(3L);
        when(materiels.findByIdForUpdate(2L)).thenReturn(Optional.of(materiel));
        when(zonages.findByIdForUpdate(1L)).thenReturn(Optional.of(zone));
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(materiels.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(zonages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAvailableMaterialInAnExistingZone() {
        MaterielResponse resultat = service.create(new MaterielRequest("  Vidéoprojecteur  ", 1L));
        assertThat(resultat.nom()).isEqualTo("Vidéoprojecteur");
        assertThat(resultat.zonage().nom()).isEqualTo("Étagère A");
        assertThat(resultat.emprunteurId()).isNull();
        assertThat(resultat.dateEmprunt()).isNull();
    }

    @Test
    void borrowsForTheConnectedUserWithTheirNameAndDate() {
        Adherent adherent = new Adherent();
        adherent.setPrenom("Camille");
        adherent.setNom("Martin");
        user.setAdherent(adherent);
        MaterielResponse resultat = service.emprunter(2L, user.getUsername());
        assertThat(resultat.emprunteurId()).isEqualTo(3L);
        assertThat(resultat.emprunteurNom()).isEqualTo("Camille Martin");
        assertThat(resultat.dateEmprunt()).isNotNull();
    }

    @Test
    void editChangesNameAndZoneWithoutChangingLoanOrHs() {
        service.emprunter(2L, user.getUsername());
        materiel.setHorsService(true);
        materiel.setQuantiteHorsService(1);
        materiel.setQuantiteTotale(2);
        Zonage reserve = Zonage.builder().id(6L).nom("Réserve").couleur("#654321").build();
        when(zonages.findByIdForUpdate(6L)).thenReturn(Optional.of(reserve));
        MaterielResponse resultat = service.update(2L, new MaterielRequest("  Projecteur  ", 6L, false));
        assertThat(resultat.nom()).isEqualTo("Projecteur");
        assertThat(resultat.zonage().id()).isEqualTo(6L);
        assertThat(resultat.emprunteurId()).isEqualTo(user.getId());
        assertThat(resultat.dateEmprunt()).isNotNull();
        assertThat(resultat.horsService()).isTrue();
    }

    @Test
    void editRejectsInvalidNameZoneOrMissingMaterial() {
        assertThatThrownBy(() -> service.update(2L, new MaterielRequest(" ", 1L))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.update(2L, new MaterielRequest("x".repeat(151), 1L))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.update(2L, new MaterielRequest("Objet", null))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.update(2L, new MaterielRequest("Objet", 9L)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.update(9L, new MaterielRequest("Objet", 1L)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(materiel.getNom()).isEqualTo("Vidéoprojecteur");
    }

    @Test
    void deleteRefusesActiveLoanAndRemovesHistoryBeforeMaterial() {
        service.emprunter(2L, user.getUsername());
        assertThatThrownBy(() -> service.delete(2L)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(emprunts, never()).deleteAllByMaterielId(any());
        verify(materiels, never()).delete(any());
        service.restituer(2L, user.getUsername(), false);
        service.delete(2L);
        var ordre = inOrder(emprunts, materiels);
        ordre.verify(emprunts).deleteAllByMaterielId(2L);
        ordre.verify(materiels).delete(materiel);
    }

    @Test
    void fallsBackToUsernameWhenThereIsNoAdherent() {
        assertThat(service.emprunter(2L, user.getUsername()).emprunteurNom()).isEqualTo(user.getUsername());
    }

    @Test
    void cannotReplaceAnExistingBorrower() {
        service.emprunter(2L, user.getUsername());
        assertThatThrownBy(() -> service.emprunter(2L, "other@example.org"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(service.getHistorique(2L).stream().filter(e -> e.dateRetour() == null).findFirst().orElseThrow().emprunteurId()).isEqualTo(3L);
    }

    @Test
    void onlyBorrowerOrAdminCanReturnAndClearTheLoan() {
        service.emprunter(2L, user.getUsername());
        User autre = new User("other@example.org", "password");
        autre.setId(4L);
        when(users.findByUsername(autre.getUsername())).thenReturn(Optional.of(autre));
        assertThatThrownBy(() -> service.restituer(2L, autre.getUsername(), false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(service.getHistorique(2L).stream().filter(e -> e.dateRetour() == null).findFirst().orElseThrow().emprunteurId()).isEqualTo(3L);
        MaterielResponse retour = service.restituer(2L, user.getUsername(), false);
        assertThat(retour.emprunteurId()).isNull();
        assertThat(retour.emprunteurNom()).isNull();
        assertThat(retour.dateEmprunt()).isNull();
        service.emprunter(2L, user.getUsername());
        assertThat(service.restituer(2L, autre.getUsername(), true).emprunteurId()).isNull();
    }

    @Test
    void rejectsAnAlreadyAvailableReturnAndUnknownMaterial() {
        assertThatThrownBy(() -> service.restituer(2L, user.getUsername(), false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.emprunter(999L, user.getUsername()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void normalizesAndUpdatesZoneLabelsAndColours() {
        assertThat(service.createZonage(new ZonageConfiguration(null, "  Étagère B  ", "#abcdef")))
                .isEqualTo(new ZonageConfiguration(null, "Étagère B", "#ABCDEF"));
        assertThat(service.updateZonage(1L, new ZonageConfiguration(null, "Réserve", "#123456")))
                .isEqualTo(new ZonageConfiguration(1L, "Réserve", "#123456"));
    }

    @Test
    void rejectsInvalidMaterialAndZoneConfigurations() {
        for (MaterielRequest request : new MaterielRequest[] {
                new MaterielRequest(" ", 1L), new MaterielRequest("x".repeat(151), 1L), new MaterielRequest("Objet", null) }) {
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> service.create(new MaterielRequest("Objet", 999L)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        for (ZonageConfiguration request : new ZonageConfiguration[] {
                new ZonageConfiguration(null, " ", "#123456"), new ZonageConfiguration(null, "x".repeat(101), "#123456"),
                new ZonageConfiguration(null, "Zone", "red"), new ZonageConfiguration(null, "Zone", null) }) {
            assertThatThrownBy(() -> service.createZonage(request)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void adminCanSelectACaMemberButCaCannotChooseABorrower() {
        user.getRoles().add(ERole.ROLE_ADMIN);
        User autre = new User("ca@example.org", "password");
        autre.setId(4L);
        autre.getRoles().add(ERole.ROLE_MEMBRECA);
        when(users.findById(4L)).thenReturn(Optional.of(autre));
        assertThatThrownBy(() -> service.emprunter(2L, user.getUsername(), 4L, false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(service.emprunter(2L, user.getUsername(), 4L, true).emprunteurId()).isEqualTo(4L);
        verify(emprunts).save(org.mockito.ArgumentMatchers.argThat(emprunt ->
                emprunt.getEmprunteurId().equals(4L) && emprunt.getEnregistreParId().equals(3L)));
    }

    @Test
    void adminCannotSelectANonCaMemberOrAMissingAccount() {
        assertThatThrownBy(() -> service.emprunter(2L, user.getUsername(), 4L, true))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        when(users.findById(3L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.emprunter(2L, user.getUsername(), 3L, true))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        user.getRoles().add(ERole.ROLE_ADMIN);
        assertThatThrownBy(() -> service.emprunter(2L, user.getUsername(), 3L, true))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(emprunts, never()).save(any());
    }

    @Test
    void hsRequiresAnExplanationBeforeChangingState() {
        for (String commentaire : new String[] {null, "  ", "x".repeat(2001)}) {
            assertThatThrownBy(() -> service.updateHorsService(2L,
                    new HorsServiceRequest(true, commentaire), user.getUsername()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThat(materiel.isHorsService()).isFalse();
        verify(emprunts, never()).save(any());
        materiel.setHorsService(true);
        materiel.setQuantiteHorsService(1);
        assertThatThrownBy(() -> service.updateHorsService(2L,
                new HorsServiceRequest(false, " "), user.getUsername()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(materiel.isHorsService()).isTrue();
    }

    @Test
    void hsReducesAvailabilityAndCanBeRepaired() {
        service.updateHorsService(2L, new HorsServiceRequest(true, "Lampe cassée"), user.getUsername());
        assertThatThrownBy(() -> service.emprunter(2L, user.getUsername())).isInstanceOf(ResponseStatusException.class);
        service.updateHorsService(2L, new HorsServiceRequest(false, "Lampe remplacée"), user.getUsername());
        assertThat(service.emprunter(2L, user.getUsername()).quantiteDisponible()).isZero();
        assertThatThrownBy(() -> service.updateHorsService(2L, new HorsServiceRequest(null, null), user.getUsername()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cannotDeleteAZoneContainingMaterial() {
        when(materiels.existsByZonageId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteZonage(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(zonages, never()).delete(any());
        when(materiels.existsByZonageId(1L)).thenReturn(false);
        service.deleteZonage(1L);
        verify(zonages).delete(materiel.getZonage());
    }
}
