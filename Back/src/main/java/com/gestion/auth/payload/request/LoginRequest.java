package com.gestion.auth.payload.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class LoginRequest {
	@NotBlank
	private String username;

	@NotBlank
	private String password;

}
