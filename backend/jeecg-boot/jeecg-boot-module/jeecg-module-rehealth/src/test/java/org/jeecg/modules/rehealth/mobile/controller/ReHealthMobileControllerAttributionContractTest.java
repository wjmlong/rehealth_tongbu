package org.jeecg.modules.rehealth.mobile.controller;

import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReHealthMobileControllerAttributionContractTest {
    @Test
    void exposesExactAuthenticatedAttributionRoute() throws NoSuchMethodException {
        Method method = ReHealthMobileController.class.getMethod(
                "attributionEvents",
                AttributionEventsRequestDto.class
        );

        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertEquals("/attribution/events", mapping.value()[0]);
        assertFalse(method.isAnnotationPresent(IgnoreAuth.class));
    }
}
