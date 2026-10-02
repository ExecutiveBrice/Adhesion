package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import com.wild.corp.adhesion.shop.order.service.OrderNumberGenerator;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.order.service.OrderExpiryService;
import com.wild.corp.adhesion.shop.order.service.OrderExpiryJob;
import com.wild.corp.adhesion.shop.payment.model.Payment;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.*;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = PaymentReconciliationIntegrationTest.JpaApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentReconciliationIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.wild.corp.adhesion.shop")
    @EnableJpaRepositories("com.wild.corp.adhesion.shop")
    @Import({PaymentService.class, OrderService.class, OrderExpiryService.class})
    static class JpaApplication {}

    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PaymentService payments;
    @Autowired private PaymentAttemptRepository attempts;
    @Autowired private ShopOrderRepository orders;
    @Autowired private ProductVariantRepository variants;
    @Autowired private OrderExpiryService expiry;
    @MockitoBean private PaymentGateway gateway;
    @MockitoBean private UserRepository users;
    @MockitoBean private OrderNumberGenerator numbers;

    @Test
    void orderNeverSentToPaymentProviderAlsoExpiresAndReleasesItsReservation() {
        Checkout checkout = checkout();
        Instant now = Instant.now();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Long paymentId = attempts.findById(checkout.attemptId()).orElseThrow().getPayment().getId();
            em.createNativeQuery("delete from shop_payment_attempts where payment_id = :id")
                    .setParameter("id", paymentId).executeUpdate();
            em.createNativeQuery("delete from shop_payments where id = :id")
                    .setParameter("id", paymentId).executeUpdate();
            em.clear();
        });
        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofHours(1)));

        assertThat(expiry.expireIfUnpaid(orderId(checkout), now)).isTrue();
        assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.EXPIRED);
        OrderItemStatus expiredItemStatus =
                new TransactionTemplate(transactionManager).execute(status -> orders.findByOrderNumber(checkout.orderNumber())
                        .orElseThrow().getItems().getFirst().getStatus());
        assertThat(expiredItemStatus)
                .isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(variants.findById(checkout.variantId()).orElseThrow().getStockReserved()).isZero();
        verifyNoInteractions(gateway);
    }

    @Test
    void overdueOrderWithoutConfirmedPaymentExpiresAndReleasesStock() {
        Checkout checkout = checkout();
        Instant now = Instant.now();
        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofHours(1)));
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId()))
                .thenReturn(new PaymentResult(checkout.externalId(), PaymentStatus.PENDING, new Money(3000, "EUR"), null, null));

        new OrderExpiryJob(orders, expiry, Clock.fixed(now, ZoneOffset.UTC)).expireOverdueOrders();

        assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.EXPIRED);
        var variant = variants.findById(checkout.variantId()).orElseThrow();
        assertThat(variant.getStockOnHand()).isEqualTo(10);
        assertThat(variant.getStockReserved()).isZero();
        verify(gateway).retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId());
        assertThat(attempts.findPendingHelloAssoAttemptIds(0L, PageRequest.of(0, 500)))
                .doesNotContain(checkout.attemptId());
    }

    @Test
    void successfulPaymentAtDeadlineWinsAndConsumesStockOnlyOnce() {
        Checkout checkout = checkout();
        Instant now = Instant.now();
        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofHours(1)));
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId())).thenReturn(success(checkout));

        assertThat(expiry.expireIfUnpaid(orderId(checkout), now)).isFalse();
        assertPaidOnce(checkout);
    }

    @Test
    void recentCheckoutDelaysReleaseButOldAbandonedCheckoutEventuallyExpires() {
        Checkout checkout = checkout();
        Instant now = Instant.now();
        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofMinutes(10)));
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId()))
                .thenReturn(new PaymentResult(checkout.externalId(), PaymentStatus.PENDING, new Money(3000, "EUR"), null, null));

        assertThat(expiry.expireIfUnpaid(orderId(checkout), now)).isFalse();
        assertThat(variants.findById(checkout.variantId()).orElseThrow().getStockReserved()).isEqualTo(2);

        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofMinutes(46)));
        assertThat(expiry.expireIfUnpaid(orderId(checkout), now)).isTrue();
        assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(variants.findById(checkout.variantId()).orElseThrow().getStockReserved()).isZero();
    }

    @Test
    void providerOutageLeavesReservationAndOrderUntouchedForRetry() {
        Checkout checkout = checkout();
        Instant now = Instant.now();
        ageCheckout(checkout, now.minus(Duration.ofHours(25)), now.minus(Duration.ofHours(1)));
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId()))
                .thenThrow(new PaymentProviderException(PaymentProviderType.HELLOASSO, "TIMEOUT", "Indisponible", true));

        assertThatThrownBy(() -> expiry.expireIfUnpaid(orderId(checkout), now))
                .isInstanceOf(PaymentProviderException.class);
        assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(variants.findById(checkout.variantId()).orElseThrow().getStockReserved()).isEqualTo(2);
    }

    @Test
    void expiredOrderCannotStartAnotherPaymentSession() {
        Checkout checkout = checkout();
        ageCheckout(checkout, Instant.now().minus(Duration.ofHours(25)), Instant.now().minus(Duration.ofHours(1)));
        assertThatThrownBy(() -> payments.createPaymentSession(orderId(checkout), "another-key",
                java.net.URI.create("https://example.test/return"), java.net.URI.create("https://example.test/cancel")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("24 heures");
        verify(gateway, never()).createPayment(any());
    }

    @Test
    void periodicReconciliationConfirmsPaymentWithoutBrowserAndRemovesItFromPendingScan() {
        Checkout checkout = checkout();
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId()))
                .thenReturn(success(checkout));
        assertThat(attempts.findPendingHelloAssoAttemptIds(0L, PageRequest.of(0, 500)))
                .contains(checkout.attemptId());

        new PaymentReconciliationJob(attempts, payments, true, 500).reconcile();

        assertPaidOnce(checkout);
        assertThat(attempts.findPendingHelloAssoAttemptIds(0L, PageRequest.of(0, 500)))
                .doesNotContain(checkout.attemptId());
    }

    @Test
    void webhookManualAndPeriodicVerificationCanArriveTogetherWithoutDoubleStockConsumption() throws Exception {
        Checkout checkout = checkout();
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId())).thenReturn(success(checkout));
        var notification = new PaymentNotification("notification", Map.of());
        when(gateway.processNotification(PaymentProviderType.HELLOASSO, notification)).thenReturn(success(checkout));
        var ready = new CountDownLatch(3);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(3)) {
            var automatic = executor.submit(() -> {
                ready.countDown(); start.await();
                return payments.refreshPaymentStatus(checkout.attemptId());
            });
            var manual = executor.submit(() -> {
                ready.countDown(); start.await();
                return payments.verifyPaymentForManager(checkout.orderNumber());
            });
            var webhook = executor.submit(() -> {
                ready.countDown(); start.await();
                return payments.processNotification(PaymentProviderType.HELLOASSO, notification);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            automatic.get(15, TimeUnit.SECONDS);
            manual.get(15, TimeUnit.SECONDS);
            webhook.get(15, TimeUnit.SECONDS);
        }
        assertPaidOnce(checkout);
    }

    @Test
    void invalidAmountCannotConfirmPaymentOrConsumeStockAndCanBeRetried() {
        Checkout checkout = checkout();
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId()))
                .thenReturn(new PaymentResult(checkout.externalId(), PaymentStatus.SUCCEEDED, new Money(1, "EUR"), null, null));
        assertThatThrownBy(() -> payments.refreshPaymentStatus(checkout.attemptId())).isInstanceOf(PaymentProviderException.class);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            var variant = variants.findById(checkout.variantId()).orElseThrow();
            assertThat(variant.getStockOnHand()).isEqualTo(10);
            assertThat(variant.getStockReserved()).isEqualTo(2);
        });
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId())).thenReturn(success(checkout));
        payments.refreshPaymentStatus(checkout.attemptId());
        assertPaidOnce(checkout);
    }

    @Test
    void manualVerificationChecksOlderPendingAttemptsAndIsIdempotentAfterSuccess() {
        Checkout checkout = checkout();
        String secondId = checkout.externalId() + "-second";
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var payment = attempts.findById(checkout.attemptId()).orElseThrow().getPayment();
            payment.startAttempt(PaymentProviderType.HELLOASSO, "second-key").markPending(secondId, "https://example.test");
        });
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, secondId))
                .thenReturn(new PaymentResult(secondId, PaymentStatus.PENDING, new Money(3000, "EUR"), null, null));
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId())).thenReturn(success(checkout));

        payments.verifyPaymentForManager(checkout.orderNumber());
        payments.verifyPaymentForManager(checkout.orderNumber());

        assertPaidOnce(checkout);
        verify(gateway).retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId());
        verify(gateway).retrievePayment(PaymentProviderType.HELLOASSO, secondId);
    }

    @Test
    void duplicateOrStaleNotificationsDoNotDowngradePaidOrRefundedOrders() {
        Checkout checkout = checkout();
        when(gateway.retrievePayment(PaymentProviderType.HELLOASSO, checkout.externalId())).thenReturn(success(checkout));
        payments.refreshPaymentStatus(checkout.attemptId());
        var notification = new PaymentNotification("notification", Map.of());
        when(gateway.processNotification(PaymentProviderType.HELLOASSO, notification))
                .thenReturn(new PaymentResult(checkout.externalId(), PaymentStatus.FAILED, new Money(0, "EUR"), null, null))
                .thenReturn(new PaymentResult(checkout.externalId(), PaymentStatus.REFUNDED, new Money(3000, "EUR"), null, null))
                .thenReturn(success(checkout));
        payments.processNotification(PaymentProviderType.HELLOASSO, notification);
        assertPaidOnce(checkout);
        payments.processNotification(PaymentProviderType.HELLOASSO, notification);
        payments.processNotification(PaymentProviderType.HELLOASSO, notification);
        assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REFUNDED);
        OrderItemStatus refundedItemStatus =
                new TransactionTemplate(transactionManager).execute(status -> orders.findByOrderNumber(checkout.orderNumber())
                        .orElseThrow().getItems().getFirst().getStatus());
        assertThat(refundedItemStatus)
                .isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(variants.findById(checkout.variantId()).orElseThrow().getStockOnHand()).isEqualTo(8);
    }

    private Checkout checkout() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String unique = UUID.randomUUID().toString();
            Product product = new Product("Tee-shirt", unique, null, true, 0);
            em.persist(product);
            ProductVariant variant = new ProductVariant(product, unique, "M", new Money(1500, "EUR"), true, 0);
            variant.trackStock(10);
            variant.reserveStock(2);
            em.persist(variant);
            ShopOrder order = new ShopOrder(unique, 42L, "EUR");
            order.addItem(OrderItem.snapshot(product.getId(), product.getName(), variant.getId(), "M", unique,
                    new Money(1500, "EUR"), 2, true));
            order.submitForPayment();
            em.persist(order);
            Payment payment = new Payment(order.getId(), order.getTotal());
            var attempt = payment.startAttempt(PaymentProviderType.HELLOASSO, "first-key");
            attempt.markPending(unique, "https://example.test");
            em.persist(payment);
            em.flush();
            return new Checkout(order.getOrderNumber(), variant.getId(), attempt.getId(), unique);
        });
    }

    private PaymentResult success(Checkout checkout) {
        return new PaymentResult(checkout.externalId(), PaymentStatus.SUCCEEDED, new Money(3000, "EUR"), null, null);
    }

    private Long orderId(Checkout checkout) {
        return orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getId();
    }

    private void ageCheckout(Checkout checkout, Instant orderCreatedAt, Instant attemptCreatedAt) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            em.createNativeQuery("update shop_orders set created_at = :date where order_number = :number")
                    .setParameter("date", orderCreatedAt).setParameter("number", checkout.orderNumber()).executeUpdate();
            em.createNativeQuery("update shop_payment_attempts set created_at = :date where id = :id")
                    .setParameter("date", attemptCreatedAt).setParameter("id", checkout.attemptId()).executeUpdate();
            em.clear();
        });
    }

    private void assertPaidOnce(Checkout checkout) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(orders.findByOrderNumber(checkout.orderNumber()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
            var variant = variants.findById(checkout.variantId()).orElseThrow();
            assertThat(variant.getStockOnHand()).isEqualTo(8);
            assertThat(variant.getStockReserved()).isZero();
        });
    }

    private record Checkout(String orderNumber, Long variantId, Long attemptId, String externalId) {}
}
