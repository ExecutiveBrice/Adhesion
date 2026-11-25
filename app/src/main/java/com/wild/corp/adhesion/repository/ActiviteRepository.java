package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Activite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long>, JpaSpecificationExecutor<Activite> {

    List<Activite> findByNom(String nom);

    boolean existsByNom(String nom);

    // Sérialise les écritures qui peuvent créer les mêmes fiches de présence.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activite a where a.id = :id")
    Optional<Activite> findByIdForUpdate(@Param("id") Long id);
}
