package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;

@Component
public class OrderExpiryJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderExpiryJob.class);
    private static final int BATCH_SIZE = 50;
    private final ShopOrderRepository orders;
    private final OrderExpiryService expiry;
    private final Clock clock;
    private long afterId;

    @Autowired
    public OrderExpiryJob(ShopOrderRepository orders, OrderExpiryService expiry) {
        this(orders, expiry, Clock.systemUTC());
    }

    public OrderExpiryJob(ShopOrderRepository orders, OrderExpiryService expiry, Clock clock) {
        this.orders = orders;
        this.expiry = expiry;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${shop.order.expiry-scan-delay-ms:60000}",
            initialDelayString = "${shop.order.expiry-initial-delay-ms:30000}")
    public void expireOverdueOrders() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(ShopOrder.PAYMENT_WINDOW);
        var page = PageRequest.of(0, BATCH_SIZE);
        var ids = orders.findExpiredPendingOrderIds(cutoff, afterId, page);
        if (ids.isEmpty() && afterId != 0) {
            afterId = 0;
            ids = orders.findExpiredPendingOrderIds(cutoff, afterId, page);
        }
        for (Long id : ids) {
            try {
                if (expiry.expireIfUnpaid(id, now)) LOGGER.info("Commande boutique {} expirée après 24 heures", id);
            } catch (RuntimeException exception) {
                LOGGER.warn("Expiration de la commande boutique {} reportée ({})", id,
                        exception.getClass().getSimpleName());
            } finally {
                afterId = id;
            }
        }
        if (ids.size() < BATCH_SIZE) afterId = 0;
    }
}
