package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.EventChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventChatMessageRepository extends JpaRepository<EventChatMessage, Long> {
    List<EventChatMessage> findBySeanceIdOrderByIdDesc(Long id, Pageable page);
    List<EventChatMessage> findBySeanceIdAndIdLessThanOrderByIdDesc(Long id, Long beforeId, Pageable page);
    List<EventChatMessage> findBySeanceIdAndIdGreaterThanOrderByIdAsc(Long id, Long afterId, Pageable page);
}
