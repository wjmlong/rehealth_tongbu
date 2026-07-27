package org.jeecg.modules.rehealth.mobile.controller;

import org.apache.shiro.authz.UnauthorizedException;
import org.jeecg.common.system.vo.LoginUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedTenantResolverTest {
    private final AuthenticatedTenantResolver resolver = new AuthenticatedTenantResolver();

    @Test
    void acceptsTenantAssignedToAuthenticatedUser() {
        LoginUser user = new LoginUser();
        user.setRelTenantIds("tenant-a, tenant-b");

        assertEquals("tenant-b", resolver.resolve(user, "tenant-b"));
    }

    @Test
    void rejectsCrossTenantRequest() {
        LoginUser user = new LoginUser();
        user.setRelTenantIds("tenant-a");

        assertThrows(UnauthorizedException.class, () -> resolver.resolve(user, "tenant-b"));
    }
}
