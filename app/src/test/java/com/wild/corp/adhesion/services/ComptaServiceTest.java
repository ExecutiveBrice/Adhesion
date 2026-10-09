package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.wild.corp.adhesion.models.resources.ComptaPeriode;
import com.wild.corp.adhesion.models.ComptaActivite;
import com.wild.corp.adhesion.models.Paiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ComptaServiceTest {
    private final ComptaService service = new ComptaService();
    private final Activite activite = activity("Basket");
    private final LocalDate debut = LocalDate.of(2024, 2, 29);
    private final LocalDate fin = LocalDate.of(2024, 3, 31);

    @BeforeEach
    void setup() {
        service.activiteServices = mock(ActiviteServices.class);
        service.adhesionServices = mock(AdhesionServices.class);
        service.adhesionRepository = mock(AdhesionRepository.class);
        when(service.activiteServices.getAll()).thenReturn(List.of(activite));
    }

    @Test
    void includesBothBoundariesAndExcludesAdjacentDays() {
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion(activite, true,
                payment(debut.minusDays(1), "HelloAsso", 1000),
                payment(debut, "HelloAsso", 10),
                payment(debut.plusDays(1), "HelloAsso", 20),
                payment(fin, "HelloAsso", 30),
                payment(fin.plusDays(1), "HelloAsso", 2000))));

        assertThat(service.getAll(debut, fin)).singleElement().satisfies(compte ->
                assertThat(compte.getHelloAsso()).isEqualTo(60));
        verify(service.adhesionServices, times(1)).getAll();
    }

    @Test
    void includesPaymentsForASingleDay() {
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion(activite, true,
                payment(debut.minusDays(1), "Chèque", 1000),
                payment(debut, "Chèque", 12), payment(debut, "Chèque", 18),
                payment(debut.plusDays(1), "Chèque", 2000))));
        assertThat(service.getAll(debut, debut).getFirst().getCheque()).isEqualTo(30);
    }

    @Test
    void separatesPaymentTypesAndGroupsActivitiesWithTheSameName() {
        Activite otherSlot = activity("Basket");
        Activite autre = activity("Danse");
        when(service.activiteServices.getAll()).thenReturn(List.of(activite, otherSlot, autre));
        when(service.adhesionServices.getAll()).thenReturn(List.of(
                adhesion(activite, true, payment(debut, "Espèces", 10), payment(fin, "Pass sport", 20),
                        payment(fin, "Intermarché", 30), payment(fin, "HelloAsso", 40),
                        payment(fin, "HelloAsso 3X", 50), payment(fin, "Chèque", 60),
                        payment(fin, "Chèque 3X", 70), payment(fin, "Autre", 80)),
                adhesion(otherSlot, true, payment(fin, "Espèces", 5)),
                adhesion(autre, true, payment(fin, "Espèces", 200))));

        List<ComptaActivite> result = service.getAll(debut, fin);
        assertThat(result).hasSize(2);
        ComptaActivite basket = result.getFirst();
        assertThat(basket.getEspece()).isEqualTo(15);
        assertThat(basket.getPassport()).isEqualTo(20);
        assertThat(basket.getIntermarche()).isEqualTo(30);
        assertThat(basket.getHelloAsso()).isEqualTo(40);
        assertThat(basket.getHelloAsso3x()).isEqualTo(50);
        assertThat(basket.getCheque()).isEqualTo(60);
        assertThat(basket.getCheque3x()).isEqualTo(70);
        assertThat(basket.getAutre()).isEqualTo(80);
        assertThat(result.get(1).getEspece()).isEqualTo(200);
    }

    @Test
    void ignoresUnvalidatedAdhesionsAndIncompletePayments() {
        Adhesion withoutPayments = adhesion(activite, true);
        withoutPayments.setPaiements(null);
        when(service.adhesionServices.getAll()).thenReturn(List.of(
                adhesion(activite, false, payment(fin, "Espèces", 1000)),
                adhesion(activite, null, payment(fin, "Espèces", 1000)),
                adhesion(null, true, payment(fin, "Espèces", 1000)), withoutPayments,
                adhesion(activite, true, null, payment(null, "Espèces", 1000),
                        payment(fin, "Espèces", null), payment(fin, null, 1000),
                        payment(fin, "Espèces", 15))));
        assertThat(service.getAll(debut, fin).getFirst().getEspece()).isEqualTo(15);
    }

    @Test
    void rejectsMissingAndReversedPeriodsBeforeReadingData() {
        assertThatThrownBy(() -> service.getAll(null, fin)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getAll(debut, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getAll(fin, debut)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(service.activiteServices, service.adhesionServices);
    }

    @Test
    void doesNotWrapAnOverflowingTotal() {
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion(activite, true,
                payment(fin, "HelloAsso", Integer.MAX_VALUE), payment(fin, "HelloAsso", 1))));
        assertThatThrownBy(() -> service.getAll(debut, fin)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void detailsContainAllPaymentsAndIdentifyThoseUsedInTheSummary() {
        Adhesion retained = adhesion(activite, true,
                payment(debut.minusDays(1), "HelloAsso", 1000),
                payment(fin, "Intermarché", 30), payment(debut, "HelloAsso", 10),
                payment(fin.plusDays(1), "Chèque", 2000),
                payment(fin, "Inconnu", 3000), payment(fin, "Espèces", null));
        retained.setId(7L);
        Adherent member = new Adherent();
        member.setNom("Martin");
        member.setPrenom("Camille");
        retained.setAdherent(member);
        retained.setStatutActuel("Validée");
        Adhesion second = adhesion(activite, true, payment(debut, "HelloAsso", 20));
        second.setId(8L);
        when(service.adhesionServices.getAll()).thenReturn(List.of(retained, second,
                adhesion(activite, false, payment(debut, "HelloAsso", 1000)),
                adhesion(activite, true, payment(debut.minusDays(1), "HelloAsso", 1000)),
                adhesion(activite, true, payment(debut, "Inconnu", 1000))));

        ComptaPeriode result = service.getPeriode(debut, fin);
        assertThat(result.adhesions()).hasSize(2);
        assertThat(result.adhesions()).filteredOn(row -> row.id().equals(7L)).singleElement().satisfies(row -> {
            assertThat(row.nomAdherent()).isEqualTo("Martin");
            assertThat(row.prenomAdherent()).isEqualTo("Camille");
            assertThat(row.nomActivite()).isEqualTo("Basket");
            assertThat(row.statutActuel()).isEqualTo("Validée");
            assertThat(row.totalPeriode()).isEqualTo(40);
            assertThat(row.paiements()).hasSize(6);
            assertThat(row.paiements()).filteredOn(ComptaPeriode.PaiementCompta::inclusDansPeriode)
                    .extracting(ComptaPeriode.PaiementCompta::dateReglement)
                    .containsExactly(debut, fin);
            assertThat(row.paiements()).filteredOn(ComptaPeriode.PaiementCompta::inclusDansPeriode)
                    .extracting(ComptaPeriode.PaiementCompta::montant)
                    .containsExactly(10, 30);
        });
        int totalDetails = result.adhesions().stream().mapToInt(ComptaPeriode.AdhesionCompta::totalPeriode).sum();
        ComptaActivite recap = result.recap().getFirst();
        assertThat(totalDetails).isEqualTo(60);
        assertThat(recap.getHelloAsso()).isEqualTo(30);
        assertThat(recap.getIntermarche()).isEqualTo(30);
        assertThat(totalDetails).isEqualTo(recap.getHelloAsso() + recap.getIntermarche());
        verify(service.adhesionServices, times(1)).getAll();
    }

    @Test
    void detailsSupportASingleDayAndAnEmptyPeriod() {
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion(activite, true,
                payment(debut, "Espèces", 15), payment(debut.plusDays(1), "Espèces", 1000))));
        ComptaPeriode singleDay = service.getPeriode(debut, debut);
        assertThat(singleDay.adhesions()).singleElement().satisfies(row -> {
            assertThat(row.paiements()).hasSize(2);
            assertThat(row.paiements()).filteredOn(ComptaPeriode.PaiementCompta::inclusDansPeriode).hasSize(1);
            assertThat(row.totalPeriode()).isEqualTo(15);
        });
        ComptaPeriode empty = service.getPeriode(fin, fin);
        assertThat(empty.adhesions()).isEmpty();
        assertThat(empty.recap().getFirst().getEspece()).isZero();
    }

    @Test
    void usesTheChosenContactEmailIncludingARepresentative() {
        Adhesion adhesion = adhesion(activite, true, payment(debut, "HelloAsso", 10));
        Adherent member = new Adherent();
        User user = new User();
        user.setUsername("member@example.org");
        member.setUser(user);
        adhesion.setAdherent(member);
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion));
        assertThat(service.getPeriode(debut, fin).adhesions().getFirst().emailAdherent()).isEqualTo("member@example.org");
        Adherent representative = new Adherent();
        User contact = new User();
        contact.setUsername("parent@example.org");
        representative.setUser(contact);
        member.setRepresentant(representative);
        member.setEmailRepresentant(true);
        assertThat(service.getPeriode(debut, fin).adhesions().getFirst().emailAdherent()).isEqualTo("parent@example.org");
        representative.setUser(null);
        assertThat(service.getPeriode(debut, fin).adhesions().getFirst().emailAdherent()).isNull();
    }

    @Test
    void editsAnExistingPaymentAndReconcilesCBWithTheSummary() {
        Paiement payment = payment(debut, "HelloAsso", 10);
        payment.setId(8L);
        Adhesion adhesion = adhesion(activite, true, payment);
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion));
        service.savePaiement(7L, new ComptaPeriode.PaiementCompta(8L, fin, "CB", 15), "comptable");
        assertThat(payment.getMontant()).isEqualTo(15);
        assertThat(payment.getDateReglement()).isEqualTo(fin);
        assertThat(payment.getTypeReglement()).isEqualTo("CB");
        ComptaPeriode result = service.getPeriode(debut, fin);
        assertThat(result.recap().getFirst().getCb()).isEqualTo(15);
        assertThat(result.recap().getFirst().getHelloAsso()).isZero();
        assertThat(result.adhesions().getFirst().totalPeriode()).isEqualTo(15);
        verify(service.adhesionRepository).save(adhesion);
        verify(service.adhesionServices).addModification(eq("comptable"), eq(7L), contains("comptabilité"));
    }

    @Test
    void addsTheRequestedPaymentWithoutReplacingItsDateOrAmount() {
        Adhesion adhesion = adhesion(activite, true);
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        service.savePaiement(7L, new ComptaPeriode.PaiementCompta(null, debut, "Chèque", 22), "comptable");
        assertThat(adhesion.getPaiements()).singleElement().satisfies(payment -> {
            assertThat(payment.getMontant()).isEqualTo(22);
            assertThat(payment.getDateReglement()).isEqualTo(debut);
            assertThat(payment.getTypeReglement()).isEqualTo("Chèque");
        });
        verify(service.adhesionRepository).save(adhesion);
    }

    @Test
    void deletesOnlyAPaymentBelongingToTheAdhesionAndRecalculates() {
        Paiement payment = payment(debut, "HelloAsso", 10);
        payment.setId(8L);
        Adhesion adhesion = adhesion(activite, true, payment);
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion));
        service.deletePaiement(7L, 8L, "comptable");
        assertThat(adhesion.getPaiements()).isEmpty();
        assertThat(service.getPeriode(debut, fin).adhesions()).isEmpty();
        verify(service.adhesionRepository).save(adhesion);
        verify(service.adhesionServices).addModification(eq("comptable"), eq(7L), contains("Suppression"));
    }

    @Test
    void rejectsCrossAdhesionPaymentEditsAndDeletesWithoutWriting() {
        Adhesion adhesion = adhesion(activite, true);
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        assertThatThrownBy(() -> service.savePaiement(7L,
                new ComptaPeriode.PaiementCompta(999L, debut, "Chèque", 22), "comptable"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.deletePaiement(7L, 999L, "comptable"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(service.adhesionRepository, never()).save(any());
        verifyNoInteractions(service.adhesionServices);
    }

    @Test
    void rejectsIncompletePaymentWritesBeforeReadingAnAdhesion() {
        for (ComptaPeriode.PaiementCompta invalid : List.of(
                new ComptaPeriode.PaiementCompta(null, null, "Chèque", 22),
                new ComptaPeriode.PaiementCompta(null, debut, "Chèque", null),
                new ComptaPeriode.PaiementCompta(null, debut, "Inconnu", 22))) {
            assertThatThrownBy(() -> service.savePaiement(7L, invalid, "comptable"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(service.adhesionRepository, service.adhesionServices);
    }

    @Test
    void showsEveryInstallmentAndIncompletePaymentWhenOnlyOneContributes() {
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion(activite, true,
                payment(debut.minusDays(1), "Chèque 3X", 90),
                payment(debut, "Chèque 3X", 100),
                payment(fin.plusDays(1), "Chèque 3X", 110),
                payment(null, null, null))));

        ComptaPeriode result = service.getPeriode(debut, fin);
        assertThat(result.adhesions()).singleElement().satisfies(row -> {
            assertThat(row.paiements()).hasSize(4);
            assertThat(row.paiements()).extracting(ComptaPeriode.PaiementCompta::inclusDansPeriode)
                    .containsExactly(false, true, false, false);
            assertThat(row.paiements().getLast().dateReglement()).isNull();
            assertThat(row.totalPeriode()).isEqualTo(100);
        });
        assertThat(result.recap().getFirst().getCheque3x()).isEqualTo(100);
    }

    @Test
    void recalculatesAfterMovingPaymentsIntoAndOutOfThePeriodWithoutHidingTheOtherPayments() {
        Paiement current = payment(debut, "HelloAsso", 10);
        current.setId(8L);
        Paiement previous = payment(debut.minusDays(1), "HelloAsso", 100);
        previous.setId(9L);
        Adhesion adhesion = adhesion(activite, true, current, previous);
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion));
        ComptaPeriode initial = service.getPeriode(debut, fin);
        assertThat(initial.adhesions().getFirst().paiements()).hasSize(2);
        assertThat(initial.adhesions().getFirst().totalPeriode()).isEqualTo(10);

        service.savePaiement(7L, new ComptaPeriode.PaiementCompta(9L, fin, "HelloAsso", 100), "comptable");
        assertThat(service.getPeriode(debut, fin).recap().getFirst().getHelloAsso()).isEqualTo(110);
        service.savePaiement(7L, new ComptaPeriode.PaiementCompta(8L, fin.plusDays(1), "HelloAsso", 10), "comptable");
        ComptaPeriode updated = service.getPeriode(debut, fin);
        assertThat(updated.adhesions().getFirst().paiements()).hasSize(2);
        assertThat(updated.adhesions().getFirst().totalPeriode()).isEqualTo(100);
        assertThat(updated.recap().getFirst().getHelloAsso()).isEqualTo(100);
        assertThat(updated.adhesions().getFirst().paiements()).filteredOn(ComptaPeriode.PaiementCompta::inclusDansPeriode)
                .extracting(ComptaPeriode.PaiementCompta::id).containsExactly(9L);
    }

    @Test
    void persistsReconciliationChoicesWithoutChangingPaymentsOrTotals() {
        Adhesion adhesion = adhesion(activite, true, payment(debut, "HelloAsso", 10));
        when(service.adhesionRepository.findById(7L)).thenReturn(Optional.of(adhesion));
        when(service.adhesionServices.getAll()).thenReturn(List.of(adhesion));
        assertThat(service.getPeriode(debut, fin).adhesions().getFirst().rapprochement()).isNull();
        for (boolean value : List.of(true, false)) {
            service.updateRapprochement(7L, value, "comptable");
            assertThat(adhesion.getRapprochement()).isEqualTo(value);
            ComptaPeriode result = service.getPeriode(debut, fin);
            assertThat(result.adhesions().getFirst().rapprochement()).isEqualTo(value);
            assertThat(result.adhesions().getFirst().totalPeriode()).isEqualTo(10);
            assertThat(result.recap().getFirst().getHelloAsso()).isEqualTo(10);
            assertThat(adhesion.getPaiements()).hasSize(1);
        }
        verify(service.adhesionRepository, times(2)).save(adhesion);
        verify(service.adhesionServices).addModification("comptable", 7L, "Rapprochement depuis la comptabilité : Oui");
        verify(service.adhesionServices).addModification("comptable", 7L, "Rapprochement depuis la comptabilité : Non");
    }

    @Test
    void rejectsReconciliationOfAMissingAdhesionWithoutWriting() {
        assertThatThrownBy(() -> service.updateRapprochement(999L, true, "comptable"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(service.adhesionRepository, never()).save(any());
        verifyNoInteractions(service.adhesionServices);
    }

    private static Activite activity(String nom) {
        Activite result = new Activite();
        result.setNom(nom);
        return result;
    }

    private static Adhesion adhesion(Activite activite, Boolean validated, Paiement... payments) {
        Adhesion result = new Adhesion();
        result.setActivite(activite);
        result.setValidPaiementSecretariat(validated);
        result.setPaiements(new ArrayList<>(Arrays.asList(payments)));
        return result;
    }

    private static Paiement payment(LocalDate date, String type, Integer montant) {
        Paiement result = new Paiement();
        result.setDateReglement(date);
        result.setTypeReglement(type);
        result.setMontant(montant);
        return result;
    }
}
