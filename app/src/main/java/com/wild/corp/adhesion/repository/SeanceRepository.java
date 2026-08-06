package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.ESeance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, Long> {

    Optional<Seance> findByIdAndActivite_Id(Long id, Long activiteId);

    List<Seance> findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
            LocalDateTime debut, LocalDateTime fin);

    @Query("select distinct s from Seance s join s.activite a join a.adhesions ad join ad.adherent h join h.tribu t " +
            "where t.uuid = :tribuUuid and ad.statutActuel not in :statutsExclus and s.debut >= :debut and s.debut < :fin order by s.debut")
    List<Seance> findAllByTribuAndStatutNonExcluAndDebutBetweenOrderByDebut(@Param("tribuUuid") java.util.UUID tribuUuid,
            @Param("statutsExclus") List<String> statutsExclus,
            @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seance s set s.etatSeance = :etat where s.id = :id and s.activite.id = :activiteId")
    int updateEtat(@Param("id") Long id, @Param("activiteId") Long activiteId, @Param("etat") ESeance etat);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seance s set s.commentaire = :commentaire where s.id = :id and s.activite.id = :activiteId")
    int updateCommentaire(@Param("id") Long id, @Param("activiteId") Long activiteId,
                          @Param("commentaire") String commentaire);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seance s set s.debut = :debut, s.fin = :fin where s.id = :id and s.activite.id = :activiteId")
    int updateHoraire(@Param("id") Long id, @Param("activiteId") Long activiteId,
                      @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
