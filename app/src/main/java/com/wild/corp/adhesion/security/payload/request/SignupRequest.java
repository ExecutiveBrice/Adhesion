package com.wild.corp.adhesion.security.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


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
