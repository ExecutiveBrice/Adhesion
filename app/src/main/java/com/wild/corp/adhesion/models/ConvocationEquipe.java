package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
@Table(name = "convocations_equipe", uniqueConstraints = @UniqueConstraint(columnNames = {"seance_id", "adherent_id", "objet"}))
public class ConvocationEquipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;
    @ManyToOne(optional = false) @JoinColumn(name = "adherent_id", nullable = false)
    private Adherent adherent;
    @ManyToOne @JoinColumn(name = "adhesion_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Adhesion adhesion;
    @Column(nullable = false)
    private boolean selectionnee;
    @Column(nullable = false, length = 100)
    private String objet = "PARTICIPANT";
    private String responsabilite;

    public boolean isParticipant() { return "PARTICIPANT".equals(objet); }
    public static String responsibilityKey(ResponsabiliteSeance r) {
        return "RESPONSABILITE:" + r.getTache().getId() + ":" + (r.getEquipeId() == null ? 0 : r.getEquipeId());
    }
    public String recipientLabel() {
        return adherent.getPrenom() + " " + adherent.getNom()
                + (isParticipant() ? "" : " · " + responsabilite);
    }

    private LocalDateTime publication;
    private Boolean reponse;
    private LocalDateTime dateReponse;
    private Long reponduPar;

    // A dated null answer means “not sure yet”; an undated null means no reply.
    public boolean hasReplied() { return reponse != null || dateReponse != null; }

    @ElementCollection
    @CollectionTable(name = "convocation_reponses", joinColumns = @JoinColumn(name = "convocation_id"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private java.util.List<ReponseConvocation> historique = new java.util.ArrayList<>();
}
