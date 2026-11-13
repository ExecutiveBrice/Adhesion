package com.wild.corp.adhesion.shop.payment.provider;

public class PaymentProviderException extends RuntimeException {

    private final PaymentProviderType providerType;
    private final String errorCode;
    private final boolean retryable;

    public PaymentProviderException(PaymentProviderType providerType,
                                    String errorCode,
                                    String message,
                                    boolean retryable) {
        this(providerType, errorCode, message, retryable, null);
    }

    public PaymentProviderException(PaymentProviderType providerType,
                                    String errorCode,
                                    String message,
                                    boolean retryable,
                                    Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public PaymentProviderType getProviderType() {
        return providerType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
