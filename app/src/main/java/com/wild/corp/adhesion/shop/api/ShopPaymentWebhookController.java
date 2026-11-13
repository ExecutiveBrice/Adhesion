package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.payment.helloasso.HelloAssoWebhookService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "shop.payment", name = "provider", havingValue = "helloasso")
public class ShopPaymentWebhookController {
    private final HelloAssoWebhookService notifications;

    public ShopPaymentWebhookController(HelloAssoWebhookService notifications) {
        this.notifications = notifications;
    }

    // Public server-to-server endpoint. No order data is returned; the payload is never proof of payment.
    @PostMapping(value = "/shop/payments/helloasso/notifications", consumes = "application/json")
    public ResponseEntity<Void> receive(@RequestBody byte[] body,
                                       @RequestHeader(value = "x-ha-signature", required = false) String signature) {
        notifications.receive(body, signature);
        return ResponseEntity.ok().build(); // HelloAsso specifically expects HTTP 200.
    }
}
