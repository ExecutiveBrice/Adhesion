package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdherentRepository extends JpaRepository<Adherent, Long>, JpaSpecificationExecutor<Adherent> {

    @Query("select distinct a from Adherent a join a.user u join u.roles r where r = :role")
    List<Adherent> findByUserRole(@Param("role") ERole role);
    @Query("select a.id from Adherent a")
    List<Long> getAllIds();

}
