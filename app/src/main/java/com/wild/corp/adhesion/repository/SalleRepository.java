package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {

    List<Salle> findAllByOrderByNomAsc();
}
