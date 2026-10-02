package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.SectionChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionChatRepository extends JpaRepository<SectionChat, Long> {
    List<SectionChat> findAllBySectionIdOrderByNomAsc(Long sectionId);
    List<SectionChat> findAllByOrderByNomAsc();
    boolean existsBySectionIdAndNomIgnoreCase(Long sectionId, String nom);
}
