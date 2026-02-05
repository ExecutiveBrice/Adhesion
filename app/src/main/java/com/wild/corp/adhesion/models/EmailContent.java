package com.wild.corp.adhesion.models;


import com.wild.corp.adhesion.models.resources.Groupe;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class EmailContent {

    private List<Groupe> diffusion = new ArrayList<>();
    private List<String> destinataires = new ArrayList<>();
    private String subject;
    private String text;

}
