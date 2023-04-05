package com.gestion.user.repository;

import java.util.Optional;

import com.gestion.user.models.ERole;
import com.gestion.user.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
