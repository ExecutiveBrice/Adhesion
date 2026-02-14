package com.wild.corp.adhesion.models.helloasso;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelloAssoCheckoutResponse {
    private Integer checkoutIntentId;
    private String redirectUrl;
}
