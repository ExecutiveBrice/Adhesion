package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdherentRepository extends JpaRepository<Adherent, Long> {

    @Query(value = "SELECT adherents.* From adherents, users, users_roles WHERE adherents.user_id = users.id and users.id = users_roles.user_id and users_roles.roles_id = :roleId", nativeQuery = true)
    List<Adherent> findByUserRoleId(@Param("roleId") Long roleId);
    @Query("select a.id from Adherent a")
    List<Long> getAllIds();

}
