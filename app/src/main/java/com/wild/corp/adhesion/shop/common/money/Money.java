package com.wild.corp.adhesion.shop.common.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable monetary value stored in the smallest currency unit (cents for EUR).
 */
@Embeddable
public class Money {

    @Column(nullable = false)
    private long amountInCents;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Money() {
        // Required by JPA.
    }

    public Money(long amountInCents, String currency) {
        if (amountInCents < 0) {
            throw new IllegalArgumentException("Un montant monétaire ne peut pas être négatif");
        }
        this.amountInCents = amountInCents;
        this.currency = normalizeCurrency(currency);
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amountInCents, other.amountInCents), currency);
    }

    public Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive");
        }
        return new Money(Math.multiplyExact(amountInCents, quantity), currency);
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Le montant est obligatoire");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Les devises des montants doivent être identiques");
        }
    }

    public long getAmountInCents() {
        return amountInCents;
    }

    public String getCurrency() {
        return currency;
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("La devise est obligatoire");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        Currency.getInstance(normalized);
        return normalized;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Money money)) {
            return false;
        }
        return amountInCents == money.amountInCents && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountInCents, currency);
    }

    @Override
    public String toString() {
        return amountInCents + " " + currency;
    }
}
