package com.wild.corp.adhesion.services.helloasso;

import com.wild.corp.adhesion.models.helloasso.HelloAssoTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "helloAssoAuthClient", url = "${helloasso.oauth-url}")
public interface HelloAssoAuthClient {

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    HelloAssoTokenResponse getAccessToken(@RequestBody MultiValueMap<String, String> form);
}
