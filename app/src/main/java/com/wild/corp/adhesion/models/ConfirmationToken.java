package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;



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

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 32)
    private ConfirmationTokenType type;

    @Column(unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant expiresAt;

    private Instant usedAt;

    @ManyToOne
    @ToString.Exclude
    @JsonIgnore
    private User user;




}
