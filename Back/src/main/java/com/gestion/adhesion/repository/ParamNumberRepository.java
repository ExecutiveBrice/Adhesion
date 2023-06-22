package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.ParamNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParamNumberRepository extends JpaRepository<ParamNumber, Long> {

    Optional<ParamNumber> findByParamName(String paramName);

    Boolean existsByParamName(String paramName);

}
