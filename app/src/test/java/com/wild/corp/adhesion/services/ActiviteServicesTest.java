package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiviteServicesTest {

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private SeanceServices seanceServices;

    @Mock
    private AdherentServices adherentServices;

    @InjectMocks
    private ActiviteServices activiteServices;

    @Test
    void updatesAnActivityWithoutReplacingOrDeletingItsSessions() {
        Activite activiteInDB = new Activite();
        activiteInDB.setId(12L);
        activiteInDB.setNom("Ancien nom");
        activiteInDB.setJour(DayOfWeek.TUESDAY);

        Seance seance = new Seance();
        seance.setId(31L);
        seance.setActivite(activiteInDB);
        seance.setDebut(LocalDateTime.of(2026, 9, 1, 8, 15));
        seance.setFin(LocalDateTime.of(2026, 9, 1, 9, 0));
        activiteInDB.getSeances().add(seance);

        Activite modification = new Activite();
        modification.setId(12L);
        modification.setNom("Nouveau nom");
        modification.setJour(DayOfWeek.WEDNESDAY);

        when(activiteRepository.findById(12L)).thenReturn(Optional.of(activiteInDB));
        when(activiteRepository.save(activiteInDB)).thenReturn(activiteInDB);

        Activite resultat = activiteServices.save(modification);

        assertThat(resultat.getNom()).isEqualTo("Nouveau nom");
        assertThat(resultat.getJour()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(resultat.getSeances()).containsExactly(seance);
        verify(seanceServices, never()).modifyDay(activiteInDB);
        verify(activiteRepository).save(activiteInDB);
    }
}
