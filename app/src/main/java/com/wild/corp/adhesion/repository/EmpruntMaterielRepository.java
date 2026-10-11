package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.EmpruntMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmpruntMaterielRepository extends JpaRepository<EmpruntMateriel, Long> {
    void deleteAllByMaterielId(Long materielId);
    List<EmpruntMateriel> findAllByMaterielIdOrderByDateEmpruntDescIdDesc(Long materielId);
    Optional<EmpruntMateriel> findFirstByMaterielIdAndHorsServiceIsNullAndDateRetourIsNullOrderByDateEmpruntDescIdDesc(Long materielId);
}
