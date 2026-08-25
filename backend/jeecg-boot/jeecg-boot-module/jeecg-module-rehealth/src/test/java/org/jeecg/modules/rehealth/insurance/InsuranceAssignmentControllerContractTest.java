package org.jeecg.modules.rehealth.insurance;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsuranceAssignmentControllerContractTest {
    @AfterEach
    void clearSubject() {
        ThreadContext.unbindSubject();
    }

    @Test
    void controllerIsMountedUnderTheInsuranceV1AssignmentPath() {
        RequestMapping mapping = InsuranceAssignmentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/rehealth/insurance/v1/assignments", mapping.value()[0]);
    }

    @Test
    void everyEndpointRequiresItsDedicatedPermission() throws Exception {
        assertPermission("claim", "rehealth:insurance:assignment:manage", PostMapping.class,
                String.class, InsuranceAssignmentRequest.Claim.class);
        assertPermission("inviteCodes", "rehealth:insurance:assignment:manage", PostMapping.class,
                String.class, InsuranceAssignmentRequest.InviteCode.class);
        assertPermission("transfer", "rehealth:insurance:assignment:transfer", PostMapping.class,
                String.class, InsuranceAssignmentRequest.Transfer.class);
        assertPermission("end", "rehealth:insurance:assignment:manage", PostMapping.class,
                String.class, String.class, InsuranceAssignmentRequest.End.class);
        assertPermission("mine", "rehealth:insurance:assignment:view", GetMapping.class,
                String.class, int.class, int.class);
        assertPermission("department", "rehealth:insurance:assignment:view", GetMapping.class,
                String.class, int.class, int.class);
        assertPermission("history", "rehealth:insurance:assignment:view", GetMapping.class,
                String.class, String.class);
    }

    @Test
    void claimDelegatesToTheServiceWithTheResolvedTenant() {
        InsuranceAssignmentService service = mock(InsuranceAssignmentService.class);
        InsuranceTenantAccessGuard guard = mock(InsuranceTenantAccessGuard.class);
        InsuranceAssignmentController controller = new InsuranceAssignmentController(service, guard);
        bindPrincipal("service-user");
        when(guard.requireTenant(any(LoginUser.class), eq("1000"))).thenReturn(1000);
        when(service.claim(eq(1000), eq("service-user"), any(InsuranceAssignmentRequest.Claim.class)))
                .thenReturn(new InsuranceAssignmentResponse.Claimed(
                        new InsuranceAssignmentResponse.Assignment(
                                "asg-1", 1000, "enr-1", "proj-1", "默认服务项目",
                                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                                "user-1", "张三", "service-user", "李四", "PRIMARY",
                                "2026-08-25T08:00:00", null, "active", "system", "assign"),
                        true));

        ResponseEntity<Result<InsuranceAssignmentResponse.Claimed>> response = controller.claim(
                "1000", new InsuranceAssignmentRequest.Claim("13800000000", null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().getResult().created());
        assertEquals("李四", response.getBody().getResult().assignment().employeeName());
    }

    @Test
    void reservedInviteCodeEndpointReturnsReal501() {
        InsuranceAssignmentService service = mock(InsuranceAssignmentService.class);
        InsuranceTenantAccessGuard guard = mock(InsuranceTenantAccessGuard.class);
        InsuranceAssignmentController controller = new InsuranceAssignmentController(service, guard);
        bindPrincipal("service-user");
        when(guard.requireTenant(any(LoginUser.class), eq("1000"))).thenReturn(1000);

        ResponseEntity<Result<?>> response = controller.inviteCodes(
                "1000", new InsuranceAssignmentRequest.InviteCode("13800000000", 4320));

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(501, response.getBody().getCode());
    }

    @Test
    void mobileReservedEndpointsReturnReal501() {
        InsuranceAssignmentService service = mock(InsuranceAssignmentService.class);
        InsuranceMobileAssignmentController controller = new InsuranceMobileAssignmentController(service);
        bindPrincipal("app-user-1");

        ResponseEntity<Result<?>> redeem = controller.redeem(
                new InsuranceAssignmentRequest.InviteCode("13800000000", 4320));
        ResponseEntity<Result<?>> scan = controller.scan(
                new InsuranceAssignmentRequest.Scan("EMP-CODE", "1000"));

        assertEquals(HttpStatus.NOT_IMPLEMENTED, redeem.getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, scan.getStatusCode());
    }

    private void assertPermission(
            String methodName, String expectedPermission, Class<? extends java.lang.annotation.Annotation> mappingType,
            Class<?>... parameterTypes
    ) throws Exception {
        Method method = InsuranceAssignmentController.class.getMethod(methodName, parameterTypes);
        assertNotNull(method.getAnnotation(mappingType), methodName + " mapping");
        RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(permission, methodName + " permission");
        assertEquals(expectedPermission, permission.value()[0]);
    }

    private void bindPrincipal(String userId) {
        Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(new LoginUser().setId(userId).setRelTenantIds("1000"));
        ThreadContext.bind(subject);
    }
}
