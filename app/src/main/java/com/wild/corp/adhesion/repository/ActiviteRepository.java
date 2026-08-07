package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Activite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long>, JpaSpecificationExecutor<Activite> {

    List<Activite> findByNom(String nom);

    boolean existsByNom(String nom);
}
