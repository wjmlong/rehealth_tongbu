package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceAssignmentServiceTest {
    private static final String SUBJECT_REF = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceAssignmentService service = new InsuranceAssignmentService(jdbc);

    @Test
    void claimRejectsUnknownRoleTypeBeforeTouchingTheDatabase() {
        InsuranceApiException error = org.junit.jupiter.api.Assertions.assertThrows(
                InsuranceApiException.class,
                () -> service.claim(1001, "emp-1", new InsuranceAssignmentRequest.Claim("13800000000", "OWNER"))
        );
        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void claimCreatesPrimaryAssignmentWhenTheEnrollmentHasNoActiveOwner() {
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000"))).thenReturn("user-1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1001), eq("user-1"))).thenReturn(1);
        var enrollment = new InsuranceAssignmentService.EnrollmentRow(
                "enr-1", "proj-1", "默认服务项目", SUBJECT_REF, "user-1", "张三");
        when(jdbc.queryForObject(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.EnrollmentRow>>any(),
                eq(1001), eq("user-1"))).thenReturn(enrollment);
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.ActivePrimary>>any(),
                eq("enr-1"))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("emp-1"))).thenReturn("李四");

        InsuranceAssignmentResponse.Claimed claimed = service.claim(
                1001, "emp-1", new InsuranceAssignmentRequest.Claim("13800000000", null));

        assertTrue(claimed.created());
        assertEquals("PRIMARY", claimed.assignment().roleType());
        assertEquals("李四", claimed.assignment().employeeName());
        assertEquals("张三", claimed.assignment().userName());
        assertEquals("assign", claimed.assignment().changeReason());
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, org.mockito.Mockito.times(2)).update(anyString(), args.capture());
        assertTrue(args.getAllValues().stream().anyMatch(values -> values.length >= 7
                && "emp-1".equals(values[3]) && "PRIMARY".equals(values[4])));
    }

    @Test
    void claimReturnsExistingRelationshipWhenAlreadyOwned() {
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000"))).thenReturn("user-1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1001), eq("user-1"))).thenReturn(1);
        var enrollment = new InsuranceAssignmentService.EnrollmentRow(
                "enr-1", "proj-1", "默认服务项目", SUBJECT_REF, "user-1", "张三");
        when(jdbc.queryForObject(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.EnrollmentRow>>any(),
                eq(1001), eq("user-1"))).thenReturn(enrollment);
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.ActivePrimary>>any(),
                eq("enr-1"))).thenReturn(List.of(
                new InsuranceAssignmentService.ActivePrimary("asg-1", "emp-1")));
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentResponse.Assignment>>any(),
                eq("asg-1"))).thenReturn(List.of(new InsuranceAssignmentResponse.Assignment(
                "asg-1", 1001, "enr-1", "proj-1", "默认服务项目", SUBJECT_REF, "user-1", "张三",
                "emp-1", "李四", "PRIMARY", "2026-08-25T08:00:00", null, "active", "system", "assign")));

        InsuranceAssignmentResponse.Claimed claimed = service.claim(
                1001, "emp-1", new InsuranceAssignmentRequest.Claim("13800000000", null));

        assertFalse(claimed.created());
        assertEquals("asg-1", claimed.assignment().id());
    }

    @Test
    void claimConflictsWhenAnotherEmployeeIsTheActivePrimary() {
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000"))).thenReturn("user-1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1001), eq("user-1"))).thenReturn(1);
        var enrollment = new InsuranceAssignmentService.EnrollmentRow(
                "enr-1", "proj-1", "默认服务项目", SUBJECT_REF, "user-1", "张三");
        when(jdbc.queryForObject(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.EnrollmentRow>>any(),
                eq(1001), eq("user-1"))).thenReturn(enrollment);
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.ActivePrimary>>any(),
                eq("enr-1"))).thenReturn(List.of(
                new InsuranceAssignmentService.ActivePrimary("asg-1", "emp-other")));

        InsuranceApiException error = org.junit.jupiter.api.Assertions.assertThrows(
                InsuranceApiException.class,
                () -> service.claim(1001, "emp-1", new InsuranceAssignmentRequest.Claim("13800000000", null))
        );
        assertEquals(HttpStatus.CONFLICT, error.status());
    }

    @Test
    void transferEndsTheOldRowAndCreatesTheNewOne() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1001), eq("emp-b"))).thenReturn(1);
        var enrollment = new InsuranceAssignmentService.EnrollmentRow(
                "enr-1", "proj-1", "默认服务项目", SUBJECT_REF, "user-1", "张三");
        when(jdbc.queryForObject(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.EnrollmentRow>>any(),
                eq(1001), eq("enr-1"))).thenReturn(enrollment);
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.ActivePrimary>>any(),
                eq("enr-1"))).thenReturn(List.of(
                new InsuranceAssignmentService.ActivePrimary("asg-1", "emp-a")));
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.AssignmentRow>>any(),
                eq("asg-1"), eq(1001))).thenReturn(List.of(
                new InsuranceAssignmentService.AssignmentRow(
                        "asg-1", "enr-1", "emp-a", "PRIMARY", LocalDateTime.now(), "assign")));
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("emp-b"))).thenReturn("王五");

        InsuranceAssignmentResponse.TransferResult result = service.transfer(
                1001, "mgr-1", new InsuranceAssignmentRequest.Transfer(
                        List.of("enr-1"), "emp-a", "emp-b", null, "轮岗"));

        assertEquals(1, result.transferred());
        assertTrue(result.errors().isEmpty());
        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("status = 'ended'"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void endRejectsSelfScopeForAnotherEmployeesAssignment() {
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentService.AssignmentRow>>any(),
                eq("asg-1"), eq(1001))).thenReturn(List.of(
                new InsuranceAssignmentService.AssignmentRow(
                        "asg-1", "enr-1", "emp-other", "PRIMARY", LocalDateTime.now(), "assign")));

        InsuranceApiException error = org.junit.jupiter.api.Assertions.assertThrows(
                InsuranceApiException.class,
                () -> service.end(1001, "emp-1", "asg-1",
                        new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF), "不再服务")
        );
        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void departmentRejectsSelfScope() {
        InsuranceApiException error = org.junit.jupiter.api.Assertions.assertThrows(
                InsuranceApiException.class,
                () -> service.department(1001,
                        new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF), 1, 20)
        );
        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void teamScopePageSqlIncludesTheSharedDepartmentSubquery() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentResponse.Assignment>>any(),
                any(Object[].class))).thenReturn(List.of());

        service.department(1001,
                new InsuranceAssignmentScope("mgr-1", InsuranceAssignmentScope.MODE_TEAM), 1, 20);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(),
                ArgumentMatchers.<RowMapper<InsuranceAssignmentResponse.Assignment>>any(),
                any(Object[].class));
        assertTrue(sql.getValue().contains("assignee_dept.dep_id = my_dept.dep_id"));
        assertTrue(sql.getValue().contains("a.status = 'active'"));
    }
}
