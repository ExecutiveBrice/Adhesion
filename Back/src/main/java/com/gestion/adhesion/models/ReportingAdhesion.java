package com.gestion.adhesion.models;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ReportingAdhesion {

    private String x;

    private Long nbInitiee =0L;
    private Long nbPayee =0L;
    private Long nbValidee =0L;

}