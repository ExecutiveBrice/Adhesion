package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderMessageRequest(@NotBlank @Size(max = 2000) String content) {
}
