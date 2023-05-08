package com.gestion.security.payload.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class SignupRequest {
    @NotBlank
    @Size(min = 10, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

}
