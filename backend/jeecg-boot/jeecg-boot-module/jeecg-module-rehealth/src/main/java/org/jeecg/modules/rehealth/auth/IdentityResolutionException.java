package org.jeecg.modules.rehealth.auth;

public class IdentityResolutionException extends RuntimeException {
    public enum Kind {
        TOKEN_REJECTED,
        PROVIDER_UNAVAILABLE
    }

    private final Kind kind;

    public IdentityResolutionException(Kind kind) {
        super(kind.name());
        this.kind = kind;
    }

    Kind kind() {
        return kind;
    }
}
