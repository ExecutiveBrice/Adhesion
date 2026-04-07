package com.wild.corp.adhesion.services.helloasso;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "helloAssoCheckoutClient", url = "${helloasso.base-url}")
public interface HelloAssoCheckoutClient {

    @PostMapping("/organizations/{organizationSlug}/checkout-intents")
    Map<String, Object> createCheckoutIntent(@RequestHeader("Authorization") String authorization,
                                             @PathVariable String organizationSlug,
                                             @RequestBody Map<String, Object> body);

    @GetMapping("/organizations/{organizationSlug}/checkout-intents/{checkoutIntentId}")
    Map<String, Object> getCheckoutIntent(@RequestHeader("Authorization") String authorization,
                                          @PathVariable String organizationSlug,
                                          @PathVariable Integer checkoutIntentId);
}
