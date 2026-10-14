package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Zonage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ZonageRepository extends JpaRepository<Zonage, Long> {
    List<Zonage> findAllByOrderByNomAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zonage z where z.id = :id")
    Optional<Zonage> findByIdForUpdate(@Param("id") Long id);
}
