package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.Activite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long> {

    List<Activite> findByNom(String nom);


    boolean existsByNom(String nom);
}
