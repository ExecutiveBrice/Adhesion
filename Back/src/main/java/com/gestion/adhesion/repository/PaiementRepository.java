package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.Adhesion;
import com.gestion.adhesion.models.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

}

