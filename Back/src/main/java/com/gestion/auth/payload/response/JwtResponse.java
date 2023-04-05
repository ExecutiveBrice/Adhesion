package com.gestion.auth.payload.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class JwtResponse {
	private String token;
	private String type = "Bearer";
	private Long id;
	private String username;
	private Boolean enabled;
	private List<String> roles;

	public JwtResponse(String accessToken, Long id, String username, Boolean enabled, List<String> roles) {
		this.token = accessToken;
		this.id = id;
		this.username = username;
		this.enabled = enabled;
		this.roles = roles;
	}

}
