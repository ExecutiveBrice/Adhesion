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

    private Long nbEC =0L;

    private Long nbV =0L;

    private Long nbF =0L;

    private Long nbM =0L;

    private Long cotisations =0L;

}