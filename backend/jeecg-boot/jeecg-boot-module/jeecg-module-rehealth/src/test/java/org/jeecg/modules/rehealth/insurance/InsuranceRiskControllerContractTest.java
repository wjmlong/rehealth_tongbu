package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InsuranceRiskControllerContractTest {
    @AfterEach
    void clearSubject() {
        ThreadContext.unbindSubject();
    }

    @Test
    void serviceUnavailableUsesTheRealHttpStatus() {
        InsuranceRiskService service = mock(InsuranceRiskService.class);
        InsuranceTenantAccessGuard guard = mock(InsuranceTenantAccessGuard.class);
        InsuranceRiskController controller = new InsuranceRiskController(service, guard);
        bindPrincipal();
        when(guard.requireTenant(any(LoginUser.class), eq("1000"))).thenReturn(1000);
        when(service.dashboard(1000, null)).thenThrow(InsuranceApiException.serviceUnavailable("not connected"));

        ResponseEntity<Result<InsuranceRiskResponse.Dashboard>> response = controller.dashboard("1000");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(503, response.getBody().getCode());
        assertEquals("not connected", response.getBody().getMessage());
    }

    @Test
    void everyEndpointRequiresTheDedicatedInsurancePermission() throws Exception {
        assertEndpoint("dashboard", String.class);
        assertEndpoint("insureds", String.class, int.class, int.class, String.class, String.class,
                String.class, Integer.class, Integer.class);
        assertEndpoint("filterOptions", String.class);
        assertEndpoint("insured", String.class, String.class);
        assertDisabledEndpoint("dashboard", String.class);
        assertDisabledEndpoint("insureds", String.class, int.class, int.class, String.class, String.class,
                String.class, Integer.class, Integer.class);
        assertDisabledEndpoint("filterOptions", String.class);
        assertDisabledEndpoint("insured", String.class, String.class);
    }

    @Test
    void preControllerPermissionFailureUsesRealHttp403() throws Exception {
        InsuranceRiskController target = new InsuranceRiskController(
                mock(InsuranceRiskService.class),
                mock(InsuranceTenantAccessGuard.class)
        );
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            RequiresPermissions required = AnnotatedElementUtils.findMergedAnnotation(
                    invocation.getMethod(),
                    RequiresPermissions.class
            );
            if (required != null) {
                throw new UnauthorizedException("missing permission");
            }
            return invocation.proceed();
        });
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxyFactory.getProxy())
                .setControllerAdvice(new InsuranceAuthorizationExceptionHandler())
                .build();

        mockMvc.perform(get("/rehealth/insurance/v1/dashboard/risk")
                        .header("X-Tenant-Id", "1000"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("insurance permission is required"));
    }

    @Test
    void jsonContractKeepsSnakeCaseAndExplicitNulls() throws Exception {
        InsuranceRiskResponse.Risk risk = new InsuranceRiskResponse.Risk(
                "synthetic", null, null, "mock-v1", "2026-08-11T06:00:00Z", null
        );
        String json = new ObjectMapper().writeValueAsString(risk);

        assertTrue(json.contains("\"model_version\":\"mock-v1\""));
        assertTrue(json.contains("\"positive_factors\":null"));
        assertTrue(json.contains("\"score\":null"));
        assertFalse(json.contains("modelVersion"));
    }

    @Test
    void likeMetacharactersAreEscapedAsLiterals() {
        assertEquals("50!%!_!!", JdbcInsuranceRiskRepository.escapeLike("50%_!"));
    }

    private void assertEndpoint(String name, Class<?>... parameterTypes) throws Exception {
        assertPermission(InsuranceRiskController.class.getMethod(name, parameterTypes));
    }

    private void assertDisabledEndpoint(String name, Class<?>... parameterTypes) throws Exception {
        assertPermission(DisabledInsuranceRiskController.class.getMethod(name, parameterTypes));
    }

    private void assertPermission(Method method) {
        assertNotNull(method.getAnnotation(GetMapping.class));
        RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(permission);
        assertEquals("rehealth:insurance:risk:view", permission.value()[0]);
    }

    private void bindPrincipal() {
        Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(new LoginUser().setId("service-user").setRelTenantIds("1000"));
        ThreadContext.bind(subject);
    }
}
