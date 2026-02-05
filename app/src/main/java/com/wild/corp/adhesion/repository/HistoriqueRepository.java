package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Historique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueRepository extends JpaRepository<Historique, Long> {


}
