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
@Table(	name = "paramnumber",
		uniqueConstraints = { 
			@UniqueConstraint(columnNames = "paramName"),
		})
public class ParamNumber {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	private String paramName;

	private Integer paramValue;

}
