package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.payment.config.PaymentProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral facade. Selection is configuration-driven and adapters can
 * be added without changing the order or payment domain services.
 */
@Component
public class PaymentGateway {

    private final PaymentProviderType configuredProvider;
    private final Map<PaymentProviderType, PaymentProvider> providers;

    public PaymentGateway(PaymentProperties properties, List<PaymentProvider> providers) {
        this.configuredProvider = properties.getProvider();
        EnumMap<PaymentProviderType, PaymentProvider> indexedProviders = new EnumMap<>(PaymentProviderType.class);
        for (PaymentProvider provider : providers) {
            PaymentProvider previous = indexedProviders.put(provider.type(), provider);
            if (previous != null) {
                throw new IllegalStateException("Plusieurs implémentations sont configurées pour " + provider.type());
            }
        }
        this.providers = Map.copyOf(indexedProviders);
    }

    public PaymentProviderType configuredProvider() {
        return configuredProvider;
    }

    public PaymentSession createPayment(PaymentRequest request) {
        return provider(configuredProvider).createPayment(request);
    }

    public PaymentResult retrievePayment(PaymentProviderType providerType, String externalPaymentId) {
        return provider(providerType).retrievePayment(externalPaymentId);
    }

    public PaymentResult processNotification(PaymentProviderType providerType, PaymentNotification notification) {
        return provider(providerType).processNotification(notification);
    }

    private PaymentProvider provider(PaymentProviderType providerType) {
        PaymentProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new PaymentProviderException(providerType, "PROVIDER_NOT_CONFIGURED",
                    "Aucun adaptateur de paiement n'est configuré pour " + providerType, false);
        }
        return provider;
    }
}
