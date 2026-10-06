package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findBySeance_IdOrderByAdhesion_Adherent_NomAscAdhesion_Adherent_PrenomAsc(Long seanceId);

    Optional<Presence> findBySeance_IdAndAdhesion_Adherent_User_Username(Long seanceId, String username);

}
