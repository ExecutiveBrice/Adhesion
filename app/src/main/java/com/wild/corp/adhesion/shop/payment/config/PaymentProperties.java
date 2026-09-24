package com.wild.corp.adhesion.shop.payment.config;

import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shop.payment")
public class PaymentProperties {

    private PaymentProviderType provider = PaymentProviderType.MANUAL;

    public PaymentProviderType getProvider() {
        return provider;
    }

    public void setProvider(PaymentProviderType provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Le fournisseur de paiement est obligatoire");
        }
        this.provider = provider;
    }
}
