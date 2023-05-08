package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.ParamText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParamTextRepository extends JpaRepository<ParamText, Long> {

    Optional<ParamText> findByParamName(String paramName);

    Boolean existsByParamName(String paramName);

}
