package com.wild.corp.adhesion.repository;
import com.wild.corp.adhesion.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
/** Events retain their historical table and identifiers during the model migration. */
public interface SeanceRepository extends JpaRepository<Seance, Long> {
    Optional<Seance> findByIdAndActivite_Id(Long id, Long activiteId);
    List<Seance> findByActivite_IdOrderByDebutAsc(Long activiteId);
    List<Seance> findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(LocalDateTime debut, LocalDateTime fin);
    String MANAGER = "(exists (select t.id from Equipe t join t.encadrants e where t member of s.equipes and e.user.username = :username) "
            + "or exists (select t.id from Equipe t join t.referents e where t member of s.equipes and e.user.username = :username))";
    @Query("select s from Seance s where " + MANAGER + " and s.debut >= :debut and s.debut < :fin order by s.debut")
    List<Seance> findTodayByProfessorUsername(@Param("username") String username, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    @Query("select s from Seance s where s.id = :seanceId and " + MANAGER)
    Optional<Seance> findByIdAndManagerUsername(@Param("seanceId") Long id, @Param("username") String username);
    @Query("select s from Seance s where s.debut >= :debut and s.debut < :fin and exists (select p.id from Presence p where p.seance = s "
            + "and p.adhesion.adherent.tribu.uuid = :tribuUuid and p.adhesion.statutActuel not in :statutsExclus "
            + "and not exists (select c.id from ConvocationEquipe c where c.seance = s and c.adherent = p.adhesion.adherent and c.selectionnee = false)) order by s.debut")
    List<Seance> findAllByTribuAndStatutNonExcluAndDebutBetweenOrderByDebut(@Param("tribuUuid") UUID tribuUuid,
            @Param("statutsExclus") List<String> excluded, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    @Query("select s from Seance s where s.debut >= :debut and s.debut < :fin and exists (select p.id from Presence p where p.seance = s "
            + "and p.adhesion.adherent.id = :adherentId and p.adhesion.statutActuel not in :statutsExclus "
            + "and not exists (select c.id from ConvocationEquipe c where c.seance = s and c.adherent = p.adhesion.adherent and c.selectionnee = false)) order by s.debut")
    List<Seance> findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(@Param("adherentId") Long id,
            @Param("statutsExclus") List<String> excluded, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seance s set s.etatSeance = :etatRealisee where s.debut >= :debut and s.debut < :fin and s.etatSeance in :etatsARealiser")
    int updateEtatForDebutBetweenAndEtatIn(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin,
            @Param("etatsARealiser") List<ESeance> states, @Param("etatRealisee") ESeance completed);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seance s set s.etatSeance = :etatRealisee where s.debut < :fin and s.etatSeance in :etatsARealiser")
    int updateEtatForDebutBeforeAndEtatIn(@Param("fin") LocalDateTime fin, @Param("etatsARealiser") List<ESeance> states, @Param("etatRealisee") ESeance completed);
}
