package com.wild.corp.adhesion.shop.payment.repository;

import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentIdAndIdempotencyKey(Long paymentId, String idempotencyKey);

    Optional<PaymentAttempt> findByProviderKeyAndExternalPaymentId(String providerKey, String externalPaymentId);
}
