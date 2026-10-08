package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.resources.SectionConfiguration;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.utils.Status;
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
    private final UserRepository users;
    private final AdherentRepository adherents;
    private static final List<String> STATUTS_EXCLUS = List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label);

    public SectionConfigurationServices(SectionRepository sectionRepository, UserRepository users,
                                        AdherentRepository adherents) {
        this.sectionRepository = sectionRepository;
        this.users = users;
        this.adherents = adherents;
    }

    public record ReferentCandidate(Long id, String nom) {}

    @Transactional
    public List<ReferentCandidate> getReferentCandidates(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable");
        }
        return adherents.findReferentCandidates(sectionId, STATUTS_EXCLUS).stream()
                .filter(adherent -> adherent.getUser() != null)
                .map(adherent -> new ReferentCandidate(adherent.getUser().getId(),
                        (adherent.getPrenom() + " " + adherent.getNom()).trim()))
                .distinct().toList();
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
        Section section = Section.builder().nom(normalisee.nom()).type(normalisee.type())
                .couleur(normalisee.couleur() != null ? normalisee.couleur() : Section.COULEUR_PAR_DEFAUT).build();
        appliquerReferents(section, configuration.referentUserIds());
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
        // Preserve the selected color when an older client omits it.
        if (normalisee.couleur() != null) section.setCouleur(normalisee.couleur());
        appliquerReferents(section, configuration.referentUserIds());
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
        String couleur = configuration.couleur() == null ? null : nettoyer(configuration.couleur()).toUpperCase(Locale.ROOT);
        if (couleur != null && !couleur.matches("#[0-9A-F]{6}")) {
            throw configurationInvalide("La couleur de la section doit être au format #RRGGBB");
        }
        return new SectionConfiguration(null, nom, type, null, couleur);
    }

    private String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private SectionConfiguration toConfiguration(Section section) {
        return new SectionConfiguration(section.getId(), section.getNom(), section.getType(),
                section.getReferents().stream().map(com.wild.corp.adhesion.models.User::getId).sorted().toList(),
                section.getCouleur());
    }

    private void appliquerReferents(Section section, List<Long> referentUserIds) {
        // Older clients that omit this field must preserve existing assignments.
        if (referentUserIds == null) return;
        if (referentUserIds.stream().anyMatch(id -> id == null || id <= 0)
                || new java.util.HashSet<>(referentUserIds).size() != referentUserIds.size()) {
            throw configurationInvalide("Les référents de section sont invalides");
        }
        var referents = users.findAllById(referentUserIds);
        if (referents.size() != referentUserIds.size()) {
            throw configurationInvalide("Un référent de section est introuvable");
        }
        Set<Long> candidats = adherents.findReferentCandidates(section.getId(), STATUTS_EXCLUS).stream()
                .filter(adherent -> adherent.getUser() != null)
                .map(adherent -> adherent.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (referents.stream().anyMatch(referent -> !candidats.contains(referent.getId()))) {
            throw configurationInvalide("Un référent doit appartenir à une tribu inscrite dans cette section");
        }
        section.getReferents().clear();
        section.getReferents().addAll(referents);
    }

    private ResponseStatusException configurationInvalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
