package com.gestion.emails.models;


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
