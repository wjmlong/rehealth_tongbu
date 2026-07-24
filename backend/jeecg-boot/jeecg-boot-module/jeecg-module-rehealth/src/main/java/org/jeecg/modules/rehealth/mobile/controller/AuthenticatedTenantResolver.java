package org.jeecg.modules.rehealth.mobile.controller;

import org.apache.shiro.authz.UnauthorizedException;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthenticatedTenantResolver {
    public String resolve(LoginUser user, String requestedTenant) {
        if (requestedTenant == null || requestedTenant.isBlank() || "0".equals(requestedTenant)) {
            return "0";
        }
        boolean member = user.getRelTenantIds() != null && Arrays.stream(user.getRelTenantIds().split(","))
                .map(String::trim)
                .anyMatch(requestedTenant::equals);
        if (!member) {
            throw new UnauthorizedException("requested tenant is not assigned to the authenticated user");
        }
        return requestedTenant;
    }
}
