package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatIdOrderByIdDesc(Long chatId, Pageable pageable);
    List<ChatMessage> findByChatIdAndIdLessThanOrderByIdDesc(Long chatId, Long beforeId, Pageable pageable);
    List<ChatMessage> findByChatIdAndIdGreaterThanOrderByIdAsc(Long chatId, Long afterId, Pageable pageable);
}
