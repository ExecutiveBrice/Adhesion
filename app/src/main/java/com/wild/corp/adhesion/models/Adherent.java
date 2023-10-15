package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(name = "adherents",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Adherent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prenom;

    private String nom;

    private String genre;

    private String email;

    private String telephone;

    private LocalDate naissance;

    private String lieuNaissance;

    private boolean referent = false;

    private String adresse;

    private boolean adresseReferent = true;

    private boolean telephoneReferent = true;

    private boolean emailReferent = true;

    private boolean mineur = false;

    private String nomLegal;

    private String prenomLegal;

    private boolean legalReferent = true;

    private boolean completReferent = false;

    private boolean completAdhesion = false;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Accord> accords = new ArrayList<>();

    @OneToMany
    private List<Document> documents = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<Notification> derniereModifs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<Notification> derniereVisites = new ArrayList<>();

    @ManyToOne
    private Tribu tribu;

    @OneToMany(mappedBy="adherent", cascade = CascadeType.ALL)
    private Set<Adhesion> adhesions = new HashSet<>();

    @ManyToMany
    @JsonIgnore
    private Set<Activite> cours = new HashSet<>();

    @OneToOne
    @JsonIgnoreProperties({"adherent","tokens" })
    private User user;

    public String getEmail(Adherent dataAdherent) {

       if(this.isReferent()){
           return email;
       } else if (this.isEmailReferent()) {
           return dataAdherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail();
       }
       return email;
    }

    public String getAdresse(Adherent dataAdherent) {

        if(this.isReferent()){
            return adresse;
        } else if (this.isAdresseReferent()) {
            return dataAdherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getAdresse();
        }
        return adresse;
    }

    public String getTelephone(Adherent dataAdherent) {

        if(this.isReferent()){
            return telephone;
        } else if (this.isTelephoneReferent()) {
            return dataAdherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getTelephone();
        }
        return telephone;
    }

}
