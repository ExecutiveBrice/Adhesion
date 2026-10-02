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
    private SectionConfigurationServices service;

    @BeforeEach
    void setUp() {
        sectionRepository = mock(SectionRepository.class);
        service = new SectionConfigurationServices(sectionRepository);
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

        assertThat(section).isEqualTo(new SectionConfiguration(12L, "Basket loisirs", "NON_COMPETITIVE"));
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

        assertThat(section).isEqualTo(new SectionConfiguration(12L, "Basket compétition", "COMPETITION"));
    }
}
