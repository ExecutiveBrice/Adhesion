package com.wild.corp.adhesion.shop.payment.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "shop_payments")
public class Payment extends AuditableEntity {

    private static final Map<PaymentStatus, EnumSet<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.CANCELLED),
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.FAILED, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.CANCELLED),
            PaymentStatus.SUCCEEDED, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "expected_amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "expected_currency", nullable = false, length = 3))
    })
    private Money expectedAmount;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PaymentAttempt> attempts = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected Payment() {
        // Required by JPA.
    }

    public Payment(Long orderId, Money expectedAmount) {
        if (orderId == null) {
            throw new IllegalArgumentException("La commande est obligatoire");
        }
        this.orderId = orderId;
        this.expectedAmount = Objects.requireNonNull(expectedAmount, "Le montant attendu est obligatoire");
        this.status = PaymentStatus.CREATED;
    }

    public PaymentAttempt startAttempt(String providerKey, String idempotencyKey) {
        if (status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.REFUNDED) {
            throw new InvalidStatusTransitionException(status, PaymentStatus.PENDING);
        }
        if (status != PaymentStatus.PENDING) {
            transitionTo(PaymentStatus.PENDING);
        }
        PaymentAttempt attempt = new PaymentAttempt(this, providerKey, idempotencyKey, expectedAmount);
        attempts.add(attempt);
        return attempt;
    }

    public void recordSucceeded(PaymentAttempt attempt) {
        requireOwnedAttempt(attempt);
        attempt.getAmount().requireSameCurrency(expectedAmount);
        if (!attempt.getAmount().equals(expectedAmount)) {
            throw new IllegalStateException("Le montant payé ne correspond pas au montant de la commande");
        }
        attempt.markSucceeded();
        if (status != PaymentStatus.SUCCEEDED) {
            transitionTo(PaymentStatus.SUCCEEDED);
        }
    }

    public void recordFailed(PaymentAttempt attempt, String reason) {
        requireOwnedAttempt(attempt);
        attempt.markFailed(reason);
        if (status != PaymentStatus.FAILED) {
            transitionTo(PaymentStatus.FAILED);
        }
    }

    public void cancel() {
        transitionTo(PaymentStatus.CANCELLED);
    }

    public void refund(PaymentAttempt successfulAttempt) {
        requireOwnedAttempt(successfulAttempt);
        successfulAttempt.refund();
        transitionTo(PaymentStatus.REFUNDED);
    }

    private void requireOwnedAttempt(PaymentAttempt attempt) {
        if (attempt == null || attempt.getPayment() != this || !attempts.contains(attempt)) {
            throw new IllegalArgumentException("La tentative n'appartient pas à ce paiement");
        }
    }

    private void transitionTo(PaymentStatus targetStatus) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(targetStatus)) {
            throw new InvalidStatusTransitionException(status, targetStatus);
        }
        status = targetStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Money getExpectedAmount() {
        return expectedAmount;
    }

    public List<PaymentAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public long getVersion() {
        return version;
    }
}
