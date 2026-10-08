package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByOrderByNomAsc();

    @Query("select distinct s from Section s join s.referents r where r.username = :username order by s.nom")
    List<Section> findManagedByUsername(@Param("username") String username);

    boolean existsByReferents_Id(Long userId);

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
