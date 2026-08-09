package org.jeecg.modules.rehealth.controller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RehealthUserHealthAdminControllerContractTest {
    @Test
    void everyAdminReadRouteRequiresPatientViewPermission() {
        for (String methodName : Set.of("list", "detail", "disabledLegacyUsers")) {
            Method method = Arrays.stream(RehealthUserHealthAdminController.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
            assertEquals("rehealth:admin:patient:view", permission.value()[0]);
        }
    }

    @Test
    void patientResponseTypeHasNoDirectPhoneEmailOrUsernameField() {
        Set<String> fields = Arrays.stream(RehealthUserHealthVO.class.getDeclaredFields())
                .map(field -> field.getName().toLowerCase())
                .collect(Collectors.toSet());
        assertFalse(fields.contains("phone"));
        assertFalse(fields.contains("email"));
        assertFalse(fields.contains("username"));
    }
}
