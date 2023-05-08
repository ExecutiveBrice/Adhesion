package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.Tribu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribuRepository extends JpaRepository<Tribu, Long> {

}

