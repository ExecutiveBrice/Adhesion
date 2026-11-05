package com.wild.corp.adhesion.repository;
import com.wild.corp.adhesion.models.TeamMail;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface TeamMailRepository extends JpaRepository<TeamMail, Long> {
    boolean existsByCle(String cle);
    List<TeamMail> findBySeance_Id(Long seanceId);
    void deleteBySeance_Id(Long seanceId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeamMail> findFirstByEtatOrderByIdAsc(String etat);
}
