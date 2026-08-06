package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.GoogleAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoogleAgendaRepository extends JpaRepository<GoogleAgenda, Long> {

    List<GoogleAgenda> findAllByOrderByNomAsc();

    boolean existsBySource(String source);

    boolean existsBySourceAndIdNot(String source, Long id);
}
