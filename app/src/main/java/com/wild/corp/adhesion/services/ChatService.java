package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import com.wild.corp.adhesion.utils.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.wild.corp.adhesion.domain.chat.ChatAccessPolicy.canRead;
import static com.wild.corp.adhesion.domain.chat.ChatAccessPolicy.canWrite;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private static final List<String> EXCLUDED_MEMBERSHIP_STATUSES = List.of(
            Status.LISTE_ATTENTE.label, Status.ANNULEE.label);
    private final UserRepository users;
    private final ActiviteRepository activities;
    private final ChatMessageRepository messages;
    private final ChatReadRepository reads;
    private final ChatRepository chats;

    public record Room(Long id, String nom, String groupeFiltre, List<Category> categories,
                       long unreadCount, boolean canWrite) {}
    public record Category(String descriptif, String jour, String horaire) {}
    public record Unread(long unreadCount) {}
    public record Message(Long id, Long senderUserId, String senderName, String senderRole,
                          String content, Instant createdAt) {}

    public List<Room> rooms(String username) {
        User user = currentUser(username);
        List<Activite> linkedActivities = activities.findChatActivities(user.getId(), EXCLUDED_MEMBERSHIP_STATUSES);
        List<Chat> accessible = chats.findAllByOrderByNomAsc().stream()
                .filter(chat -> canRead(user, chat, linkedActivities)).toList();
        var counts = reads.unreadCounts(user.getId(), accessible.stream().map(Chat::getId).toList());
        return accessible.stream().map(chat -> new Room(chat.getId(), chat.getNom(), chat.targetLabel(),
                List.of(), counts.getOrDefault(chat.getId(), 0L), canWrite(user, chat))).toList();
    }

    public List<Message> history(String username, Long chatId, Long beforeId, Long afterId) {
        Chat chat = requireChat(currentUser(username), chatId, false);
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)
                || (beforeId != null && afterId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide");
        }
        var page = PageRequest.of(0, 50);
        List<ChatMessage> result = new ArrayList<>(afterId != null
                ? messages.findByChatIdAndIdGreaterThanOrderByIdAsc(chat.getId(), afterId, page)
                : beforeId != null
                ? messages.findByChatIdAndIdLessThanOrderByIdDesc(chat.getId(), beforeId, page)
                : messages.findByChatIdOrderByIdDesc(chat.getId(), page));
        if (afterId == null) Collections.reverse(result);
        return result.stream().map(this::response).toList();
    }

    @Transactional
    public Message send(String username, Long chatId, String content) {
        User user = currentUser(username);
        Chat chat = requireChat(user, chatId, true);
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message doit contenir entre 1 et 2000 caractères");
        }
        Adherent author = user.getAdherent();
        String name = author == null ? "" : ((author.getPrenom() == null ? "" : author.getPrenom()) + " "
                + (author.getNom() == null ? "" : author.getNom())).trim();
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderUserId(user.getId());
        message.setSenderName(name.isBlank() ? "Adhérent" : name.substring(0, Math.min(name.length(), 255)));
        message.setSenderRole(displayRole(user, chat));
        message.setContent(content.strip());
        message.setCreatedAt(Instant.now());
        return response(messages.save(message));
    }

    @Transactional
    public Unread markRead(String username, Long chatId, Long messageId) {
        User user = currentUser(username);
        Chat chat = requireChat(user, chatId, false);
        if (messageId == null || messageId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message invalide");
        }
        ChatMessage message = messages.findById(messageId)
                .filter(item -> item.getChat().getId().equals(chat.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message invalide"));
        reads.markRead(user.getId(), chat.getId(), message.getId());
        return new Unread(reads.unreadCounts(user.getId(), List.of(chat.getId())).getOrDefault(chat.getId(), 0L));
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Chat requireChat(User user, Long chatId, boolean writing) {
        Chat chat = chats.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable"));
        List<Activite> linkedActivities = chat.getCible() == ChatTarget.ASSOCIATION
                ? List.of() : activities.findChatActivities(user.getId(), EXCLUDED_MEMBERSHIP_STATUSES);
        if (!canRead(user, chat, linkedActivities) || (writing && !canWrite(user, chat))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’êtes pas autorisé à accéder à ce chat");
        }
        return chat;
    }

    private String displayRole(User user, Chat chat) {
        // The visual precedence is intentional: it follows the requested order.
        if (user.getRoles().contains(ERole.ROLE_SECRETAIRE)) return "SECRETAIRE";
        if (user.getRoles().contains(ERole.ROLE_MEMBRECA)) return "MEMBRECA";
        if (user.getRoles().contains(ERole.ROLE_ENCADRANT)) return "ENCADRANT";
        if (user.getAdherent() != null && chat.getCible() == ChatTarget.ACTIVITE
                && chat.getActivite().getReferents().stream().anyMatch(ref -> ref.getId().equals(user.getAdherent().getId())))
            return "REFERENT_ACTIVITE";
        Section section = chat.getCible() == ChatTarget.SECTION ? chat.getSection()
                : chat.getCible() == ChatTarget.ACTIVITE ? chat.getActivite().getSection() : null;
        if (section != null && section.getReferents().stream().anyMatch(ref -> ref.getId().equals(user.getId())))
            return "REFERENT_SECTION";
        if (user.getRoles().contains(ERole.ROLE_COMMUNICATION_GLOBAL)) return "COMMUNICATION_GLOBAL";
        return null;
    }

    private Message response(ChatMessage message) {
        return new Message(message.getId(), message.getSenderUserId(), message.getSenderName(), message.getSenderRole(),
                message.getContent(), message.getCreatedAt());
    }
}
