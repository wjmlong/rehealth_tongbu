package org.jeecg.common.system;

import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.util.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WebClientSessionContractTest {

    @Test
    void websiteClientUsesAnIndependentSingleLoginSlot() {
        assertNotEquals(
                CommonConstant.PREFIX_USER_TOKEN_PC,
                CommonConstant.PREFIX_USER_TOKEN_WEB
        );
        assertNotEquals(
                CommonConstant.PREFIX_USER_TOKEN_APP,
                CommonConstant.PREFIX_USER_TOKEN_WEB
        );
    }

    @Test
    void websiteClientTypeSurvivesJwtRoundTrip() {
        String token = JwtUtil.sign("website-user", "test-secret", CommonConstant.CLIENT_TYPE_WEB);

        assertEquals(CommonConstant.CLIENT_TYPE_WEB, JwtUtil.getClientType(token));
    }
}
