package org.jeecg.modules.rehealth.insurance;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceInterventionWorkbenchControllerContractTest {
    @Test
    void readsReuseRiskPermissionAndWritesRequireManagePermission() throws Exception {
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "dashboard", String.class);
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "subjects", String.class, int.class, int.class, String.class, String.class);
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "subject", String.class, String.class);
        //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】人群报告端点沿用保险风险只读权限------------
        assertPermission("rehealth:insurance:risk:view", GetMapping.class,
                "interventionReportData", String.class, int.class);
        //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】人群报告端点沿用保险风险只读权限------------
        assertPermission("rehealth:insurance:intervention:manage", PostMapping.class,
                "createAction", String.class, String.class, InsuranceInterventionWorkbenchRequest.CreateAction.class);
        assertPermission("rehealth:insurance:intervention:manage", PostMapping.class,
                "createActions", String.class, InsuranceInterventionWorkbenchRequest.BatchCreateAction.class);
        assertPermission("rehealth:insurance:intervention:manage", PutMapping.class,
                "updateAction", String.class, String.class, InsuranceInterventionWorkbenchRequest.UpdateAction.class);
    }

    @Test
    void subjectSummaryExposesExistingPatientProfileDemographics() {
        Set<String> components = Arrays.stream(InsuranceInterventionWorkbenchResponse.SubjectSummary.class
                        .getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertTrue(components.containsAll(Set.of("age", "gender", "bmi")));
    }

    private void assertPermission(String expected, Class<?> mapping, String methodName, Class<?>... args) throws Exception {
        Method method = InsuranceInterventionWorkbenchController.class.getMethod(methodName, args);
        assertNotNull(method.getAnnotation((Class) mapping));
        assertEquals(expected, method.getAnnotation(RequiresPermissions.class).value()[0]);
    }
}
