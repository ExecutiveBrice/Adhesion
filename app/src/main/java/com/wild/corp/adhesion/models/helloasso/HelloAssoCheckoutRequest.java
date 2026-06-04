package com.wild.corp.adhesion.models.helloasso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HelloAssoCheckoutRequest {

    @NotNull
    private Long adhesionId;

    @NotBlank
    private String returnUrl;

    @NotBlank
    private String errorUrl;

    @NotBlank
    private String backUrl;
}
