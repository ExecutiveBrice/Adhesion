package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Materiel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MaterielRepository extends JpaRepository<Materiel, Long> {
    @EntityGraph(attributePaths = "zonage")
    List<Materiel> findAllByOrderByNomAscIdAsc();

    boolean existsByZonageId(Long zonageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Materiel m where m.id = :id")
    Optional<Materiel> findByIdForUpdate(@Param("id") Long id);
}
