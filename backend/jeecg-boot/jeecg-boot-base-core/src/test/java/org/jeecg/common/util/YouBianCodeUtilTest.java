package org.jeecg.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YouBianCodeUtilTest {

    @Test
    void normalizedTenantRootCanGenerateRootAndChildCodes() {
        assertEquals("A02", YouBianCodeUtil.getNextYouBianCode("A01"));
        assertEquals("A01A03", YouBianCodeUtil.getSubYouBianCode("A01", "A01A02"));
        assertEquals("A01A01", YouBianCodeUtil.getSubYouBianCode("A01", null));
    }
}
