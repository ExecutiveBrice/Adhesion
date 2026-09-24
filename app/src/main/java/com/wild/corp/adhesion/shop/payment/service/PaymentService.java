package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.payment.model.Payment;
import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import com.wild.corp.adhesion.shop.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final ShopOrderRepository orderRepository;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAttemptRepository attemptRepository,
                          ShopOrderRepository orderRepository,
                          OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public Payment getOrCreatePayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElseGet(() -> {
            ShopOrder order = orderRepository.findById(orderId).orElseThrow();
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new IllegalStateException("La commande n'est pas en attente de paiement");
            }
            return paymentRepository.save(new Payment(order.getId(), order.getTotal()));
        });
    }

    public PaymentAttempt createAttempt(Long paymentId, String providerKey, String idempotencyKey) {
        return attemptRepository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .orElseGet(() -> {
                    Payment payment = paymentRepository.findById(paymentId).orElseThrow();
                    PaymentAttempt attempt = payment.startAttempt(providerKey, idempotencyKey);
                    paymentRepository.save(payment);
                    return attempt;
                });
    }

    public PaymentAttempt markAttemptPending(Long attemptId, String externalPaymentId, String redirectUrl) {
        PaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markPending(externalPaymentId, redirectUrl);
        return attempt;
    }

    public Payment markAttemptSucceeded(Long attemptId) {
        PaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        Payment payment = attempt.getPayment();
        ShopOrder order = orderRepository.findById(payment.getOrderId()).orElseThrow();
        payment.getExpectedAmount().requireSameCurrency(order.getTotal());
        if (!payment.getExpectedAmount().equals(order.getTotal())) {
            throw new IllegalStateException("Le montant du paiement ne correspond plus à la commande");
        }
        payment.recordSucceeded(attempt);
        if (order.getStatus() != OrderStatus.PAID) {
            orderService.consumeReservedStock(order);
            order.transitionTo(OrderStatus.PAID);
        }
        return payment;
    }

    public Payment markAttemptFailed(Long attemptId, String reason) {
        PaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        Payment payment = attempt.getPayment();
        payment.recordFailed(attempt, reason);
        return payment;
    }
}
