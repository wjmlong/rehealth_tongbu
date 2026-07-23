package org.jeecg.modules.rehealth.auth;

@FunctionalInterface
public interface UserTokenIdentityProvider {
    ResolvedUserIdentity resolve(String accessToken);
}
