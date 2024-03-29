package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "users",
		uniqueConstraints = { 
			@UniqueConstraint(columnNames = "username"),
		})
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 50)
	private String username;

	@NotBlank
	@Size(max = 120)
	@JsonIgnore
	private String password;

	@ManyToMany
	private Set<Role> roles = new HashSet<>();

	private Boolean emailValid;

	@OneToOne(mappedBy="user")
	private Adherent adherent;

	@OneToMany(mappedBy="user", cascade = CascadeType.ALL)
	private Set<Notification> notifs = new HashSet<>();

	@OneToMany(mappedBy="user", cascade = CascadeType.ALL)
	@JsonIgnore
	private Set<ConfirmationToken> tokens = new HashSet<>();

	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
