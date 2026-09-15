package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.ActiviteNm1;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.UserLite;
import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.models.resources.AdherentExport;
import com.wild.corp.adhesion.models.resources.AdherentLite;
import com.wild.corp.adhesion.models.resources.Groupe;
import com.wild.corp.adhesion.models.resources.Horaire;
import com.wild.corp.adhesion.repository.AdherentRepository;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.wild.corp.adhesion.utils.Status.VALIDEE;
import static com.wild.corp.adhesion.utils.Status.ANNULEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

class AdherentServicesTest {

    private final AdherentServices adherentServices = new AdherentServices();

    @Test
    void selectsMailRecipientsByEnumRoleWithoutReadingAnActivityId() {
        AdherentRepository repository = mock(AdherentRepository.class);
        when(repository.findByUserRole(ERole.ROLE_ENCADRANT)).thenReturn(List.of(existingAdherent()));
        ReflectionTestUtils.setField(adherentServices, "adherentRepository", repository);
        ActiviteServices activites = mock(ActiviteServices.class);
        ReflectionTestUtils.setField(adherentServices, "activiteServices", activites);

        Horaire selection = new Horaire();
        selection.setChecked(true);
        selection.setRole("ROLE_ENCADRANT");
        Groupe groupe = new Groupe();
        groupe.setNom("role");
        groupe.setNm1(false);
        groupe.setChecked(false);
        groupe.setHoraires(List.of(selection));
        List<String> recipients = new ArrayList<>();

        adherentServices.findByGroup(List.of(groupe), recipients);

        assertThat(recipients).containsExactly("alice@example.test");
        verifyNoInteractions(activites);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MEMBRECA", "MODERATOR", "BUREAU", "ENCADRANT", "REFERENT", "COMPTABLE"})
    void refusesEmailChangesByOtherRolesBeforeChangingPersonalData(String role) {
        authenticateAs(role);
        Adherent existing = existingAdherent();
        AdherentRepository repository = repositoryFor(existing);

        assertThatThrownBy(() -> adherentServices.update(emailUpdate("new@example.test")))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(existing.getUser().getUsername()).isEqualTo("alice@example.test");
        assertThat(existing.getNom()).isEqualTo("MARTIN");
        verify(repository, never()).save(any());
    }

    @Test
    void refusesEmailChangesWithoutAuthentication() {
        repositoryFor(existingAdherent());
        assertThatThrownBy(() -> adherentServices.update(emailUpdate("new@example.test")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SECRETAIRE", "ADMIN"})
    void allowsEmailChangesBySecretaryAndAdministrator(String role) {
        authenticateAs(role);
        Adherent existing = existingAdherent();
        AdherentRepository repository = repositoryFor(existing);

        adherentServices.update(emailUpdate(" NEW@Example.Test "));

        assertThat(existing.getUser().getUsername()).isEqualTo("new@example.test");
        verify(repository).save(existing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice@example.test", " ALICE@EXAMPLE.TEST "})
    void allowsAnOrdinaryUserToUpdateOtherFieldsWithoutChangingEmail(String email) {
        authenticateAs("USER");
        Adherent existing = existingAdherent();
        repositoryFor(existing);

        adherentServices.update(emailUpdate(email));

        assertThat(existing.getUser().getUsername()).isEqualTo("alice@example.test");
        assertThat(existing.getNom()).isEqualTo("DUPONT");
    }

    @Test
    void preservesEmailWhenUserIsOmittedFromTheUpdate() {
        authenticateAs("USER");
        Adherent existing = existingAdherent();
        repositoryFor(existing);
        AdherentLite update = emailUpdate("unused@example.test");
        update.setUser(null);

        adherentServices.update(update);

        assertThat(existing.getUser().getUsername()).isEqualTo("alice@example.test");
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "actor@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Adherent existingAdherent() {
        Adherent existing = new Adherent();
        existing.setId(42L);
        existing.setNom("MARTIN");
        existing.setPrenom("Alice");
        existing.setUser(new User("alice@example.test", "unused"));
        existing.setTribu(new Tribu(UUID.randomUUID()));
        return existing;
    }

    private AdherentRepository repositoryFor(Adherent existing) {
        AdherentRepository repository = mock(AdherentRepository.class);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        ReflectionTestUtils.setField(adherentServices, "adherentRepository", repository);
        AdhesionServices adhesions = mock(AdhesionServices.class);
        when(adhesions.reduceAdhesions(any())).thenReturn(Set.of());
        ReflectionTestUtils.setField(adherentServices, "adhesionServices", adhesions);
        return repository;
    }

    private AdherentLite emailUpdate(String email) {
        AdherentLite update = new AdherentLite();
        update.setId(42L);
        update.setNom("Dupont");
        update.setPrenom("Alice");
        UserLite user = new UserLite();
        user.setUsername(email);
        update.setUser(user);
        return update;
    }

    @Test
    void updatesTheStateAndDateOfExistingAccords() {
        Accord accordEnBase = new Accord();
        accordEnBase.setId(41L);
        accordEnBase.setEtat(true);

        Adherent adherentEnBase = new Adherent();
        adherentEnBase.setAccords(List.of(accordEnBase));

        Accord accordFront = new Accord();
        accordFront.setId(41L);
        accordFront.setEtat(false);
        accordFront.setDatePassage(LocalDate.of(2026, 8, 25));
        AdherentLite adherentFront = new AdherentLite();
        adherentFront.setAccords(List.of(accordFront));

        adherentServices.updateAccords(adherentFront, adherentEnBase);

        assertThat(accordEnBase.getEtat()).isFalse();
        assertThat(accordEnBase.getDatePassage()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void ignoresAnIncompleteAccordWhenCheckingWhetherAnAdherentIsComplete() {
        Adherent adherent = new Adherent();
        adherent.setNom("MARTIN");
        adherent.setPrenom("Alice");
        adherent.setGenre("Féminin");
        adherent.setNaissance(LocalDate.of(2000, 1, 1));
        adherent.setLieuNaissance("44");
        adherent.setAdresse("1 rue des Fleurs");
        adherent.setTelephone("0600000000");

        Accord accordSansNom = new Accord();
        Accord rgpd = new Accord();
        rgpd.setNom("RGPD");
        rgpd.setEtat(true);
        Accord droitImage = new Accord();
        droitImage.setNom("DroitImage");
        droitImage.setEtat(true);
        adherent.setAccords(List.of(accordSansNom, rgpd, droitImage));

        adherentServices.isComplet(adherent);

        assertThat(adherent.getCompletAdhesion()).isTrue();
    }

    @Test
    void refusesToCreateAnAdherentWithoutATribu() {
        assertThatThrownBy(() -> adherentServices.saveNewAdherent(new AdherentLite()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tribu obligatoire");
    }

    @Test
    void includesPreviousSeasonActivitiesInTheExport() {
        Activite activite = new Activite();
        activite.setNom("Danse");
        activite.setHoraire("Majeur");

        Adhesion adhesion = new Adhesion();
        adhesion.setActivite(activite);
        adhesion.setStatutActuel(VALIDEE.label);

        ActiviteNm1 yoga = new ActiviteNm1();
        yoga.setNom("Yoga");
        yoga.setHoraire("Majeur");
        ActiviteNm1 natation = new ActiviteNm1();
        natation.setNom("Natation");
        natation.setHoraire("Mineur");

        User user = new User();
        user.setUsername("alice@example.test");
        Adherent adherent = new Adherent();
        adherent.setId(1L);
        adherent.setPrenom("Alice");
        adherent.setNom("Martin");
        adherent.setMineur(false);
        adherent.setTelephone("0600000000");
        adherent.setUser(user);
        adherent.setAdhesions(new HashSet<>(List.of(adhesion)));
        adherent.setActivitesNm1(new ArrayList<>(List.of(yoga, natation)));

        AdherentRepository repository = mock(AdherentRepository.class);
        when(repository.findAll()).thenReturn(List.of(adherent));
        ReflectionTestUtils.setField(adherentServices, "adherentRepository", repository);

        List<AdherentExport> exports = adherentServices.getAllExportFlat();

        assertThat(exports).singleElement().satisfies(export -> {
            assertThat(export.getActivite1()).isEqualTo("Danse Majeur");
            assertThat(export.getStatutAdhesion1()).isEqualTo("Validée");
            assertThat(export.getTelephone()).isEqualTo("0600000000");
            assertThat(export.getActivitesNm1()).isEqualTo("Yoga Majeur, Natation Mineur");
        });
    }

    @Test
    void includesAdherentsWithCancelledOrNoAdhesionInTheExport() {
        Activite activite = new Activite();
        activite.setNom("Basket");
        activite.setHoraire("Loisir");

        Adhesion adhesionAnnulee = new Adhesion();
        adhesionAnnulee.setActivite(activite);
        adhesionAnnulee.setStatutActuel(ANNULEE.label);

        User user = new User();
        user.setUsername("bob@example.test");
        Adherent adherentAvecAdhesionAnnulee = new Adherent();
        adherentAvecAdhesionAnnulee.setId(1L);
        adherentAvecAdhesionAnnulee.setPrenom("Bob");
        adherentAvecAdhesionAnnulee.setNom("Dupont");
        adherentAvecAdhesionAnnulee.setMineur(false);
        adherentAvecAdhesionAnnulee.setUser(user);
        adherentAvecAdhesionAnnulee.setAdhesions(new HashSet<>(List.of(adhesionAnnulee)));

        Adherent adherentSansAdhesion = new Adherent();
        adherentSansAdhesion.setId(2L);
        adherentSansAdhesion.setPrenom("Chloé");
        adherentSansAdhesion.setNom("Martin");
        adherentSansAdhesion.setMineur(false);
        adherentSansAdhesion.setUser(user);

        AdherentRepository repository = mock(AdherentRepository.class);
        when(repository.findAll()).thenReturn(List.of(adherentAvecAdhesionAnnulee, adherentSansAdhesion));
        ReflectionTestUtils.setField(adherentServices, "adherentRepository", repository);

        List<AdherentExport> exports = adherentServices.getAllExportFlat();

        assertThat(exports).hasSize(2);
        assertThat(exports).filteredOn(export -> export.getId().equals(1L)).singleElement().satisfies(export -> {
            assertThat(export.getActivite1()).isEqualTo("Basket Loisir");
            assertThat(export.getStatutAdhesion1()).isEqualTo("Annulée");
        });
        assertThat(exports).filteredOn(export -> export.getId().equals(2L)).singleElement().satisfies(export -> {
            assertThat(export.getActivite1()).isNull();
            assertThat(export.getStatutAdhesion1()).isNull();
        });
    }
}
