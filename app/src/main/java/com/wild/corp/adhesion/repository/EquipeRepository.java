package com.wild.corp.adhesion.repository;
import com.wild.corp.adhesion.models.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    List<Equipe> findByActivite_Section_IdAndArchiveeFalseOrderByActivite_NomAscNomAsc(Long sectionId);
    List<Equipe> findByActivite_IdOrderByNomAsc(Long activiteId);
    List<Equipe> findByActivite_IdOrderByIdAsc(Long activiteId);
}
