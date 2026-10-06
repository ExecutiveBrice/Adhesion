package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.ResponsabiliteSeanceRequest;
import com.wild.corp.adhesion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResponsabiliteSeanceServicesTest {
    private final AdherentRepository adherents = mock(AdherentRepository.class);
    private final TacheSeanceRepository taches = mock(TacheSeanceRepository.class);
    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final ResponsabiliteSeanceServices service = new ResponsabiliteSeanceServices(adherents, taches, activites);
    private final Seance seance = new Seance();
    private final TacheSeance tache = new TacheSeance();
    private final Adherent adherent = new Adherent();

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setId(7L);
        Activite activite = new Activite();
        activite.setSection(section);
        seance.setActivite(activite);
        tache.setId(3L);
        tache.setNom("Matériel");
        adherent.setId(12L);
        when(taches.findById(3L)).thenReturn(Optional.of(tache));
        when(adherents.findResponsabiliteCandidate(eq(12L), eq(7L), anyList())).thenReturn(Optional.of(adherent));
    }

    @Test
    void assignsMemberOfTheSectionAndAllowsSeveralTasksOrMembers() {
        TacheSeance autreTache = new TacheSeance();
        autreTache.setId(4L);
        when(taches.findById(4L)).thenReturn(Optional.of(autreTache));
        Adherent autreAdherent = new Adherent();
        autreAdherent.setId(13L);
        when(adherents.findResponsabiliteCandidate(eq(13L), eq(7L), anyList())).thenReturn(Optional.of(autreAdherent));
        service.remplacer(seance, List.of(new ResponsabiliteSeanceRequest(3L, 12L),
                new ResponsabiliteSeanceRequest(4L, 12L), new ResponsabiliteSeanceRequest(3L, 13L)));
        assertThat(seance.getResponsabilites()).hasSize(3);
        assertThat(seance.getResponsabilites().getFirst().getAdherent()).isSameAs(adherent);
    }

    @Test
    void omissionPreservesAssignmentsAndEmptyListClearsThem() {
        seance.getResponsabilites().add(new ResponsabiliteSeance(tache, adherent));
        service.remplacer(seance, null);
        assertThat(seance.getResponsabilites()).hasSize(1);
        service.remplacer(seance, List.of());
        assertThat(seance.getResponsabilites()).isEmpty();
    }

    @Test
    void rejectsNonMembersUnknownTasksAndDuplicatesWithoutChangingExistingAssignments() {
        seance.getResponsabilites().add(new ResponsabiliteSeance(tache, adherent));
        var valide = new ResponsabiliteSeanceRequest(3L, 12L);
        for (var invalide : List.of(new ResponsabiliteSeanceRequest(3L, 99L),
                new ResponsabiliteSeanceRequest(99L, 12L), valide)) {
            assertThatThrownBy(() -> service.remplacer(seance, List.of(valide, invalide)))
                    .isInstanceOf(ResponseStatusException.class);
            assertThat(seance.getResponsabilites()).hasSize(1);
        }
    }
}
