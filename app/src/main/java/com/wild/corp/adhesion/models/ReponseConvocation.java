package com.wild.corp.adhesion.models;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.time.LocalDateTime;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReponseConvocation {
    private LocalDateTime dateReponse;
    private Boolean reponse;
    private Long auteurId;
    private int revision;
}
