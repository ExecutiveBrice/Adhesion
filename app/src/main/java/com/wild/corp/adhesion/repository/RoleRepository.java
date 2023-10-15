package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
  Optional<Role> findByName(ERole name);
}
