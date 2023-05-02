package com.gestion.user.models;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "param",
		uniqueConstraints = { 
			@UniqueConstraint(columnNames = "paramName"),
		})
public class Param {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	private String paramName;

	private String paramValue;

}
