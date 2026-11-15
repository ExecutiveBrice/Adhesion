package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByEquipeId(Long equipeId);
    List<Chat> findAllBySectionIdOrderByNomAsc(Long sectionId);
    List<Chat> findAllByOrderByNomAsc();
    boolean existsBySectionIdAndNomIgnoreCase(Long sectionId, String nom);
}
