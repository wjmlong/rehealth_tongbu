package org.jeecg.modules.rehealth.mobile.controller;

import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HealthAgentControllerContractTest {
    @Test
    void exposesAuthenticatedMobileAgentMessageRoute() throws Exception {
        RequestMapping base = HealthAgentController.class.getAnnotation(RequestMapping.class);
        Method message = HealthAgentController.class.getMethod(
                "message",
                String.class,
                HealthAgentMessageRequestDto.class
        );

        assertArrayEquals(new String[]{"/rehealth/mobile/agent"}, base.value());
        assertArrayEquals(new String[]{"/messages"}, message.getAnnotation(PostMapping.class).value());
        assertFalse(HealthAgentController.class.isAnnotationPresent(IgnoreAuth.class));
        assertFalse(message.isAnnotationPresent(IgnoreAuth.class));
    }
}
