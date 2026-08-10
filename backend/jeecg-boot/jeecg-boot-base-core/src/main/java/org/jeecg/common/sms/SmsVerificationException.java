package org.jeecg.common.sms;

/** Sanitized provider/configuration failure that never carries a phone number or verification code. */
public class SmsVerificationException extends RuntimeException {

    private final String providerCode;
    private final String requestId;

    public SmsVerificationException(String message, String providerCode, String requestId) {
        super(message);
        this.providerCode = providerCode;
        this.requestId = requestId;
    }

    public SmsVerificationException(String message, String providerCode, String requestId, Throwable cause) {
        super(message, cause);
        this.providerCode = providerCode;
        this.requestId = requestId;
    }

    public SmsVerificationException(String message, Throwable cause) {
        super(message, cause);
        this.providerCode = null;
        this.requestId = null;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getRequestId() {
        return requestId;
    }
}
