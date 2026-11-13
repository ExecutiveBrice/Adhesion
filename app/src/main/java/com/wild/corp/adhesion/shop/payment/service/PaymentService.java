package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.payment.model.Payment;
import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import com.wild.corp.adhesion.shop.payment.repository.PaymentRepository;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.PaymentGateway;
import com.wild.corp.adhesion.shop.payment.provider.PaymentLine;
import com.wild.corp.adhesion.shop.payment.provider.PaymentPayer;
import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.provider.PaymentRequest;
import com.wild.corp.adhesion.shop.payment.provider.PaymentResult;
import com.wild.corp.adhesion.shop.payment.provider.PaymentSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Comparator;
import java.time.Instant;

@Service
@Transactional
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final ShopOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAttemptRepository attemptRepository,
                          ShopOrderRepository orderRepository,
                          UserRepository userRepository,
                          OrderService orderService,
                          PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
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

    private PaymentAttempt createAttempt(Payment payment, PaymentProviderType providerType, String idempotencyKey) {
        return attemptRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), idempotencyKey)
                .orElseGet(() -> {
                    payment.startAttempt(providerType, idempotencyKey);
                    Payment savedPayment = paymentRepository.save(payment);
                    return savedPayment.getAttempts().stream()
                            .filter(candidate -> idempotencyKey.equals(candidate.getIdempotencyKey()))
                            .findFirst()
                            .orElseThrow();
                });
    }

    @Transactional(dontRollbackOn = PaymentProviderException.class)
    public PaymentSession createPaymentSession(Long orderId,
                                               String idempotencyKey,
                                               URI returnUrl,
                                               URI cancelUrl) {
        ShopOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("La commande n'est pas en attente de paiement");
        }
        if (order.getPaymentExpiresAt() != null && !Instant.now().isBefore(order.getPaymentExpiresAt())) {
            throw new IllegalStateException("Le délai de paiement de 24 heures est écoulé");
        }
        Payment payment = getOrCreatePayment(orderId);
        PaymentProviderType providerType = paymentGateway.configuredProvider();
        PaymentAttempt attempt = createAttempt(payment, providerType, idempotencyKey);
        Payment owningPayment = attempt.getPayment();
        requireProvider(attempt, providerType);

        PaymentSession persistedSession = persistedSession(attempt);
        if (persistedSession != null) {
            return persistedSession;
        }
        if (attempt.getStatus() == PaymentStatus.FAILED || attempt.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Cette tentative est terminée ; utilisez une nouvelle clé d'idempotence");
        }

        PaymentRequest request = new PaymentRequest(order.getId(), order.getOrderNumber(), owningPayment.getExpectedAmount(),
                idempotencyKey, returnUrl, cancelUrl, order.getItems().stream()
                .map(item -> new PaymentLine(item.getProductName(), item.getVariantName(), item.getQuantity()))
                .toList(), payerFor(order.getCustomerUserId()));
        try {
            PaymentSession session = paymentGateway.createPayment(request);
            attempt.markPending(session.externalPaymentId(), uriToString(session.redirectUrl()));
            if (session.status() == PaymentStatus.SUCCEEDED) {
                completePayment(owningPayment, attempt, order);
            }
            return session;
        } catch (PaymentProviderException exception) {
            owningPayment.recordFailed(attempt, exception.getMessage());
            LOGGER.warn("Échec du fournisseur de paiement pour la commande {} : {} ({})",
                    order.getOrderNumber(), exception.getProviderType(), exception.getErrorCode());
            throw exception;
        }
    }

    private PaymentPayer payerFor(Long userId) {
        return userRepository.findById(userId).map(user -> {
            Adherent adherent = user.getAdherent();
            if (adherent != null && Boolean.TRUE.equals(adherent.getMineur())
                    && adherent.getRepresentant() != null) {
                adherent = adherent.getRepresentant();
                User representative = adherent.getUser();
                if (representative != null) {
                    user = representative;
                }
            }
            return new PaymentPayer(adherent == null ? null : adherent.getPrenom(),
                    adherent == null ? null : adherent.getNom(), user.getUsername());
        }).orElse(null);
    }

    public Payment refreshPaymentStatus(Long attemptId) {
        Long paymentId = attemptRepository.findPaymentIdByAttemptId(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Tentative de paiement introuvable"));
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        PaymentAttempt attempt = payment.getAttempts().stream().filter(candidate -> attemptId.equals(candidate.getId()))
                .findFirst().orElseThrow();
        return refreshAttempt(attempt);
    }

    private Payment refreshAttempt(PaymentAttempt attempt) {
        if (attempt.getExternalPaymentId() == null) {
            throw new IllegalStateException("La tentative ne possède pas encore d'identifiant externe");
        }
        PaymentResult result = paymentGateway.retrievePayment(attempt.getProviderType(), attempt.getExternalPaymentId());
        return applyVerifiedResult(attempt, result);
    }

    public Payment refreshPaymentStatusForOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException("Aucun paiement n’a encore été lancé pour cette commande"));
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }
        var pendingAttempts = payment.getAttempts().stream()
                .filter(candidate -> candidate.getExternalPaymentId() != null)
                .filter(candidate -> candidate.getStatus() == PaymentStatus.PENDING)
                .sorted(Comparator.comparing(PaymentAttempt::getId).reversed()).toList();
        if (pendingAttempts.isEmpty()) {
            throw new IllegalStateException("Aucune tentative de paiement en attente de confirmation");
        }
        for (PaymentAttempt attempt : pendingAttempts) {
            refreshAttempt(attempt);
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) break;
        }
        return payment;
    }

    public ShopOrder verifyPaymentForManager(String orderNumber) {
        Long orderId = orderRepository.findIdByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable"));
        refreshPaymentStatusForOrder(orderId);
        return orderRepository.findById(orderId).orElseThrow();
    }

    public Payment processNotification(PaymentProviderType providerType, PaymentNotification notification) {
        PaymentResult result = paymentGateway.processNotification(providerType, notification);
        Long attemptId = attemptRepository.findIdByProviderAndExternalId(
                        providerType.name().toLowerCase(Locale.ROOT), result.externalPaymentId())
                .orElseThrow(() -> new NoSuchElementException("Tentative de paiement introuvable"));
        Long paymentId = attemptRepository.findPaymentIdByAttemptId(attemptId).orElseThrow();
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        PaymentAttempt attempt = payment.getAttempts().stream().filter(candidate -> attemptId.equals(candidate.getId()))
                .findFirst().orElseThrow();
        requireProvider(attempt, providerType);
        return applyVerifiedResult(attempt, result);
    }

    private Payment applyVerifiedResult(PaymentAttempt attempt, PaymentResult result) {
        Payment payment = attempt.getPayment();
        validateResult(attempt, payment, result);
        // Replayed or delayed notifications must never downgrade a confirmed payment.
        if (payment.getStatus() == PaymentStatus.REFUNDED
                || (attempt.getStatus() == PaymentStatus.SUCCEEDED
                && result.status() != PaymentStatus.REFUNDED && result.status() != PaymentStatus.SUCCEEDED)) {
            return payment;
        }
        ShopOrder order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable"));

        return switch (result.status()) {
            case CREATED -> throw invalidProviderResponse(attempt, "Le fournisseur a retourné un statut non vérifiable");
            case PENDING -> payment;
            case SUCCEEDED -> {
                completePayment(payment, attempt, order);
                yield payment;
            }
            case FAILED -> {
                payment.recordFailed(attempt, failureReason(result));
                yield payment;
            }
            case CANCELLED -> {
                payment.recordCancelledAttempt(attempt);
                yield payment;
            }
            case REFUNDED -> {
                payment.refund(attempt);
                if (order.getStatus() != OrderStatus.REFUNDED) {
                    order.transitionTo(OrderStatus.REFUNDED);
                }
                yield payment;
            }
        };
    }

    private void completePayment(Payment payment, PaymentAttempt attempt, ShopOrder order) {
        payment.getExpectedAmount().requireSameCurrency(order.getTotal());
        if (!payment.getExpectedAmount().equals(order.getTotal())) {
            throw new IllegalStateException("Le montant du paiement ne correspond plus à la commande");
        }
        payment.recordSucceeded(attempt);
        if (order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.CANCELLED) {
            LOGGER.error("Paiement confirmé pour la commande {} après sa clôture ; intervention nécessaire", order.getOrderNumber());
            throw new IllegalStateException("Paiement confirmé après la clôture de la commande ; intervention nécessaire");
        }
        if (order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.PROCESSING
                && order.getStatus() != OrderStatus.COMPLETED) {
            orderService.consumeReservedStock(order);
            order.transitionTo(OrderStatus.PAID);
        }
    }

    private void validateResult(PaymentAttempt attempt, Payment payment, PaymentResult result) {
        if (!attempt.getExternalPaymentId().equals(result.externalPaymentId())) {
            throw invalidProviderResponse(attempt, "L'identifiant externe vérifié ne correspond pas à la tentative");
        }
        if (result.status() == PaymentStatus.SUCCEEDED || result.status() == PaymentStatus.REFUNDED) {
            payment.getExpectedAmount().requireSameCurrency(result.amount());
            // HelloAsso peut inclure une contribution supplémentaire, mais le montant
            // vérifié doit au moins couvrir celui de la commande boutique.
            boolean invalidAmount = attempt.getProviderType() == PaymentProviderType.HELLOASSO
                    ? result.amount().getAmountInCents() < payment.getExpectedAmount().getAmountInCents()
                    : !payment.getExpectedAmount().equals(result.amount());
            if (invalidAmount) {
                throw invalidProviderResponse(attempt, "Le montant vérifié ne correspond pas au montant attendu");
            }
        }
    }

    private PaymentProviderException invalidProviderResponse(PaymentAttempt attempt, String message) {
        return new PaymentProviderException(attempt.getProviderType(), "INVALID_PROVIDER_RESPONSE", message, false);
    }

    private void requireProvider(PaymentAttempt attempt, PaymentProviderType providerType) {
        if (attempt.getProviderType() != providerType) {
            throw new IllegalStateException("La clé d'idempotence appartient à un autre fournisseur de paiement");
        }
    }

    private PaymentSession persistedSession(PaymentAttempt attempt) {
        if (attempt.getExternalPaymentId() == null
                || (attempt.getStatus() != PaymentStatus.PENDING && attempt.getStatus() != PaymentStatus.SUCCEEDED)) {
            return null;
        }
        URI redirectUrl = attempt.getRedirectUrl() == null ? null : URI.create(attempt.getRedirectUrl());
        return new PaymentSession(attempt.getExternalPaymentId(), redirectUrl, attempt.getStatus());
    }

    private String failureReason(PaymentResult result) {
        if (result.failureMessage() != null) {
            return result.failureMessage();
        }
        return result.failureCode() == null ? "Paiement refusé par le fournisseur" : result.failureCode();
    }

    private String uriToString(URI uri) {
        return uri == null ? null : uri.toASCIIString();
    }
}
