package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.wild.corp.adhesion.models.ChatChannel;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelOrderByIdDesc(ChatChannel channel, Pageable pageable);
    List<ChatMessage> findByChannelAndIdLessThanOrderByIdDesc(ChatChannel channel, Long beforeId, Pageable pageable);
    List<ChatMessage> findByChannelAndIdGreaterThanOrderByIdAsc(ChatChannel channel, Long afterId, Pageable pageable);
    List<ChatMessage> findByActiviteIdOrderByIdDesc(Long activiteId, Pageable pageable);
    List<ChatMessage> findByActiviteIdAndIdLessThanOrderByIdDesc(Long activiteId, Long beforeId, Pageable pageable);
    List<ChatMessage> findByActiviteIdAndIdGreaterThanOrderByIdAsc(Long activiteId, Long afterId, Pageable pageable);
    List<ChatMessage> findBySectionChatIdOrderByIdDesc(Long sectionChatId, Pageable pageable);
    List<ChatMessage> findBySectionChatIdAndIdLessThanOrderByIdDesc(Long sectionChatId, Long beforeId, Pageable pageable);
    List<ChatMessage> findBySectionChatIdAndIdGreaterThanOrderByIdAsc(Long sectionChatId, Long afterId, Pageable pageable);
}
