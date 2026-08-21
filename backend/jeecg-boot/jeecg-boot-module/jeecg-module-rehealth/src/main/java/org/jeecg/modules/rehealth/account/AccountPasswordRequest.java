package org.jeecg.modules.rehealth.account;

public final class AccountPasswordRequest {
    private AccountPasswordRequest() {
    }

    public record Change(
            String oldPassword,
            String newPassword,
            String confirmPassword
    ) {
    }
}
