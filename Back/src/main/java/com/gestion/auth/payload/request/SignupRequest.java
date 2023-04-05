package com.gestion.auth.payload.request;

import lombok.*;

import java.util.Set;

import javax.validation.constraints.*;

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
