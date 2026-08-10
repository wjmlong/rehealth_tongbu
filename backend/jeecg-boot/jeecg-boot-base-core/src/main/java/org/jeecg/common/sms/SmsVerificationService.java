package org.jeecg.common.sms;

/** Server-side registration SMS verification boundary. */
public interface SmsVerificationService {

    SendReceipt sendRegistrationCode(String phoneNumber, String outId);

    boolean checkRegistrationCode(String phoneNumber, String verifyCode, String outId);

    record SendReceipt(
            String outId,
            String requestId,
            String bizId,
            int validSeconds,
            int intervalSeconds
    ) {
    }
}
