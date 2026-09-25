package com.wild.corp.adhesion.shop.payment.helloasso.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutBody;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutResponse;

@FeignClient(
        name = "helloAssoCheckoutClient",
        contextId = "helloAssoCheckoutClient",
        url = "${shop.payment.helloasso.base-url}"
)
public interface HelloAssoCheckoutClient {

    @PostMapping("/v5/organizations/{organizationSlug}/checkout-intents")
    HelloAssoApiV5CommonModelsCartsInitCheckoutResponse createCheckoutIntent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String organizationSlug,
            @RequestBody HelloAssoApiV5CommonModelsCartsInitCheckoutBody body);

    @GetMapping("/v5/organizations/{organizationSlug}/checkout-intents/{checkoutIntentId}")
    String getCheckoutIntent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String organizationSlug,
            @PathVariable String checkoutIntentId);
}
