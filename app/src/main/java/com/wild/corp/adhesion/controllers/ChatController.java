package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ChatService;
import com.wild.corp.adhesion.services.ParamBooleanServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/chat")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;
    private final ParamBooleanServices paramBooleanServices;
    public record SendMessage(@NotBlank @Size(max = 2000) String content) {}
    public record ReadMessages(@NotNull @Positive Long lastMessageId) {}

    @PostMapping("/activities/{activityId}/read")
    public ChatService.Unread markRead(Principal principal, @PathVariable Long activityId,
            @Valid @RequestBody ReadMessages request) {
        requireChatEnabled();
        return chat.markRead(principal.getName(), activityId, request.lastMessageId());
    }

    @GetMapping("/activities")
    public List<ChatService.Room> rooms(Principal principal) {
        requireChatEnabled();
        return chat.rooms(principal.getName());
    }

    @GetMapping("/section-chats")
    public List<ChatService.SectionRoom> sectionRooms(Principal principal) {
        requireChatEnabled();
        return chat.sectionRooms(principal.getName());
    }

    @PostMapping("/section-chats/{sectionChatId}/read")
    public ChatService.Unread markSectionRead(Principal principal, @PathVariable Long sectionChatId,
            @Valid @RequestBody ReadMessages request) {
        requireChatEnabled();
        return chat.markSectionRead(principal.getName(), sectionChatId, request.lastMessageId());
    }

    @GetMapping("/section-chats/{sectionChatId}/messages")
    public List<ChatService.Message> sectionHistory(Principal principal, @PathVariable Long sectionChatId,
            @RequestParam(required = false) Long beforeId, @RequestParam(required = false) Long afterId) {
        requireChatEnabled();
        return chat.sectionHistory(principal.getName(), sectionChatId, beforeId, afterId);
    }

    @PostMapping("/section-chats/{sectionChatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.Message sendSectionMessage(Principal principal, @PathVariable Long sectionChatId,
            @Valid @RequestBody SendMessage request) {
        requireChatEnabled();
        return chat.sendSectionMessage(principal.getName(), sectionChatId, request.content());
    }

    @GetMapping("/activities/{activityId}/messages")
    public List<ChatService.Message> history(Principal principal, @PathVariable Long activityId,
            @RequestParam(required = false) Long beforeId, @RequestParam(required = false) Long afterId) {
        requireChatEnabled();
        return chat.history(principal.getName(), activityId, beforeId, afterId);
    }

    @PostMapping("/activities/{activityId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.Message send(Principal principal, @PathVariable Long activityId,
            @Valid @RequestBody SendMessage request) {
        requireChatEnabled();
        return chat.send(principal.getName(), activityId, request.content());
    }

    private void requireChatEnabled() {
        if (!paramBooleanServices.findByParamValue("Show_Chat")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le chat est désactivé");
        }
    }
}
