package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.repository.PaymentRepository;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class OrderExpiryService {
    // HelloAsso documents that an unconfirmed checkout can be considered abandoned after 45 minutes.
    private static final Duration CHECKOUT_GRACE = Duration.ofMinutes(45);

    private final ShopOrderRepository orders;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final OrderService orderService;

    public OrderExpiryService(ShopOrderRepository orders, PaymentRepository paymentRepository,
                              PaymentService paymentService, OrderService orderService) {
        this.orders = orders;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @Transactional
    public boolean expireIfUnpaid(Long orderId, Instant now) {
        // All confirmation paths lock this payment. Keep the lock through stock release.
        var payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
                return false;
            }
            boolean pending = payment.getAttempts().stream()
                    .anyMatch(attempt -> attempt.getStatus() == PaymentStatus.PENDING
                            && attempt.getExternalPaymentId() != null);
            if (pending) {
                paymentService.refreshPaymentStatusForOrder(orderId);
                if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
                    return false;
                }
                boolean recentCheckout = payment.getAttempts().stream()
                        .filter(attempt -> attempt.getStatus() == PaymentStatus.PENDING)
                        .map(PaymentAttempt::getCreatedAt)
                        .anyMatch(createdAt -> createdAt == null || createdAt.isAfter(now.minus(CHECKOUT_GRACE)));
                if (recentCheckout) return false;
            }
        }
        ShopOrder order = orders.findByIdForUpdate(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) return false;
        return orderService.expireUnpaidOrder(orderId, now);
    }
}
