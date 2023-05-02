package com.gestion.user.repository;

import com.gestion.user.models.Tribu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribuRepository extends JpaRepository<Tribu, Long> {

}

