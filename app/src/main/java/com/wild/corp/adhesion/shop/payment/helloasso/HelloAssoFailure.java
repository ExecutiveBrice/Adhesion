package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import feign.FeignException;

final class HelloAssoFailure {

    private HelloAssoFailure() {
    }

    static PaymentProviderException from(FeignException exception, String operation) {
        int status = exception.status();
        boolean retryable = status == -1 || status == 408 || status == 429 || status >= 500;
        String code = status > 0 ? operation + "_HTTP_" + status : operation + "_NETWORK";
        // Ne pas propager le corps HTTP : il peut contenir des données envoyées à HelloAsso.
        return new PaymentProviderException(PaymentProviderType.HELLOASSO, code,
                "L'appel HelloAsso a échoué pendant " + operation, retryable, exception);
    }
}
