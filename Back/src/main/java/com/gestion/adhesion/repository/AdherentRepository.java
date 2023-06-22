package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.Adherent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdherentRepository extends JpaRepository<Adherent, Long> {


    @Query("select a.id from Adherent a")
    List<Long> getAllIds();

}
