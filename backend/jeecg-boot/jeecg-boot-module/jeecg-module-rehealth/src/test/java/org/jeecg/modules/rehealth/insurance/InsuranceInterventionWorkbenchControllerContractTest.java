package org.jeecg.modules.rehealth.insurance;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InsuranceInterventionWorkbenchControllerContractTest {
    @Test
    void readsReuseRiskPermissionAndWritesRequireManagePermission() throws Exception {
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "dashboard", String.class);
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "subjects", String.class, int.class, int.class, String.class, String.class);
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "subject", String.class, String.class);
        assertPermission("rehealth:insurance:intervention:manage", PostMapping.class,
                "createAction", String.class, String.class, InsuranceInterventionWorkbenchRequest.CreateAction.class);
        assertPermission("rehealth:insurance:intervention:manage", PutMapping.class,
                "updateAction", String.class, String.class, InsuranceInterventionWorkbenchRequest.UpdateAction.class);
    }

    private void assertPermission(String expected, Class<?> mapping, String methodName, Class<?>... args) throws Exception {
        Method method = InsuranceInterventionWorkbenchController.class.getMethod(methodName, args);
        assertNotNull(method.getAnnotation((Class) mapping));
        assertEquals(expected, method.getAnnotation(RequiresPermissions.class).value()[0]);
    }
}
