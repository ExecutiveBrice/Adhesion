package com.gestion.adhesion.models;


import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class Email {

    private String diffusion;
    private String subject;
    private String text;

}
