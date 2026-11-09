package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.EventChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/teams/events/{id}/messages") @PreAuthorize("isAuthenticated()")
public class EventChatController {
    private final EventChatService chat;
    public record Send(String content) { }
    @GetMapping
    public List<EventChatService.Message> history(@PathVariable Long id,
            @RequestParam(required = false) Long beforeId, @RequestParam(required = false) Long afterId, Principal p) {
        return chat.history(id, beforeId, afterId, p.getName());
    }
    @PostMapping
    public EventChatService.Message send(@PathVariable Long id, @RequestBody Send body, Principal p) {
        return chat.send(id, body.content(), p.getName());
    }
}
