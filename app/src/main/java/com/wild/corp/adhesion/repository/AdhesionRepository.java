package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdhesionRepository extends JpaRepository<Adhesion, Long>, JpaSpecificationExecutor<Adhesion> {


    List<Adhesion> findByActiviteNom(String nom);

    Page<Adhesion> findByActiviteNom(String nom, Pageable pageable);

    Page<Adhesion> findByActiviteId(Long id, Pageable pageable);

    @Query("select distinct adhesion.statutActuel from Adhesion adhesion where adhesion.statutActuel is not null order by adhesion.statutActuel")
    List<String> findDistinctStatuses();

}
