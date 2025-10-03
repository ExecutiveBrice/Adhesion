package com.wild.corp.adhesion.models;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ComptaActivite {

    private String nomActivite;

    private Integer helloAsso =0;

    private Integer helloAsso3x =0;

    private Integer cheque =0;

    private Integer cheque3x =0;

    private Integer passport =0;

    private Integer espece =0;

    private Integer intermarche =0;

    private Integer autre =0;
}