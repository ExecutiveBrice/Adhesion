package com.wild.corp.adhesion.models;


import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class EmailContent {

    private String diffusion;
    private String subject;
    private String text;

    public EmailContent(String diffusion, String subject, String text) {
        this.diffusion = diffusion;
        this.subject = subject;
        this.text = text;
    }
}
