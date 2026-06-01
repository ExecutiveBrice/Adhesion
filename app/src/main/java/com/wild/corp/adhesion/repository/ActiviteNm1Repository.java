package com.wild.corp.adhesion.repository;


import com.wild.corp.adhesion.models.ActiviteNm1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ActiviteNm1Repository extends JpaRepository<ActiviteNm1, Long> {

    List<ActiviteNm1> findByNom(String nom);

    List<ActiviteNm1> findByActiviteId(Long activiteId);
}
