package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.resources.SectionConfiguration;
import com.wild.corp.adhesion.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectionConfigurationServicesTest {

    private SectionRepository sectionRepository;
    private com.wild.corp.adhesion.repository.UserRepository users;
    private SectionConfigurationServices service;

    @BeforeEach
    void setUp() {
        sectionRepository = mock(SectionRepository.class);
        users = mock(com.wild.corp.adhesion.repository.UserRepository.class);
        service = new SectionConfigurationServices(sectionRepository, users);
    }

    @Test
    void createsASectionWithNormalizedValues() {
        when(sectionRepository.existsByNomIgnoreCase(anyString())).thenReturn(false);
        when(sectionRepository.save(any())).thenAnswer(invocation -> {
            Section section = invocation.getArgument(0);
            section.setId(12L);
            return section;
        });

        SectionConfiguration section = service.create(new SectionConfiguration(
                null, "  Basket loisirs ", " non_competitive "));

        assertThat(section).isEqualTo(new SectionConfiguration(12L, "Basket loisirs", "NON_COMPETITIVE", java.util.List.of()));
    }

    @Test
    void rejectsAnUnknownType() {
        assertThatThrownBy(() -> service.create(new SectionConfiguration(null, "Basket", "LOISIR")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("type de section");
    }

    @Test
    void updatesTheNameAndTypeOfASection() {
        Section existante = Section.builder().id(12L).nom("Basket loisirs").type("NON_COMPETITIVE").build();
        when(sectionRepository.findById(12L)).thenReturn(java.util.Optional.of(existante));
        when(sectionRepository.existsByNomIgnoreCaseAndIdNot("Basket compétition", 12L)).thenReturn(false);
        when(sectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SectionConfiguration section = service.update(12L, new SectionConfiguration(
                12L, " Basket compétition ", "competition"));

        assertThat(section).isEqualTo(new SectionConfiguration(12L, "Basket compétition", "COMPETITION", java.util.List.of()));
    }

    @Test
    void assignsAndRevokesSectionReferentsWhilePreservingAssignmentsForOlderClients() {
        Section section = Section.builder().id(12L).nom("Basket").type("COMPETITION").build();
        var referent = new com.wild.corp.adhesion.models.User("referent", "password");
        referent.setId(4L);
        when(sectionRepository.findById(12L)).thenReturn(java.util.Optional.of(section));
        when(sectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findAllById(java.util.List.of(4L))).thenReturn(java.util.List.of(referent));
        assertThat(service.update(12L, new SectionConfiguration(12L, "Basket", "COMPETITION", java.util.List.of(4L)))
                .referentUserIds()).containsExactly(4L);
        assertThat(service.update(12L, new SectionConfiguration(12L, "Basket", "COMPETITION"))
                .referentUserIds()).containsExactly(4L);
        assertThat(service.update(12L, new SectionConfiguration(12L, "Basket", "COMPETITION", java.util.List.of()))
                .referentUserIds()).isEmpty();
    }

    @Test
    void rejectsMissingOrDuplicateSectionReferents() {
        when(sectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        for (var ids : java.util.List.of(java.util.List.of(4L), java.util.List.of(4L, 4L), java.util.List.of(0L))) {
            assertThatThrownBy(() -> service.create(new SectionConfiguration(null, "Basket", "COMPETITION", ids)))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
