package com.gestion.user.repository;

import com.gestion.user.models.Adhesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdhesionRepository extends JpaRepository<Adhesion, Long> {

}

