package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import com.wild.corp.adhesion.utils.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatService {
    public static final long COMMUNICATION_ROOM_ID = 0L;
    public static final long REFERENT_ENCADRANT_ROOM_ID = -1L;
    private static final List<String> MEMBER_STATUSES = List.of(
            Status.VALIDEE.label, Status.ATTENTE_CERFTIF.label, Status.LICENCE_T.label,
            Status.LICENCE_GENEREE.label, Status.VALIDEE_GROUPEMENT_SPORTIF.label);
    private final UserRepository users;
    private final ActiviteRepository activities;
    private final ChatMessageRepository messages;
    private final ChatReadRepository reads;
    private final SectionChatRepository sectionChats;

    public record Room(Long id, String nom, String groupeFiltre, List<Category> categories, long unreadCount,
                       boolean communication, boolean referentEncadrant, boolean canWrite, Long sectionChatId) {}
    public record Category(String descriptif, String jour, String horaire) {}
    public record Unread(long unreadCount) {}
    public record Message(Long id, Long senderUserId, String senderName, String senderRole,
                          String content, Instant createdAt) {}
    public record SectionRoom(Long id, String nom, String section, long unreadCount, boolean canWrite) {}

    public ChatService(UserRepository users, ActiviteRepository activities, ChatMessageRepository messages,
                       ChatReadRepository reads) {
        this(users, activities, messages, reads, null);
    }

    @Autowired
    public ChatService(UserRepository users, ActiviteRepository activities, ChatMessageRepository messages,
                       ChatReadRepository reads, SectionChatRepository sectionChats) {
        this.users = users;
        this.activities = activities;
        this.messages = messages;
        this.reads = reads;
        this.sectionChats = sectionChats;
    }

    public List<Room> rooms(String username) {
        User user = currentUser(username);
        if (sectionChats == null) return List.of();
        List<SectionChat> accessibles = sectionChats.findAllByOrderByNomAsc().stream()
                .filter(chat -> canReadSectionChat(user, chat)).toList();
        var counts = reads.sectionUnreadCounts(user.getId(), accessibles.stream().map(SectionChat::getId).toList());
        return accessibles.stream().map(chat -> new Room(sectionRoomId(chat.getId()), chat.getNom(),
                cibleNom(chat), List.of(), counts.getOrDefault(chat.getId(), 0L),
                false, false, canWriteSectionChat(user, chat), chat.getId())).toList();
    }

    public List<SectionRoom> sectionRooms(String username) {
        if (sectionChats == null) return List.of();
        User user = currentUser(username);
        List<SectionChat> accessibles = sectionChats.findAllByOrderByNomAsc().stream()
                .filter(chat -> canReadSectionChat(user, chat)).toList();
        var counts = reads.sectionUnreadCounts(user.getId(), accessibles.stream().map(SectionChat::getId).toList());
        return accessibles.stream().map(chat -> new SectionRoom(chat.getId(), chat.getNom(), cibleNom(chat),
                counts.getOrDefault(chat.getId(), 0L), canWriteSectionChat(user, chat))).toList();
    }

    public List<Message> sectionHistory(String username, Long sectionChatId, Long beforeId, Long afterId) {
        SectionChat chat = requireSectionChat(currentUser(username), sectionChatId, false);
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)
                || (beforeId != null && afterId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide");
        }
        var page = PageRequest.of(0, 50);
        List<ChatMessage> result = new ArrayList<>(afterId != null
                ? messages.findBySectionChatIdAndIdGreaterThanOrderByIdAsc(chat.getId(), afterId, page)
                : beforeId != null
                ? messages.findBySectionChatIdAndIdLessThanOrderByIdDesc(chat.getId(), beforeId, page)
                : messages.findBySectionChatIdOrderByIdDesc(chat.getId(), page));
        if (afterId == null) Collections.reverse(result);
        return result.stream().map(this::response).toList();
    }

    @Transactional
    public Message sendSectionMessage(String username, Long sectionChatId, String content) {
        User user = currentUser(username);
        SectionChat chat = requireSectionChat(user, sectionChatId, true);
        return saveMessage(user, content, null, chat, ChatChannel.ACTIVITY);
    }

    @Transactional
    public Unread markSectionRead(String username, Long sectionChatId, Long messageId) {
        User user = currentUser(username);
        SectionChat chat = requireSectionChat(user, sectionChatId, false);
        ChatMessage message = messages.findById(messageId)
                .filter(item -> item.getSectionChat() != null && item.getSectionChat().getId().equals(chat.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message invalide"));
        reads.markSectionRead(user.getId(), chat.getId(), message.getId());
        return new Unread(reads.sectionUnreadCounts(user.getId(), List.of(chat.getId())).getOrDefault(chat.getId(), 0L));
    }

    private List<Category> categories(Activite activity) {
        return activity.getPlanificationsHebdomadaires().stream()
                .map(category -> new Category(
                        category.getDescriptif() == null ? "" : category.getDescriptif().strip(),
                        dayLabel(category.getJour()),
                        category.getHoraireDebut() == null ? "" : category.getHoraireDebut().toString()))
                .toList();
    }

    private String dayLabel(java.time.DayOfWeek day) {
        if (day == null) return "";
        return switch (day) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
            case SATURDAY -> "Samedi";
            case SUNDAY -> "Dimanche";
        };
    }

    @Transactional
    public Unread markRead(String username, Long activityId, Long messageId) {
        User user = currentUser(username);
        requireRoom(user, activityId);
        ChatMessage message = messages.findById(messageId)
                .filter(m -> activityId == COMMUNICATION_ROOM_ID ? m.getChannel() == ChatChannel.COMMUNICATION
                        : activityId == REFERENT_ENCADRANT_ROOM_ID ? m.getChannel() == ChatChannel.REFERENT_ENCADRANT
                        : m.getActivite() != null && m.getActivite().getId().equals(activityId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message invalide"));
        if (activityId == COMMUNICATION_ROOM_ID) {
            reads.markCommunicationRead(user.getId(), message.getId());
            return new Unread(reads.communicationUnreadCount(user.getId()));
        }
        if (activityId == REFERENT_ENCADRANT_ROOM_ID) {
            reads.markReferentEncadrantRead(user.getId(), message.getId());
            return new Unread(reads.referentEncadrantUnreadCount(user.getId()));
        }
        reads.markRead(user.getId(), activityId, message.getId());
        return new Unread(reads.unreadCounts(user.getId(), List.of(activityId)).getOrDefault(activityId, 0L));
    }

    public List<Message> history(String username, Long activityId, Long beforeId, Long afterId) {
        requireRoom(currentUser(username), activityId);
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)
                || (beforeId != null && afterId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide");
        }
        var page = PageRequest.of(0, 50);
        List<ChatMessage> result = new ArrayList<>(isGlobalRoom(activityId)
                ? afterId != null ? messages.findByChannelAndIdGreaterThanOrderByIdAsc(channel(activityId), afterId, page)
                : beforeId != null ? messages.findByChannelAndIdLessThanOrderByIdDesc(channel(activityId), beforeId, page)
                : messages.findByChannelOrderByIdDesc(channel(activityId), page)
                : afterId != null
                ? messages.findByActiviteIdAndIdGreaterThanOrderByIdAsc(activityId, afterId, page)
                : beforeId != null
                ? messages.findByActiviteIdAndIdLessThanOrderByIdDesc(activityId, beforeId, page)
                : messages.findByActiviteIdOrderByIdDesc(activityId, page));
        if (afterId == null) Collections.reverse(result);
        return result.stream().map(this::response).toList();
    }

    @Transactional
    public Message send(String username, Long activityId, String content) {
        User user = currentUser(username);
        Activite activity = requireRoom(user, activityId);
        if (activityId == COMMUNICATION_ROOM_ID && !canCommunicate(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls les membres du bureau, du CA, le secrétariat et la communication globale peuvent publier dans Communication");
        }
        return saveMessage(user, content, activity, null, channel(activityId));
    }

    private Message saveMessage(User user, String content, Activite activity, SectionChat sectionChat, ChatChannel channel) {
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message doit contenir entre 1 et 2000 caractères");
        }
        Adherent author = user.getAdherent();
        String name = author == null ? "" : ((author.getPrenom() == null ? "" : author.getPrenom()) + " "
                + (author.getNom() == null ? "" : author.getNom())).trim();
        ChatMessage message = new ChatMessage();
        message.setActivite(activity);
        message.setSectionChat(sectionChat);
        message.setChannel(channel);
        message.setSenderUserId(user.getId());
        message.setSenderName(name.isBlank() ? "Adhérent" : name.substring(0, Math.min(name.length(), 255)));
        message.setSenderRole(displayRole(user));
        message.setContent(content.strip());
        message.setCreatedAt(Instant.now());
        return response(messages.save(message));
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Activite requireRoom(User user, Long activityId) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Les salons automatiques ont été supprimés. Utilisez un chat créé par l’administration.");
    }

    private SectionChat requireSectionChat(User user, Long sectionChatId, boolean ecriture) {
        if (sectionChats == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salon introuvable");
        }
        SectionChat chat = sectionChats.findById(sectionChatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salon introuvable"));
        if (!canReadSectionChat(user, chat) || (ecriture && !canWriteSectionChat(user, chat))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’êtes pas autorisé à accéder à ce salon");
        }
        return chat;
    }

    private boolean canReadSectionChat(User user, SectionChat chat) {
        return user.getRoles().contains(ERole.ROLE_ADMIN) || chat.getPermissions().stream()
                .anyMatch(permission -> user.getRoles().stream().map(Enum::name).anyMatch(permission.getRole()::equals));
    }

    private boolean canWriteSectionChat(User user, SectionChat chat) {
        return user.getRoles().contains(ERole.ROLE_ADMIN) || chat.getPermissions().stream()
                .anyMatch(permission -> permission.isEcriture()
                        && user.getRoles().stream().map(Enum::name).anyMatch(permission.getRole()::equals));
    }

    private long sectionRoomId(Long sectionChatId) {
        return -1_000_000L - sectionChatId;
    }

    private String cibleNom(SectionChat chat) {
        return switch (chat.getCible()) {
            case SECTION -> "Section · " + chat.getSection().getNom();
            case ACTIVITE -> "Activité · " + chat.getActivite().getNom();
            case ASSOCIATION -> "Association";
        };
    }

    private boolean canCommunicate(User user) {
        return canWriteEveryChat(user) || user.getRoles().contains(ERole.ROLE_COMMUNICATION_GLOBAL);
    }

    private boolean canAccessReferentEncadrant(User user) {
        return canWriteEveryChat(user) || user.getRoles().stream().anyMatch(role -> role == ERole.ROLE_REFERENT_ACTIVITE
                || role == ERole.ROLE_ENCADRANT);
    }

    private boolean isGlobalRoom(Long roomId) {
        return roomId == COMMUNICATION_ROOM_ID || roomId == REFERENT_ENCADRANT_ROOM_ID;
    }

    private ChatChannel channel(Long roomId) {
        if (roomId == COMMUNICATION_ROOM_ID) return ChatChannel.COMMUNICATION;
        if (roomId == REFERENT_ENCADRANT_ROOM_ID) return ChatChannel.REFERENT_ENCADRANT;
        return ChatChannel.ACTIVITY;
    }

    private boolean canWriteEveryChat(User user) {
        return user.getRoles().stream().anyMatch(role -> role == ERole.ROLE_BUREAU
                || role == ERole.ROLE_MEMBRECA || role == ERole.ROLE_SECRETAIRE);
    }

    private String displayRole(User user) {
        // The visual precedence is intentional: it follows the requested order.
        if (user.getRoles().contains(ERole.ROLE_SECRETAIRE)) return "SECRETAIRE";
        if (user.getRoles().contains(ERole.ROLE_MEMBRECA)) return "MEMBRECA";
        if (user.getRoles().contains(ERole.ROLE_ENCADRANT)) return "ENCADRANT";
        if (user.getRoles().contains(ERole.ROLE_REFERENT_ACTIVITE)) return "REFERENT_ACTIVITE";
        if (user.getRoles().contains(ERole.ROLE_REFERENT_SECTION)) return "REFERENT_SECTION";
        if (user.getRoles().contains(ERole.ROLE_COMMUNICATION_SECTION)) return "COMMUNICATION_SECTION";
        if (user.getRoles().contains(ERole.ROLE_COMMUNICATION_GLOBAL)) return "COMMUNICATION_GLOBAL";
        return null;
    }

    private Message response(ChatMessage message) {
        return new Message(message.getId(), message.getSenderUserId(), message.getSenderName(), message.getSenderRole(),
                message.getContent(), message.getCreatedAt());
    }
}
