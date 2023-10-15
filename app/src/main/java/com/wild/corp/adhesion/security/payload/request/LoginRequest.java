package com.wild.corp.adhesion.security.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

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
