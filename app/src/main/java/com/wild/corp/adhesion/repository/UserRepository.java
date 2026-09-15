package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from User user where user.username = :username")
  Optional<User> findByUsernameForUpdate(@Param("username") String username);

  Boolean existsByUsername(String username);

}
