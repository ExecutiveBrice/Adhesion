package com.wild.corp.adhesion.repository;
import com.wild.corp.adhesion.models.ConvocationEquipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ConvocationEquipeRepository extends JpaRepository<ConvocationEquipe, Long> {
    @org.springframework.data.jpa.repository.Query("select c from ConvocationEquipe c join c.adherent p join p.user u " +
            "join fetch c.seance s where u.username = :name and c.selectionnee = true " +
            "and c.publication is not null and c.reponse is null and c.dateReponse is null " +
            "and (s.sansInvitation = false or c.objet <> 'PARTICIPANT') " +
            "and s.debut > :now and s.etatSeance in :states order by s.debut, s.id")
    List<ConvocationEquipe> findPendingForUser(
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            @org.springframework.data.repository.query.Param("states") List<com.wild.corp.adhesion.models.ESeance> states);

    @org.springframework.data.jpa.repository.Query("select c from ConvocationEquipe c join c.adherent p left join p.user u " +
            "left join p.representant r left join r.user ru where c.selectionnee = true and c.publication is not null " +
            "and (u.username = :name or ru.username = :name)")
    List<ConvocationEquipe> findPublishedForUser(@org.springframework.data.repository.query.Param("name") String name);
    List<ConvocationEquipe> findBySeance_IdOrderByIdAsc(Long seanceId);
    void deleteBySeance_Id(Long seanceId);
}
