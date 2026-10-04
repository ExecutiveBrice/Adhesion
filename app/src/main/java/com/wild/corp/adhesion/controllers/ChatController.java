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

    @GetMapping
    public List<ChatService.Room> rooms(Principal principal) {
        requireChatEnabled();
        return chat.rooms(principal.getName());
    }

    @PostMapping("/{chatId}/read")
    public ChatService.Unread markRead(Principal principal, @PathVariable Long chatId,
            @Valid @RequestBody ReadMessages request) {
        requireChatEnabled();
        return chat.markRead(principal.getName(), chatId, request.lastMessageId());
    }

    @GetMapping("/{chatId}/messages")
    public List<ChatService.Message> history(Principal principal, @PathVariable Long chatId,
            @RequestParam(required = false) Long beforeId, @RequestParam(required = false) Long afterId) {
        requireChatEnabled();
        return chat.history(principal.getName(), chatId, beforeId, afterId);
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.Message send(Principal principal, @PathVariable Long chatId,
            @Valid @RequestBody SendMessage request) {
        requireChatEnabled();
        return chat.send(principal.getName(), chatId, request.content());
    }

    private void requireChatEnabled() {
        if (!paramBooleanServices.findByParamValue("Show_Chat")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le chat est désactivé");
        }
    }
}
