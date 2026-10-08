package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdherentRepository extends JpaRepository<Adherent, Long>, JpaSpecificationExecutor<Adherent> {

    Optional<Adherent> findByUserUsername(String username);

    @Query("select distinct a from Adherent a where exists (select ad.id from Adhesion ad " +
            "where ad.adherent.tribu = a.tribu and a.tribu is not null " +
            "and ad.activite.section.id = :sectionId " +
            "and (ad.statutActuel is null or ad.statutActuel not in :statutsExclus)) " +
            "order by a.nom, a.prenom, a.id")
    List<Adherent> findReferentCandidates(@Param("sectionId") Long sectionId,
                                         @Param("statutsExclus") List<String> statutsExclus);

    @Query("select distinct a from Adherent a join a.user u join u.roles r where r = :role")
    List<Adherent> findByUserRole(@Param("role") ERole role);
    @Query("select a.id from Adherent a")
    List<Long> getAllIds();

    @Query("select a from Adherent a where exists (select ad.id from Adhesion ad " +
            "where ad.adherent = a and ad.activite.section.id = :sectionId " +
            "and ad.statutActuel not in :statutsExclus) order by a.nom, a.prenom, a.id")
    List<Adherent> findResponsabiliteCandidates(@Param("sectionId") Long sectionId,
                                               @Param("statutsExclus") List<String> statutsExclus);

    @Query("select a from Adherent a where a.id = :adherentId and exists (select ad.id from Adhesion ad " +
            "where ad.adherent = a and ad.activite.section.id = :sectionId " +
            "and ad.statutActuel not in :statutsExclus)")
    Optional<Adherent> findResponsabiliteCandidate(@Param("adherentId") Long adherentId,
                                                 @Param("sectionId") Long sectionId,
                                                 @Param("statutsExclus") List<String> statutsExclus);

}
