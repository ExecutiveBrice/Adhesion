package com.wild.corp.adhesion.shop.payment.provider;

/**
 * Port sortant minimal vers un fournisseur de paiement. Une implémentation ne
 * doit exposer aucune classe propre à son SDK au domaine boutique.
 */
public interface PaymentProvider {

    PaymentProviderType type();

    PaymentSession createPayment(PaymentRequest request) throws PaymentProviderException;

    PaymentResult retrievePayment(String externalPaymentId) throws PaymentProviderException;

    PaymentResult processNotification(PaymentNotification notification) throws PaymentProviderException;
}
