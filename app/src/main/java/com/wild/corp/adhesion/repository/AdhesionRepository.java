package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdhesionRepository extends JpaRepository<Adhesion, Long> {


    List<Adhesion> findByActiviteNom(String nom);

}

