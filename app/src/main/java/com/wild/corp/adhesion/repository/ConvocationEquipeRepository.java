package com.wild.corp.adhesion.repository;
import com.wild.corp.adhesion.models.ConvocationEquipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ConvocationEquipeRepository extends JpaRepository<ConvocationEquipe, Long> {
    @org.springframework.data.jpa.repository.Query("select c from ConvocationEquipe c join c.adherent p left join p.user u " +
            "left join p.representant r left join r.user ru where c.selectionnee = true and c.publication is not null " +
            "and (u.username = :name or ru.username = :name)")
    List<ConvocationEquipe> findPublishedForUser(@org.springframework.data.repository.query.Param("name") String name);
    List<ConvocationEquipe> findBySeance_IdOrderByIdAsc(Long seanceId);
    void deleteBySeance_Id(Long seanceId);
}
