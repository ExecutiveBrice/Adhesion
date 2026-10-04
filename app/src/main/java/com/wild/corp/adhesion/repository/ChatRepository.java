package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllBySectionIdOrderByNomAsc(Long sectionId);
    List<Chat> findAllByOrderByNomAsc();
    boolean existsBySectionIdAndNomIgnoreCase(Long sectionId, String nom);
}
