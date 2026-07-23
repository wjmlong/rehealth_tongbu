package org.jeecg.modules.rehealth.auth;

import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.exception.JeecgBoot401Exception;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JeecgUserTokenIdentityProvider implements UserTokenIdentityProvider {
    private static final String DEFAULT_TENANT_ID = "0";

    private final CommonAPI commonApi;
    private final RedisUtil redisUtil;

    public JeecgUserTokenIdentityProvider(CommonAPI commonApi, RedisUtil redisUtil) {
        this.commonApi = commonApi;
        this.redisUtil = redisUtil;
    }

    @Override
    public ResolvedUserIdentity resolve(String accessToken) {
        try {
            TokenUtils.verifyToken(accessToken, commonApi, redisUtil);
            String username = JwtUtil.getUsername(accessToken);
            LoginUser user = TokenUtils.getLoginUser(username, commonApi, redisUtil);
            if (user == null || user.getId() == null || user.getId().isBlank() || user.getStatus() != 1) {
                throw new IdentityResolutionException(IdentityResolutionException.Kind.TOKEN_REJECTED);
            }
            return new ResolvedUserIdentity(user.getId(), tenantIds(user.getRelTenantIds()));
        } catch (JeecgBoot401Exception | IllegalArgumentException exception) {
            throw new IdentityResolutionException(IdentityResolutionException.Kind.TOKEN_REJECTED);
        } catch (IdentityResolutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IdentityResolutionException(IdentityResolutionException.Kind.PROVIDER_UNAVAILABLE);
        }
    }

    private Set<String> tenantIds(String storedTenantIds) {
        if (storedTenantIds == null || storedTenantIds.isBlank()) {
            return Set.of(DEFAULT_TENANT_ID);
        }
        return Arrays.stream(storedTenantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
