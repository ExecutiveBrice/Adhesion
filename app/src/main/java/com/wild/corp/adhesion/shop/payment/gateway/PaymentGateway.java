package com.wild.corp.adhesion.shop.payment.gateway;

/**
 * Provider-neutral outbound payment port. Provider adapters are implemented outside the domain.
 */
public interface PaymentGateway {

    String providerKey();

    PaymentInitiation initiate(PaymentInitiationRequest request);

    PaymentVerification verify(String externalPaymentId);
}
