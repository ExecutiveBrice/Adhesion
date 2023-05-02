package com.gestion.user.repository;

import com.gestion.user.models.Activite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long> {

    Optional<Activite> findByNom(String nom);

    public boolean existsByNom(String nom);
}
