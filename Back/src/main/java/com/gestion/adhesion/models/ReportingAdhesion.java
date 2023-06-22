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

    private Long nbEC =0L;

    private Long nbV =0L;

}