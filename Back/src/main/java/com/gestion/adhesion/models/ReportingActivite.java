package com.gestion.adhesion.models;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ReportingActivite {

    private String nomActivite;

    private Long nbInitee =0L;

    private Long nbPayee =0L;

    private Long nbValidee =0L;

    private Long nbF =0L;

    private Long nbM =0L;

    private Long cotisations =0L;

}