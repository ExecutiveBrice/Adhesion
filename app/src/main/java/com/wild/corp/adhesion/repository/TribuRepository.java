package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Tribu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TribuRepository extends JpaRepository<Tribu, Long> {

}

