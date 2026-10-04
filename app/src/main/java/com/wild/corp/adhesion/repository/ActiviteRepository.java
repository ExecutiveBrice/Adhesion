package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Activite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long>, JpaSpecificationExecutor<Activite> {

    List<Activite> findByNom(String nom);

    boolean existsByNom(String nom);

    @Query("""
        select a from Activite a where
        exists (select h.id from Adhesion h where h.adherent.user.id = :userId
            and (h.statutActuel is null or h.statutActuel not in :excludedStatuses)
            and (h.activite = a or h.surClassement = a))
        or exists (select p.id from Adherent p join p.cours c where p.user.id = :userId and c = a)
        or exists (select r.id from Adherent r join r.activitesReferent c where r.user.id = :userId and c = a)
        or exists (select s.id from Section s join s.referents r where s = a.section and r.id = :userId)
        order by a.nom, a.id
        """)
    List<Activite> findChatActivities(@Param("userId") Long userId, @Param("excludedStatuses") List<String> excludedStatuses);
}
