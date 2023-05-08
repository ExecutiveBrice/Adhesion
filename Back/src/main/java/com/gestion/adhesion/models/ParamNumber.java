package com.gestion.adhesion.models;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

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
