package com.gestion.user.models;

import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class UserLite {

    private Long id;

    private String prenom;

    private String nom;

    private String email;

    private Set<Role> roles = new HashSet<>();

    public UserLite(User user){
        this.id = user.getId();
        this.nom = user.getAdherent().getNom();
        this.prenom = user.getAdherent().getPrenom();
        this.email = user.getUsername();
        this.roles = user.getRoles();
    }

}
