package com.wild.corp.adhesion.services.helloasso;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.wild.corp.adhesion.client.helloasso.api.CheckoutIntentsManagementApi;

import java.util.Map;

@FeignClient(name = "helloAssoCheckoutClient", url = "${helloasso.base-url}")
public interface HelloAssoCheckoutClient extends CheckoutIntentsManagementApi {


}
