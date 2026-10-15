package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Materiel;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ContextConfiguration(classes = MaterielPersistenceTest.JpaTestApplication.class)
@Import(MaterielServices.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MaterielPersistenceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Materiel.class)
    @EnableJpaRepositories(basePackageClasses = MaterielRepository.class)
    static class JpaTestApplication {}

    @Autowired private MaterielServices service;
    @Autowired private MaterielRepository materiels;
    @Autowired private ZonageRepository zonages;
    @Autowired private UserRepository users;
    @Autowired private EmpruntMaterielRepository emprunts;

    @AfterEach
    void cleanUp() {
        emprunts.deleteAll();
        materiels.deleteAll();
        zonages.deleteAll();
        users.deleteAll();
    }

    @Test
    void loanAndZoneChangesPersistAcrossTransactions() {
        User user = users.save(new User("member@example.org", "password"));
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Étagère A", "#123456"));
        MaterielResponse objet = service.create(new MaterielRequest("Vidéoprojecteur", zone.id()));
        service.emprunter(objet.id(), user.getUsername());

        MaterielResponse emprunte = service.getAll().getFirst();
        assertThat(emprunte.emprunteurId()).isEqualTo(user.getId());
        assertThat(emprunte.emprunteurNom()).isEqualTo(user.getUsername());
        assertThat(emprunte.dateEmprunt()).isNotNull();

        service.updateZonage(zone.id(), new ZonageConfiguration(null, "Réserve", "#ABCDEF"));
        assertThat(service.getAll().getFirst().zonage().nom()).isEqualTo("Réserve");
        assertThat(service.getAll().getFirst().zonage().couleur()).isEqualTo("#ABCDEF");
        assertThatThrownBy(() -> service.deleteZonage(zone.id())).isInstanceOf(ResponseStatusException.class);

        service.restituer(objet.id(), user.getUsername(), false);
        MaterielResponse retourne = service.getAll().getFirst();
        assertThat(retourne.emprunteurId()).isNull();
        assertThat(retourne.emprunteurNom()).isNull();
        assertThat(retourne.dateEmprunt()).isNull();
        assertThat(retourne.zonage().id()).isEqualTo(zone.id());
    }

    @Test
    void historyKeepsSeveralLoansAndAnyAdminCanRecordTheReturn() {
        User admin = new User("admin@example.org", "password");
        admin.getRoles().add(ERole.ROLE_ADMIN);
        admin.getRoles().add(ERole.ROLE_MEMBRECA);
        admin = users.save(admin);
        User autre = new User("admin2@example.org", "password");
        autre.getRoles().add(ERole.ROLE_ADMIN);
        autre.getRoles().add(ERole.ROLE_MEMBRECA);
        autre = users.save(autre);
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456"));
        Long id = service.create(new MaterielRequest("Objet", zone.id())).id();
        User membreCA = new User("ca@example.org", "password");
        membreCA.getRoles().add(ERole.ROLE_MEMBRECA);
        membreCA = users.save(membreCA);
        User adminHorsCA = new User("admin-hors-ca@example.org", "password");
        adminHorsCA.getRoles().add(ERole.ROLE_ADMIN);
        users.save(adminHorsCA);
        users.save(new User("ordinary@example.org", "password"));
        assertThat(service.getMembresCA()).extracting(MembreCAMaterielResponse::id)
                .containsExactlyInAnyOrder(admin.getId(), autre.getId(), membreCA.getId());
        service.emprunter(id, admin.getUsername(), autre.getId(), true);
        service.restituer(id, admin.getUsername(), true);
        EmpruntMaterielResponse premier = service.getHistorique(id).getFirst();
        assertThat(premier.emprunteurId()).isEqualTo(autre.getId());
        assertThat(premier.enregistreParNom()).isEqualTo(admin.getUsername());
        assertThat(premier.rapporteParNom()).isEqualTo(admin.getUsername());
        assertThat(premier.dateRetour()).isAfterOrEqualTo(premier.dateEmprunt());
        service.emprunter(id, autre.getUsername(), admin.getId(), true);
        List<EmpruntMaterielResponse> journal = service.getHistorique(id);
        assertThat(journal).hasSize(2);
        assertThat(journal.getFirst().emprunteurId()).isEqualTo(admin.getId());
        assertThat(journal.getFirst().dateRetour()).isNull();
        assertThat(journal.getLast()).isEqualTo(premier);
        service.restituer(id, autre.getUsername(), true);
        service.updateHorsService(id, new HorsServiceRequest(true, "Lampe cassée"), autre.getUsername());
        assertThat(service.getAll().getFirst().horsService()).isTrue();
        users.deleteAll();
        assertThat(service.getHistorique(id)).hasSize(3);
        assertThat(service.getHistorique(id).getFirst().horsService()).isTrue();
        assertThat(service.getHistorique(id).getFirst().enregistreParNom()).isEqualTo(autre.getUsername());
        assertThat(service.getHistorique(id).get(1).dateRetour()).isNotNull();
        assertThat(service.getHistorique(id).getLast().emprunteurNom()).isEqualTo(autre.getUsername());
    }

    @Test
    void legacyOpenLoanIsAddedToHistoryExactlyOnceAndPreservedOnReturn() {
        User user = users.save(new User("legacy@example.org", "password"));
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456"));
        Long id = service.create(new MaterielRequest("Ancien objet", zone.id())).id();
        Materiel ancien = materiels.findById(id).orElseThrow();
        ancien.setEmprunteurId(user.getId());
        ancien.setEmprunteurNom("Ancien emprunteur");
        ancien.setDateEmprunt(java.time.OffsetDateTime.parse("2026-10-01T12:00:00Z"));
        materiels.save(ancien);
        assertThat(service.getHistorique(id)).hasSize(1);
        assertThat(service.getHistorique(id)).hasSize(1);
        service.restituer(id, user.getUsername(), false);
        assertThat(service.getHistorique(id)).hasSize(1);
        assertThat(service.getHistorique(id).getFirst().dateRetour()).isNotNull();
        assertThat(service.getHistorique(id).getFirst().emprunteurNom()).isEqualTo("Ancien emprunteur");
        assertThat(service.getHistorique(id).getFirst().enregistreParNom()).isNull();
    }

    @Test
    void concurrentBorrowersCannotBothBorrowTheSameObject() throws Exception {
        users.save(new User("first@example.org", "password"));
        users.save(new User("second@example.org", "password"));
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456"));
        Long id = service.create(new MaterielRequest("Objet unique", zone.id())).id();
        CountDownLatch depart = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> resultats = List.of(
                    executor.submit(() -> emprunterApres(depart, id, "first@example.org")),
                    executor.submit(() -> emprunterApres(depart, id, "second@example.org")));
            depart.countDown();
            assertThat(List.of(resultats.get(0).get(10, TimeUnit.SECONDS), resultats.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(service.getAll().getFirst().emprunteurNom()).isIn("first@example.org", "second@example.org");
        assertThat(service.getHistorique(id)).hasSize(1);
    }

    @Test
    void editsPersistAndDeletionHandlesTheLoanHistoryForeignKey() {
        User user = new User("logistique@example.org", "password");
        user.getRoles().add(ERole.ROLE_LOGISTIQUE);
        users.save(user);
        assertThat(users.findAllByRole(ERole.ROLE_LOGISTIQUE)).hasSize(1);
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456"));
        ZonageConfiguration reserve = service.createZonage(new ZonageConfiguration(null, "Réserve", "#654321"));
        Long id = service.create(new MaterielRequest("Objet", zone.id(), false)).id();
        service.emprunter(id, user.getUsername());
        service.update(id, new MaterielRequest("Nouveau nom", reserve.id()));
        MaterielResponse resultat = service.getAll().getFirst();
        assertThat(resultat.nom()).isEqualTo("Nouveau nom");
        assertThat(resultat.zonage().id()).isEqualTo(reserve.id());
        assertThat(resultat.horsService()).isFalse();
        assertThat(resultat.emprunteurId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.getHistorique(id)).hasSize(1);
        service.restituer(id, user.getUsername(), false);
        service.delete(id);
        assertThat(materiels.findById(id)).isEmpty();
        assertThat(emprunts.findAllByMaterielIdOrderByDateEmpruntDescIdDesc(id)).isEmpty();
        service.deleteZonage(reserve.id());
        assertThat(zonages.findById(reserve.id())).isEmpty();
    }

    @Test
    void hsEventsPersistWithoutBorrowingAndDoNotReplaceAnOpenLoan() {
        User acteur = users.save(new User("admin-hs@example.org", "password"));
        ZonageConfiguration zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456"));
        Long id = service.create(new MaterielRequest("Objet", zone.id())).id();
        service.updateHorsService(id, new HorsServiceRequest(true, "Lampe cassée"), acteur.getUsername());
        service.updateHorsService(id, new HorsServiceRequest(true, "Lampe cassée"), acteur.getUsername());
        assertThat(service.getHistorique(id)).hasSize(1);
        EmpruntMaterielResponse hs = service.getHistorique(id).getFirst();
        assertThat(hs.horsService()).isTrue();
        assertThat(hs.commentaire()).isEqualTo("Lampe cassée");
        assertThat(service.getAll().getFirst().probleme()).isEqualTo("Lampe cassée");
        assertThat(hs.emprunteurId()).isNull();
        assertThat(hs.emprunteurNom()).isNull();
        assertThat(hs.dateEmprunt()).isNotNull();
        assertThat(hs.dateRetour()).isNull();
        assertThat(hs.enregistreParNom()).isEqualTo(acteur.getUsername());
        assertThat(service.getAll().getFirst().emprunteurId()).isNull();

        assertThatThrownBy(() -> service.emprunter(id, acteur.getUsername())).isInstanceOf(ResponseStatusException.class);
        service.updateHorsService(id, new HorsServiceRequest(false, "Lampe remplacée"), acteur.getUsername());
        service.emprunter(id, acteur.getUsername());
        service.restituer(id, acteur.getUsername(), true);
        List<EmpruntMaterielResponse> journal = service.getHistorique(id);
        assertThat(journal).hasSize(3);
        assertThat(journal.get(1).horsService()).isFalse();
        assertThat(journal.get(1).commentaire()).isEqualTo("Lampe remplacée");
        assertThat(service.getAll().getFirst().probleme()).isNull();
        assertThat(journal.get(1).dateRetour()).isNull();
        assertThat(journal.getFirst().horsService()).isNull();
        assertThat(journal.getFirst().dateRetour()).isNotNull();
        assertThat(journal.getLast()).isEqualTo(hs);
        assertThat(service.getAll().getFirst().horsService()).isFalse();
    }

    @Test
    void quantitiesSupportParallelLoansPartialReturnsAndStockConstraints() {
        User pierre = users.save(new User("pierre@example.org", "password"));
        User marie = users.save(new User("marie@example.org", "password"));
        Long zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456")).id();
        Long id = service.create(new MaterielRequest("Tables", zone, false, 10)).id();
        service.emprunter(id, pierre.getUsername(), null, false, 3);
        MaterielResponse stock = service.emprunter(id, marie.getUsername(), null, false, 5);
        assertThat(stock.quantiteDisponible()).isEqualTo(2);
        assertThat(stock.emprunts()).hasSize(2);
        assertThatThrownBy(() -> service.emprunter(id, pierre.getUsername(), null, false, 3)).isInstanceOf(ResponseStatusException.class);
        Long empruntPierre = stock.emprunts().stream().filter(e -> e.emprunteurId().equals(pierre.getId())).findFirst().orElseThrow().id();
        assertThatThrownBy(() -> service.restituer(id, marie.getUsername(), false, new RetourMaterielRequest(empruntPierre, 1)))
                .isInstanceOf(ResponseStatusException.class);
        stock = service.restituer(id, pierre.getUsername(), false, new RetourMaterielRequest(empruntPierre, 2));
        assertThat(stock.quantiteDisponible()).isEqualTo(4);
        EmpruntMaterielResponse partiel = service.getHistorique(id).stream().filter(e -> e.id().equals(empruntPierre)).findFirst().orElseThrow();
        assertThat(partiel.quantiteRetournee()).isEqualTo(2);
        assertThat(partiel.retours()).hasSize(1);
        assertThat(partiel.retours().getFirst().getQuantite()).isEqualTo(2);
        assertThat(partiel.dateRetour()).isNull();
        assertThatThrownBy(() -> service.restituer(id, pierre.getUsername(), false, new RetourMaterielRequest(empruntPierre, 2)))
                .isInstanceOf(ResponseStatusException.class);
        stock = service.updateHorsService(id, new HorsServiceRequest(true, "Pied cassé", 1), pierre.getUsername());
        assertThat(stock.quantiteDisponible()).isEqualTo(3);
        assertThatThrownBy(() -> service.update(id, new MaterielRequest("Tables", zone, false, 6))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.updateHorsService(id, new HorsServiceRequest(true, "Panne", 5), pierre.getUsername()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.emprunter(id, pierre.getUsername(), null, false, 0)).isInstanceOf(ResponseStatusException.class);
        service.restituer(id, pierre.getUsername(), false, new RetourMaterielRequest(empruntPierre, 1));
        assertThat(service.getHistorique(id).stream().filter(e -> e.id().equals(empruntPierre)).findFirst().orElseThrow().retours()).hasSize(2);
    }

    @Test
    void concurrentQuantityLoansNeverExceedStock() throws Exception {
        users.save(new User("first@example.org", "password"));
        users.save(new User("second@example.org", "password"));
        Long zone = service.createZonage(new ZonageConfiguration(null, "Stock", "#123456")).id();
        Long id = service.create(new MaterielRequest("Tables", zone, false, 10)).id();
        CountDownLatch depart = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> emprunterQuantiteApres(depart, id, "first@example.org"));
            var second = executor.submit(() -> emprunterQuantiteApres(depart, id, "second@example.org"));
            depart.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(service.getAll().getFirst().quantiteDisponible()).isEqualTo(4);
    }

    private int emprunterQuantiteApres(CountDownLatch depart, Long id, String username) throws Exception {
        depart.await(5, TimeUnit.SECONDS);
        try { service.emprunter(id, username, null, false, 6); return 200; }
        catch (ResponseStatusException exception) { return exception.getStatusCode().value(); }
    }

    private int emprunterApres(CountDownLatch depart, Long id, String username) throws InterruptedException {
        if (!depart.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Délai de démarrage dépassé");
        try {
            service.emprunter(id, username);
            return 200;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }
}
