package org.jeecg.modules.rehealth.insurance;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.rehealth.careplan.CarePlanVersionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InsuranceCarePlanControllerContractTest {
    @Test
    void viewingEditingAndPublishingUseSeparatedPermissions() throws Exception {
        assertPermission("rehealth:insurance:care-plan:view", GetMapping.class,
                "list", String.class, String.class);
        assertPermission("rehealth:insurance:care-plan:view", GetMapping.class,
                "detail", String.class, String.class);
        assertPermission("rehealth:insurance:care-plan:manage", PostMapping.class,
                "createDraft", String.class, String.class, CarePlanVersionRequest.CreateDraft.class);
        assertPermission("rehealth:insurance:care-plan:manage", PutMapping.class,
                "updateDraft", String.class, String.class, CarePlanVersionRequest.UpdateDraft.class);
        assertPermission("rehealth:insurance:care-plan:manage", PostMapping.class,
                "createRevision", String.class, String.class, CarePlanVersionRequest.CreateRevision.class);
        assertPermission("rehealth:insurance:care-plan:manage", PostMapping.class,
                "discardDraft", String.class, String.class, CarePlanVersionRequest.DiscardDraft.class);
        assertPermission("rehealth:insurance:care-plan:publish", PostMapping.class,
                "publish", String.class, String.class, CarePlanVersionRequest.Publish.class);
        assertPermission("rehealth:insurance:care-plan:publish", PostMapping.class,
                "withdraw", String.class, String.class, CarePlanVersionRequest.Withdraw.class);
    }

    private void assertPermission(String expected, Class<?> mapping, String methodName, Class<?>... args) throws Exception {
        Method method = InsuranceCarePlanController.class.getMethod(methodName, args);
        assertNotNull(method.getAnnotation((Class) mapping));
        assertEquals(expected, method.getAnnotation(RequiresPermissions.class).value()[0]);
    }
}
