package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.TacheSeance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TacheSeanceRepository extends JpaRepository<TacheSeance, Long> {
    List<TacheSeance> findAllByOrderByNomAsc();
    boolean existsByNomIgnoreCase(String nom);
    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);

    @Query("select count(s) > 0 from Seance s join s.responsabilites r where r.tache.id = :id")
    boolean isUsed(@Param("id") Long id);
}
