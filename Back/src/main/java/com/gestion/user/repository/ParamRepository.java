package com.gestion.user.repository;

import com.gestion.user.models.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParamRepository extends JpaRepository<Param, Long> {

    Optional<Param> findByParamName(String paramName);

    Boolean existsByParamName(String paramName);

}
