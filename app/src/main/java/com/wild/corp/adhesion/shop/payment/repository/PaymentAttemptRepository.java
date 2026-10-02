package com.wild.corp.adhesion.shop.payment.repository;

import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentIdAndIdempotencyKey(Long paymentId, String idempotencyKey);

    Optional<PaymentAttempt> findByProviderKeyAndExternalPaymentId(String providerKey, String externalPaymentId);

    // Scalar lookups avoid loading stale payment state before acquiring its lock.
    @Query("select a.payment.id from PaymentAttempt a where a.id = :id")
    Optional<Long> findPaymentIdByAttemptId(Long id);

    @Query("select a.id from PaymentAttempt a where a.providerKey = :providerKey and a.externalPaymentId = :externalId")
    Optional<Long> findIdByProviderAndExternalId(String providerKey, String externalId);

    @Query("""
            select a.id from PaymentAttempt a where a.providerKey = :providerKey
            and a.payment.orderId = :orderId and a.idempotencyKey = :key
            and a.externalPaymentId is not null
            """)
    Optional<Long> findIdByCheckoutMetadata(String providerKey, Long orderId, String key);

    @Query("""
            select a.id from PaymentAttempt a, ShopOrder o
            where o.id = a.payment.orderId and o.status = 'PENDING_PAYMENT'
            and a.status = 'PENDING' and a.providerKey = 'helloasso'
            and a.externalPaymentId is not null and a.id > :afterId
            order by a.id
            """)
    List<Long> findPendingHelloAssoAttemptIds(Long afterId, Pageable pageable);
}
