package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.TacheSeance;
import com.wild.corp.adhesion.models.resources.TacheSeanceConfiguration;
import com.wild.corp.adhesion.repository.TacheSeanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TacheSeanceConfigurationServicesTest {
    private final TacheSeanceRepository repository = mock(TacheSeanceRepository.class);
    private final TacheSeanceConfigurationServices service = new TacheSeanceConfigurationServices(repository);

    @Test
    void normalizesNamesAndRejectsBlankOversizedAndDuplicateNames() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(new TacheSeanceConfiguration(null, "  Matériel  ")).nom()).isEqualTo("Matériel");
        for (String nom : new String[] {" ", "a".repeat(101)}) {
            assertThatThrownBy(() -> service.create(new TacheSeanceConfiguration(null, nom)))
                    .isInstanceOf(ResponseStatusException.class);
        }
        when(repository.existsByNomIgnoreCase("matériel")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new TacheSeanceConfiguration(null, "matériel")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void keepsAssignedTasksAndDeletesUnusedTasks() {
        TacheSeance tache = new TacheSeance();
        tache.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(tache));
        when(repository.isUsed(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).delete(any());
        when(repository.isUsed(1L)).thenReturn(false);
        service.delete(1L);
        verify(repository).delete(tache);
    }

    @Test
    void savesIconAndColorAndPreservesThemWhenOlderClientsOmitPresentation() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create(new TacheSeanceConfiguration(null, "Accueil", "handshake", " #aB47bc "));
        assertThat(created.icone()).isEqualTo("handshake");
        assertThat(created.couleur()).isEqualTo("#AB47BC");
        var ancienne = service.create(new TacheSeanceConfiguration(null, "Matériel"));
        assertThat(ancienne.icone()).isEqualTo(TacheSeance.ICONE_PAR_DEFAUT);
        assertThat(ancienne.couleur()).isEqualTo(TacheSeance.COULEUR_PAR_DEFAUT);
        TacheSeance tache = new TacheSeance();
        tache.setId(3L);
        tache.setIcone("handshake");
        tache.setCouleur("#AB47BC");
        when(repository.findById(3L)).thenReturn(Optional.of(tache));
        var renamed = service.update(3L, new TacheSeanceConfiguration(3L, "Accueil visiteurs"));
        assertThat(renamed.icone()).isEqualTo("handshake");
        assertThat(renamed.couleur()).isEqualTo("#AB47BC");
    }

    @Test
    void rejectsUnsupportedIconsAndInvalidColors() {
        for (var configuration : java.util.List.of(
                new TacheSeanceConfiguration(null, "Accueil", "inconnue", "#AB47BC"),
                new TacheSeanceConfiguration(null, "Accueil", "", "#AB47BC"),
                new TacheSeanceConfiguration(null, "Accueil", "handshake", "red"),
                new TacheSeanceConfiguration(null, "Accueil", "handshake", "#ABC"))) {
            assertThatThrownBy(() -> service.create(configuration)).isInstanceOf(ResponseStatusException.class);
        }
        verify(repository, never()).save(any());
    }
}
