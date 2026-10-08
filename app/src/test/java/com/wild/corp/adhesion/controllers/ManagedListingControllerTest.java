package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.AdhesionServices;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ManagedListingControllerTest {
    private final SectionRepository sections = mock(SectionRepository.class);
    private final AdhesionServices adhesionServices = mock(AdhesionServices.class);
    private final AdherentServices adherentServices = mock(AdherentServices.class);
    private final AdhesionController adhesions = new AdhesionController();
    private final AdherentController adherents = new AdherentController();
    private final UsernamePasswordAuthenticationToken user =
            new UsernamePasswordAuthenticationToken("referent@example.test", "");

    ManagedListingControllerTest() {
        ReflectionTestUtils.setField(adhesions, "sectionRepository", sections);
        ReflectionTestUtils.setField(adhesions, "adhesionServices", adhesionServices);
        ReflectionTestUtils.setField(adherents, "sectionRepository", sections);
        ReflectionTestUtils.setField(adherents, "adherentServices", adherentServices);
    }

    @Test
    void rejectsAccountsWithoutManagedSections() {
        assertThatThrownBy(() -> adhesions.getManagedPage(user, "Toutes", "", "", null, null, null,
                Pageable.unpaged())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThatThrownBy(() -> adherents.getManagedPage(user, "", "", "", Pageable.unpaged()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(adhesionServices, adherentServices);
    }

    @Test
    void passesOnlyAssignedSectionIdsToBothLists() {
        when(sections.findManagedByUsername(user.getName())).thenReturn(List.of(
                Section.builder().id(4L).build(), Section.builder().id(9L).build()));

        adhesions.getManagedPage(user, "Toutes", "", "", null, null, null, Pageable.unpaged());
        adherents.getManagedPage(user, "", "", "", Pageable.unpaged());

        verify(adhesionServices).getAllLite("Toutes", "", "", null, null, null,
                Pageable.unpaged(), Set.of(4L, 9L));
        verify(adherentServices).getPage("", "", "", Pageable.unpaged(), Set.of(4L, 9L));
    }
}
