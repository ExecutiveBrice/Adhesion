package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "users",
		uniqueConstraints = { 
			@UniqueConstraint(columnNames = "username"),
		})
@JsonIgnoreProperties(ignoreUnknown = true,   allowSetters = true)
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 255)
	private String username;

	@NotBlank
	@Size(max = 120)
	@JsonIgnore
	private String password;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_role_names", joinColumns = @JoinColumn(name = "user_id"))
	@Convert(converter = ERoleConverter.class)
	@Column(name = "role_name", length = 32, nullable = false)
	private Set<ERole> roles = new HashSet<>();

	private Boolean emailValid;

	@Column(nullable = false, columnDefinition = "bigint default 0")
	private long sessionVersion = 0;

	@OneToOne(mappedBy="user")
	@ToString.Exclude
	private Adherent adherent;

	@OneToMany(mappedBy="user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Notification> notifs = new HashSet<>();

	@OneToMany(mappedBy="user", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<ConfirmationToken> tokens = new HashSet<>();

	@OneToMany(mappedBy="user", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<PwaSessionToken> pwaSessions = new HashSet<>();

	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
