package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class EventChatService {
    private final TeamManagementService events;
    private final EventChatMessageRepository messages;
    private final SeanceRepository sessions;
    private final UserRepository users;

    public record Message(Long id, Long senderUserId, String senderName, String content, Instant createdAt) { }

    public List<Message> history(Long eventId, Long beforeId, Long afterId, String username) {
        events.checkChatAccess(eventId, username);
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)
                || (beforeId != null && afterId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide");
        }
        var page = PageRequest.of(0, 50);
        var result = new ArrayList<>(afterId != null
                ? messages.findBySeanceIdAndIdGreaterThanOrderByIdAsc(eventId, afterId, page)
                : beforeId != null ? messages.findBySeanceIdAndIdLessThanOrderByIdDesc(eventId, beforeId, page)
                : messages.findBySeanceIdOrderByIdDesc(eventId, page));
        if (afterId == null) Collections.reverse(result);
        return result.stream().map(this::view).toList();
    }

    @Transactional
    public Message send(Long eventId, String content, String username) {
        events.checkChatAccess(eventId, username);
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message doit contenir entre 1 et 2000 caractères");
        }
        User user = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Adherent author = user.getAdherent();
        String name = author == null ? "" : (Objects.toString(author.getPrenom(), "") + " " + Objects.toString(author.getNom(), "")).trim();
        EventChatMessage message = new EventChatMessage();
        message.setSeance(sessions.getReferenceById(eventId));
        message.setSenderUserId(user.getId());
        message.setSenderName(name.isBlank() ? "Adhérent" : name.substring(0, Math.min(255, name.length())));
        message.setContent(content.strip());
        message.setCreatedAt(Instant.now());
        return view(messages.save(message));
    }

    private Message view(EventChatMessage m) {
        return new Message(m.getId(), m.getSenderUserId(), m.getSenderName(), m.getContent(), m.getCreatedAt());
    }
}
