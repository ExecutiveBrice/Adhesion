package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.EmpruntMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface EmpruntMaterielRepository extends JpaRepository<EmpruntMateriel, Long> {
    @Modifying
    @Query("""
            update EmpruntMateriel e set e.reservation = true
            where e.reservation is null and e.horsService is null and e.dateRetour is null
              and e.dateAnnulation is null and e.dateDebut > :aujourdHui
            """)
    void reprendreReservationsExistantes(@Param("aujourdHui") LocalDate aujourdHui);

    @EntityGraph(attributePaths = {"materiel", "materiel.zonage", "retours"})
    @Query("""
            select distinct e from EmpruntMateriel e
            where e.horsService is null
              and e.dateAnnulation is null
              and (e.dateDebut <= :fin or (e.dateDebut is null and e.dateEmprunt < :finExclusive))
              and (e.dateRetour is null or e.dateRetour >= :debut)
            order by e.dateEmprunt, e.id
            """)
    List<EmpruntMateriel> findForCalendrier(@Param("debut") OffsetDateTime debut,
            @Param("fin") LocalDate fin, @Param("finExclusive") OffsetDateTime finExclusive);

    void deleteAllByMaterielId(Long materielId);
    List<EmpruntMateriel> findAllByMaterielIdOrderByDateEmpruntDescIdDesc(Long materielId);
}
