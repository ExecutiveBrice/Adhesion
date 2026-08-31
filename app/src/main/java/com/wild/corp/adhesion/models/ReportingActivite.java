package com.wild.corp.adhesion.models;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ReportingActivite {

    private String nomActivite;

    /** Groupe métier affiché comme en-tête et utilisé pour les sous-totaux. */
    private String groupe;

    private Long nbInitee =0L;

    private Long nbPayee =0L;

    private Long nbValidee =0L;

    private Long nbF =0L;

    private Long nbM =0L;

    private Long cotisations =0L;

}
