package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.ParamBoolean;
import com.gestion.adhesion.models.ParamText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParamBooleanRepository extends JpaRepository<ParamBoolean, Long> {

    Optional<ParamBoolean> findByParamName(String paramName);

    Boolean existsByParamName(String paramName);

}
