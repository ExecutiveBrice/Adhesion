package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;



@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;


    private UUID confirmationToken;

    private LocalDate createdDate;

    @ManyToOne
    @ToString.Exclude
    @JsonIgnore
    private User user;




}
