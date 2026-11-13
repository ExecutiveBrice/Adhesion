package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Bounded, fair scanning. Each payment uses its own transaction, independent of the browser. */
@Component
@ConditionalOnProperty(prefix = "shop.payment", name = "provider", havingValue = "helloasso")
public class PaymentReconciliationJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private final PaymentAttemptRepository attempts;
    private final PaymentService payments;
    private final boolean enabled;
    private final int batchSize;
    private long afterId;

    public PaymentReconciliationJob(PaymentAttemptRepository attempts, PaymentService payments,
                                    @Value("${shop.payment.reconciliation.enabled:true}") boolean enabled,
                                    @Value("${shop.payment.reconciliation.batch-size:50}") int batchSize) {
        if (batchSize < 1 || batchSize > 500) throw new IllegalArgumentException("Lot de vérification : 1 à 500 requis");
        this.attempts = attempts;
        this.payments = payments;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${shop.payment.reconciliation.fixed-delay-ms:120000}",
            initialDelayString = "${shop.payment.reconciliation.initial-delay-ms:30000}")
    public void reconcile() {
        if (!enabled) return;
        var page = PageRequest.of(0, batchSize);
        var ids = attempts.findPendingHelloAssoAttemptIds(afterId, page);
        if (ids.isEmpty() && afterId != 0) {
            afterId = 0;
            ids = attempts.findPendingHelloAssoAttemptIds(afterId, page);
        }
        for (Long id : ids) {
            try {
                payments.refreshPaymentStatus(id);
            } catch (RuntimeException exception) {
                // One provider/network/transaction failure must not roll back or block the other payments.
                LOGGER.warn("Vérification automatique du paiement {} en échec ; nouvelle tentative au prochain passage ({})",
                        id, exception.getClass().getSimpleName());
            } finally {
                afterId = id;
            }
        }
        if (ids.size() < batchSize) afterId = 0;
    }
}
