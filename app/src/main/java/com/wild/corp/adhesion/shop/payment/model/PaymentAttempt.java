package com.wild.corp.adhesion.shop.payment.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "shop_payment_attempts")
public class PaymentAttempt extends AuditableEntity {

    private static final Map<PaymentStatus, EnumSet<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED,
                    PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.SUCCEEDED, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "provider_key", nullable = false, length = 80)
    private String providerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Column(name = "external_payment_id", length = 255)
    private String externalPaymentId;

    @Column(name = "redirect_url", length = 2048)
    private String redirectUrl;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private long version;

    protected PaymentAttempt() {
        // Required by JPA.
    }

    PaymentAttempt(Payment payment, String providerKey, String idempotencyKey, Money amount) {
        this.payment = payment;
        this.providerKey = normalizeProvider(providerKey);
        this.idempotencyKey = requireText(idempotencyKey, "La clé d'idempotence est obligatoire");
        this.amount = Objects.requireNonNull(amount, "Le montant de la tentative est obligatoire");
        this.status = PaymentStatus.CREATED;
    }

    public void markPending(String externalPaymentId, String redirectUrl) {
        transitionTo(PaymentStatus.PENDING);
        this.externalPaymentId = normalizeOptionalText(externalPaymentId);
        this.redirectUrl = normalizeOptionalText(redirectUrl);
        this.failureReason = null;
    }

    public void markSucceeded() {
        if (status == PaymentStatus.SUCCEEDED) {
            return;
        }
        transitionTo(PaymentStatus.SUCCEEDED);
    }

    public void markFailed(String reason) {
        if (status == PaymentStatus.FAILED) {
            return;
        }
        transitionTo(PaymentStatus.FAILED);
        failureReason = normalizeOptionalText(reason);
    }

    public void cancel() {
        if (status == PaymentStatus.CANCELLED) {
            return;
        }
        transitionTo(PaymentStatus.CANCELLED);
    }

    public void refund() {
        if (status == PaymentStatus.REFUNDED) {
            return;
        }
        transitionTo(PaymentStatus.REFUNDED);
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

    public Payment getPayment() {
        return payment;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public PaymentProviderType getProviderType() {
        return PaymentProviderType.valueOf(providerKey.toUpperCase(Locale.ROOT));
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Money getAmount() {
        return amount;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getVersion() {
        return version;
    }

    private static String normalizeProvider(String value) {
        return requireText(value, "Le fournisseur de paiement est obligatoire").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
