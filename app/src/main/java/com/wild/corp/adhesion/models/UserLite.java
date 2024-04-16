package com.wild.corp.adhesion.models;

import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class UserLite {
	private Long id;

	private String adherent;

	private String username;

	private Set<Role> roles = new HashSet<>();

}
