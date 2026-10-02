package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.resources.SectionConfiguration;
import com.wild.corp.adhesion.repository.SectionRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SectionConfigurationServices {

    private static final Set<String> TYPES_AUTORISES = Set.of("COMPETITION", "NON_COMPETITIVE");

    private final SectionRepository sectionRepository;

    public SectionConfigurationServices(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Transactional
    public List<SectionConfiguration> getAll() {
        return sectionRepository.findAllByOrderByNomAsc().stream().map(this::toConfiguration).toList();
    }

    @Transactional
    public SectionConfiguration create(SectionConfiguration configuration) {
        SectionConfiguration normalisee = normaliser(configuration);
        if (sectionRepository.existsByNomIgnoreCase(normalisee.nom())) {
            throw configurationInvalide("Une section portant ce nom existe déjà");
        }
        Section section = Section.builder().nom(normalisee.nom()).type(normalisee.type()).build();
        return toConfiguration(sectionRepository.save(section));
    }

    @Transactional
    public SectionConfiguration update(Long id, SectionConfiguration configuration) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable"));
        SectionConfiguration normalisee = normaliser(configuration);
        if (sectionRepository.existsByNomIgnoreCaseAndIdNot(normalisee.nom(), id)) {
            throw configurationInvalide("Une section portant ce nom existe déjà");
        }
        section.setNom(normalisee.nom());
        section.setType(normalisee.type());
        return toConfiguration(sectionRepository.save(section));
    }

    private SectionConfiguration normaliser(SectionConfiguration configuration) {
        if (configuration == null) {
            throw configurationInvalide("Les informations de la section sont obligatoires");
        }
        String nom = nettoyer(configuration.nom());
        String type = nettoyer(configuration.type()).toUpperCase(Locale.ROOT);
        if (nom.isBlank() || nom.length() > 100) {
            throw configurationInvalide("Le nom de la section est obligatoire et limité à 100 caractères");
        }
        if (!TYPES_AUTORISES.contains(type)) {
            throw configurationInvalide("Le type de section doit être COMPETITION ou NON_COMPETITIVE");
        }
        return new SectionConfiguration(null, nom, type);
    }

    private String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private SectionConfiguration toConfiguration(Section section) {
        return new SectionConfiguration(section.getId(), section.getNom(), section.getType());
    }

    private ResponseStatusException configurationInvalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
