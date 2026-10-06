package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class ResponsabiliteSeance {
    @ManyToOne(optional = false)
    @JoinColumn(name = "tache_id", nullable = false)
    private TacheSeance tache;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "adherent_id", nullable = false)
    private Adherent adherent;

    public ResponsabiliteSeance(TacheSeance tache, Adherent adherent) {
        this.tache = tache;
        this.adherent = adherent;
    }

    @JsonProperty("adherent")
    public AdherentResponsabiliteResponse getAdherentResponse() {
        return AdherentResponsabiliteResponse.from(adherent);
    }
}
