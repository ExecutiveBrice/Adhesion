package com.wild.corp.adhesion.models;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "paramtext",
		uniqueConstraints = { 
			@UniqueConstraint(columnNames = "paramName"),
		})
public class ParamText {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	private String paramName;

	@Column(length = 1024)
	private String paramValue;

}
