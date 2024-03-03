package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Accord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccordRepository extends JpaRepository<Accord, Long> {


}
