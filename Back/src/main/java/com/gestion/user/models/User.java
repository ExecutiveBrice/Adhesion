package com.gestion.user.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
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
	@JsonIgnore
	private Set<ConfirmationToken> tokens = new HashSet<>();

	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
