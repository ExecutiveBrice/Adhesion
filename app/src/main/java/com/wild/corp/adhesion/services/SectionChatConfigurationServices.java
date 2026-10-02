package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.SectionChat;
import com.wild.corp.adhesion.models.SectionChatPermission;
import com.wild.corp.adhesion.models.resources.SectionChatConfiguration;
import com.wild.corp.adhesion.repository.SectionChatRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SectionChatConfigurationServices {

    private final SectionRepository sectionRepository;
    private final ActiviteRepository activiteRepository;
    private final SectionChatRepository sectionChatRepository;

    public SectionChatConfigurationServices(SectionRepository sectionRepository, ActiviteRepository activiteRepository,
                                            SectionChatRepository sectionChatRepository) {
        this.sectionRepository = sectionRepository;
        this.activiteRepository = activiteRepository;
        this.sectionChatRepository = sectionChatRepository;
    }

    @Transactional
    public List<SectionChatConfiguration> getAll(Long sectionId) {
        return sectionChatRepository.findAllBySectionIdOrderByNomAsc(sectionId).stream().map(this::toConfiguration).toList();
    }

    @Transactional
    public List<SectionChatConfiguration> getAll() {
        return sectionChatRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing((SectionChat chat) -> cibleNom(chat), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SectionChat::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(this::toConfiguration).toList();
    }

    @Transactional
    public SectionChatConfiguration create(SectionChatConfiguration configuration) {
        SectionChatConfiguration normalisee = normaliser(configuration);
        SectionChat chat = new SectionChat();
        appliquerCible(chat, normalisee);
        verifierNomUnique(chat, normalisee.nom());
        chat.setNom(normalisee.nom());
        appliquerPermissions(chat, normalisee);
        return toConfiguration(sectionChatRepository.save(chat));
    }

    @Transactional
    public SectionChatConfiguration update(Long sectionId, Long chatId, SectionChatConfiguration configuration) {
        SectionChat chat = requireChat(sectionId, chatId);
        return update(chat, configuration);
    }

    @Transactional
    public void delete(Long sectionId, Long chatId) {
        sectionChatRepository.delete(requireChat(sectionId, chatId));
    }

    @Transactional
    public SectionChatConfiguration update(Long chatId, SectionChatConfiguration configuration) {
        return update(sectionChatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable")), configuration);
    }

    @Transactional
    public void delete(Long chatId) {
        sectionChatRepository.delete(sectionChatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable")));
    }

    private SectionChatConfiguration update(SectionChat chat, SectionChatConfiguration configuration) {
        SectionChatConfiguration normalisee = normaliser(configuration);
        appliquerCible(chat, normalisee);
        verifierNomUnique(chat, normalisee.nom());
        chat.setNom(normalisee.nom());
        appliquerPermissions(chat, normalisee);
        return toConfiguration(sectionChatRepository.save(chat));
    }

    private SectionChat requireChat(Long sectionId, Long chatId) {
        SectionChat chat = sectionChatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable"));
        if (chat.getCible() != ChatTarget.SECTION || chat.getSection() == null || !chat.getSection().getId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable dans cette section");
        }
        return chat;
    }

    private SectionChatConfiguration normaliser(SectionChatConfiguration configuration) {
        if (configuration == null) {
            throw invalide("Les informations du chat sont obligatoires");
        }
        String nom = configuration.nom() == null ? "" : configuration.nom().trim();
        if (nom.isBlank() || nom.length() > 100) {
            throw invalide("Le nom du chat est obligatoire et limité à 100 caractères");
        }
        List<SectionChatConfiguration.RolePermission> permissions = configuration.permissions() == null ? List.of()
                : configuration.permissions().stream().map(permission -> {
                    if (permission == null || permission.role() == null) {
                        throw invalide("Chaque visibilité doit être associée à un rôle");
                    }
                    String role = permission.role().trim().toUpperCase(Locale.ROOT);
                    try {
                        ERole.valueOf(role);
                    } catch (IllegalArgumentException exception) {
                        throw invalide("Le rôle de visibilité est invalide");
                    }
                    return new SectionChatConfiguration.RolePermission(role, permission.ecriture());
                }).toList();
        if (permissions.isEmpty()) {
            throw invalide("Sélectionnez au moins un rôle pouvant lire ce chat");
        }
        if (permissions.stream().map(SectionChatConfiguration.RolePermission::role).collect(java.util.stream.Collectors.toSet()).size()
                != permissions.size()) {
            throw invalide("Un rôle ne peut être configuré qu’une seule fois");
        }
        ChatTarget cible;
        try {
            cible = ChatTarget.valueOf((configuration.cible() == null ? "" : configuration.cible()).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalide("Le rattachement du chat est invalide");
        }
        if (cible != ChatTarget.ASSOCIATION && configuration.cibleId() == null) {
            throw invalide("Sélectionnez une section ou une activité pour ce chat");
        }
        return new SectionChatConfiguration(null, nom, cible.name(), configuration.cibleId(), null, permissions);
    }

    private SectionChatConfiguration toConfiguration(SectionChat chat) {
        Long cibleId = chat.getCible() == ChatTarget.SECTION ? chat.getSection().getId()
                : chat.getCible() == ChatTarget.ACTIVITE ? chat.getActivite().getId() : null;
        return new SectionChatConfiguration(chat.getId(), chat.getNom(), chat.getCible().name(), cibleId, cibleNom(chat), chat.getPermissions().stream()
                .map(permission -> new SectionChatConfiguration.RolePermission(permission.getRole(), permission.isEcriture()))
                .toList());
    }

    private void appliquerCible(SectionChat chat, SectionChatConfiguration configuration) {
        ChatTarget cible = ChatTarget.valueOf(configuration.cible());
        chat.setCible(cible);
        chat.setSection(null);
        chat.setActivite(null);
        if (cible == ChatTarget.SECTION) {
            Section section = sectionRepository.findById(configuration.cibleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable"));
            chat.setSection(section);
        } else if (cible == ChatTarget.ACTIVITE) {
            Activite activite = activiteRepository.findById(configuration.cibleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
            chat.setActivite(activite);
        }
    }

    private void appliquerPermissions(SectionChat chat, SectionChatConfiguration configuration) {
        chat.getPermissions().clear();
        configuration.permissions().forEach(permission -> {
            SectionChatPermission entite = new SectionChatPermission();
            entite.setChat(chat);
            entite.setRole(permission.role());
            entite.setEcriture(permission.ecriture());
            chat.getPermissions().add(entite);
        });
    }

    private void verifierNomUnique(SectionChat chat, String nom) {
        boolean existe = sectionChatRepository.findAll().stream().anyMatch(autre -> !autre.getId().equals(chat.getId())
                && autre.getCible() == chat.getCible() && memeCible(autre, chat)
                && autre.getNom().equalsIgnoreCase(nom));
        if (existe) {
            throw invalide("Un chat portant ce nom existe déjà pour ce rattachement");
        }
    }

    private boolean memeCible(SectionChat premier, SectionChat second) {
        if (premier.getCible() == ChatTarget.ASSOCIATION) return true;
        if (premier.getCible() == ChatTarget.SECTION) return premier.getSection().getId().equals(second.getSection().getId());
        return premier.getActivite().getId().equals(second.getActivite().getId());
    }

    private String cibleNom(SectionChat chat) {
        return switch (chat.getCible()) {
            case SECTION -> "Section · " + chat.getSection().getNom();
            case ACTIVITE -> "Activité · " + chat.getActivite().getNom();
            case ASSOCIATION -> "Association";
        };
    }

    private ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
