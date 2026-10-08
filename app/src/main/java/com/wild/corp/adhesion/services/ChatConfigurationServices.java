package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.Chat;
import com.wild.corp.adhesion.models.ChatPermission;
import com.wild.corp.adhesion.models.resources.ChatConfiguration;
import com.wild.corp.adhesion.repository.ChatRepository;
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
public class ChatConfigurationServices {

    private final SectionRepository sectionRepository;
    private final ActiviteRepository activiteRepository;
    private final ChatRepository chatRepository;

    public ChatConfigurationServices(SectionRepository sectionRepository, ActiviteRepository activiteRepository,
                                            ChatRepository chatRepository) {
        this.sectionRepository = sectionRepository;
        this.activiteRepository = activiteRepository;
        this.chatRepository = chatRepository;
    }

    @Transactional
    public List<ChatConfiguration> getAll(Long sectionId) {
        return chatRepository.findAllBySectionIdOrderByNomAsc(sectionId).stream()
                .filter(chat -> chat.getCible() == ChatTarget.SECTION)
                .map(this::toConfiguration).toList();
    }

    @Transactional
    public List<ChatConfiguration> getAll() {
        return chatRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing((Chat chat) -> cibleNom(chat), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Chat::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(this::toConfiguration).toList();
    }

    @Transactional
    public ChatConfiguration create(ChatConfiguration configuration) {
        ChatConfiguration normalisee = normaliser(configuration);
        Chat chat = new Chat();
        appliquerCible(chat, normalisee);
        verifierNomUnique(chat, normalisee.nom());
        chat.setNom(normalisee.nom());
        appliquerPermissions(chat, normalisee);
        return toConfiguration(chatRepository.save(chat));
    }

    @Transactional
    public ChatConfiguration update(Long sectionId, Long chatId, ChatConfiguration configuration) {
        Chat chat = requireChat(sectionId, chatId);
        return update(chat, configuration);
    }

    @Transactional
    public void delete(Long sectionId, Long chatId) {
        chatRepository.delete(requireChat(sectionId, chatId));
    }

    @Transactional
    public ChatConfiguration update(Long chatId, ChatConfiguration configuration) {
        return update(chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable")), configuration);
    }

    @Transactional
    public void delete(Long chatId) {
        chatRepository.delete(chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable")));
    }

    private ChatConfiguration update(Chat chat, ChatConfiguration configuration) {
        ChatConfiguration normalisee = normaliser(configuration);
        appliquerCible(chat, normalisee);
        verifierNomUnique(chat, normalisee.nom());
        chat.setNom(normalisee.nom());
        appliquerPermissions(chat, normalisee);
        return toConfiguration(chatRepository.save(chat));
    }

    private Chat requireChat(Long sectionId, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable"));
        if (chat.getCible() != ChatTarget.SECTION || chat.getSection() == null || !chat.getSection().getId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable dans cette section");
        }
        return chat;
    }

    private ChatConfiguration normaliser(ChatConfiguration configuration) {
        if (configuration == null) {
            throw invalide("Les informations du chat sont obligatoires");
        }
        String nom = configuration.nom() == null ? "" : configuration.nom().trim();
        if (nom.isBlank() || nom.length() > 100) {
            throw invalide("Le nom du chat est obligatoire et limité à 100 caractères");
        }
        List<ChatConfiguration.RolePermission> permissions = configuration.permissions() == null ? List.of()
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
                    return new ChatConfiguration.RolePermission(role, permission.ecriture());
                }).toList();
        if (permissions.stream().map(ChatConfiguration.RolePermission::role).collect(java.util.stream.Collectors.toSet()).size()
                != permissions.size()) {
            throw invalide("Un rôle ne peut être configuré qu’une seule fois");
        }
        ChatTarget cible;
        try {
            cible = ChatTarget.valueOf((configuration.cible() == null ? "" : configuration.cible()).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalide("Le rattachement du chat est invalide");
        }
        if (cible != ChatTarget.ASSOCIATION && (configuration.cibleId() == null || configuration.cibleId() <= 0)) {
            throw invalide("Sélectionnez une section ou une activité pour ce chat");
        }
        if (cible == ChatTarget.ASSOCIATION && configuration.cibleId() != null) {
            throw invalide("Un chat global ne peut pas être rattaché à une section ou une activité");
        }
        if (cible == ChatTarget.ASSOCIATION && permissions.isEmpty()) {
            throw invalide("Sélectionnez au moins un rôle pouvant lire ce chat global");
        }
        return new ChatConfiguration(null, nom, cible.name(), configuration.cibleId(), null, permissions);
    }

    private ChatConfiguration toConfiguration(Chat chat) {
        Long cibleId = chat.getCible() == ChatTarget.SECTION ? chat.getSection().getId()
                : chat.getCible() == ChatTarget.ACTIVITE ? chat.getActivite().getId() : null;
        return new ChatConfiguration(chat.getId(), chat.getNom(), chat.getCible().name(), cibleId, cibleNom(chat), chat.getPermissions().stream()
                .map(permission -> new ChatConfiguration.RolePermission(permission.getRole(), permission.isEcriture()))
                .toList());
    }

    private void appliquerCible(Chat chat, ChatConfiguration configuration) {
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

    private void appliquerPermissions(Chat chat, ChatConfiguration configuration) {
        Set<String> roles = configuration.permissions().stream().map(ChatConfiguration.RolePermission::role)
                .collect(java.util.stream.Collectors.toSet());
        chat.getPermissions().removeIf(permission -> !roles.contains(permission.getRole()));
        configuration.permissions().forEach(permission -> {
            ChatPermission entite = chat.getPermissions().stream()
                    .filter(existing -> existing.getRole().equals(permission.role())).findFirst().orElseGet(() -> {
                        ChatPermission created = new ChatPermission();
                        chat.getPermissions().add(created);
                        return created;
                    });
            entite.setChat(chat);
            entite.setRole(permission.role());
            entite.setEcriture(permission.ecriture());
        });
    }

    private void verifierNomUnique(Chat chat, String nom) {
        boolean existe = chatRepository.findAll().stream().anyMatch(autre -> !autre.getId().equals(chat.getId())
                && autre.getCible() == chat.getCible() && memeCible(autre, chat)
                && autre.getNom().equalsIgnoreCase(nom));
        if (existe) {
            throw invalide("Un chat portant ce nom existe déjà pour ce rattachement");
        }
    }

    private boolean memeCible(Chat premier, Chat second) {
        if (premier.getCible() == ChatTarget.ASSOCIATION) return true;
        if (premier.getCible() == ChatTarget.SECTION) return premier.getSection().getId().equals(second.getSection().getId());
        return premier.getActivite().getId().equals(second.getActivite().getId());
    }

    private String cibleNom(Chat chat) {
        return chat.targetLabel();
    }

    private ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
