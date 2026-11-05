package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdhesionConsentServicesTest {
    @ParameterizedTest
    @ValueSource(strings = {"missing", "unsaved", "partiallySaved", "rgpdRefused"})
    void refusesAddingAnActivityBeforeBothAccordsAreSaved(String situation) {
        Adherent adherent = new Adherent();
        adherent.setCompletAdhesion(true); // A stale completion flag must not bypass consent checks.
        if (!"missing".equals(situation)) {
            Accord rgpd = new Accord("RGPD", "", "", "", false, "");
            Accord image = new Accord("DroitImage", "", "", "", true, "");
            if (!"unsaved".equals(situation)) rgpd.setDatePassage(LocalDate.now());
            if ("rgpdRefused".equals(situation)) {
                rgpd.setEtat(false);
                image.setDatePassage(LocalDate.now());
            }
            adherent.getAccords().add(rgpd);
            adherent.getAccords().add(image);
        }
        AdherentServices adherents = mock(AdherentServices.class);
        when(adherents.getById(42L)).thenReturn(adherent);
        when(adherents.hasValidatedAccords(adherent)).thenCallRealMethod();
        ActiviteServices activites = mock(ActiviteServices.class);
        AdhesionRepository repository = mock(AdhesionRepository.class);
        AdhesionServices service = new AdhesionServices();
        ReflectionTestUtils.setField(service, "adherentServices", adherents);
        ReflectionTestUtils.setField(service, "activiteServices", activites);
        ReflectionTestUtils.setField(service, "adhesionRepository", repository);

        assertThatThrownBy(() -> service.save(() -> "alice@example.test", 42L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Enregistrez les accords");
        verifyNoInteractions(activites, repository);
    }
}
