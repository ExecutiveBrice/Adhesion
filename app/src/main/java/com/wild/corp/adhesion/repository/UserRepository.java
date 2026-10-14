package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  @Query("select u from User u where :role member of u.roles")
  List<User> findAllByRole(@Param("role") ERole role);
  Optional<User> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from User user where user.username = :username")
  Optional<User> findByUsernameForUpdate(@Param("username") String username);

  Boolean existsByUsername(String username);

  @EntityGraph(attributePaths = {"adherent", "adherent.tribu"})
  List<User> findAllByIdIn(Collection<Long> ids);

}
