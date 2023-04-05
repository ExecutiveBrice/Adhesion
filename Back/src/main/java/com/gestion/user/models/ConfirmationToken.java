package com.gestion.user.models;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static javax.persistence.GenerationType.SEQUENCE;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
@Entity
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

    private UUID confirmationToken;

    private LocalDate createdDate;

    @OneToOne
    private User user;




}
