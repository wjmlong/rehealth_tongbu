package org.jeecg.modules.rehealth.mobile.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReHealthMobileControllerTelemetryContractTest {
    @Test
    void exposesAuthenticatedRecentTelemetryRouteWithBoundedLimit() throws Exception {
        Method method = ReHealthMobileController.class.getMethod("recentTelemetry", int.class);

        assertArrayEquals(
                new String[]{"/measurements/recent"},
                method.getAnnotation(GetMapping.class).value()
        );
        RequestParam limit = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("limit", limit.value());
        assertEquals("50", limit.defaultValue());
    }
}
