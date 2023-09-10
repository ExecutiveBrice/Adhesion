package com.gestion.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static javax.persistence.GenerationType.SEQUENCE;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmationToken {

    @Id
    @SequenceGenerator(
            name = "token_sequence",
            sequenceName = "token_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "token_sequence"
    )
    @Column(name = "id", updatable = false)
    private Long id;

    private String type;

    @Type(type="org.hibernate.type.PostgresUUIDType")
    private UUID confirmationToken;

    private LocalDate createdDate;

    @ManyToOne
    @ToString.Exclude
    @JsonIgnore
    private User user;




}
