package com.wild.corp.adhesion.shop.payment.helloasso.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.util.MultiValueMap;

@FeignClient(
        name = "helloAssoOAuthClient",
        contextId = "helloAssoOAuthClient",
        url = "${shop.payment.helloasso.base-url}"
)
public interface HelloAssoOAuthClient {

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    HelloAssoOAuthTokenResponse issueToken(@RequestBody MultiValueMap<String, String> form);
}
