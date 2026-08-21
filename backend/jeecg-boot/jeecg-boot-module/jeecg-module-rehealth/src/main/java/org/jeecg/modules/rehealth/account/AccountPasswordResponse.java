package org.jeecg.modules.rehealth.account;

public final class AccountPasswordResponse {
    private AccountPasswordResponse() {
    }

    public record Status(boolean mustChangePassword) {
    }

    public record Change(boolean mustChangePassword, String message) {
    }

    public record Reset(boolean mustChangePassword, String message) {
    }
}
