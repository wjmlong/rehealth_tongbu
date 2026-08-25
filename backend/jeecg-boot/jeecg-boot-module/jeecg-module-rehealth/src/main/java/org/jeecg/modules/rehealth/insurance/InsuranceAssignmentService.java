package org.jeecg.modules.rehealth.insurance;

import org.jeecg.modules.rehealth.insurance.entity.InsuranceAuditEventEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceAuditEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Interval-based user service assignment (insurance employee ↔ enrolled user).
 *
 * <p>Every change ends the previous relationship row and creates a new one, so
 * the responsibility chain is preserved; a generated-column unique index keeps
 * at most one active PRIMARY per enrollment even under concurrency.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系服务-----------
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceAssignmentService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 200;
    private static final int MAX_HISTORY_LOGS = 200;
    private static final Set<String> ROLE_TYPES = Set.of("PRIMARY", "BACKUP", "TEMPORARY", "SUPERVISOR");

    private static final String EXCLUDE_PLATFORM_ADMINS = """
            AND NOT EXISTS (
                SELECT 1
                FROM sys_user_role platform_user_role
                INNER JOIN sys_role platform_role ON platform_role.id = platform_user_role.role_id
                WHERE platform_user_role.user_id = u.id
                  AND platform_role.role_code IN ('admin', 'super_admin')
            )
            """;

    private static final String ASSIGNMENT_SELECT = """
            SELECT a.id, a.tenant_id, a.enrollment_id, e.project_id, p.name AS project_name,
                   e.subject_ref, e.rehealth_user_id,
                   COALESCE(profile.name, u.realname, '未命名用户') AS user_name,
                   a.employee_id, emp.realname AS employee_name,
                   a.role_type, a.start_time, a.end_time, a.status, a.start_time_source, a.change_reason
            FROM rehealth_insurance_user_assignment a
            JOIN rehealth_insurance_enrollment e ON e.id = a.enrollment_id
            LEFT JOIN rehealth_insurance_project p ON p.id = e.project_id
            LEFT JOIN sys_user u ON u.id = CONVERT(e.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
            LEFT JOIN rehealth_patient_profile profile ON profile.user_id = e.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
            LEFT JOIN sys_user emp ON emp.id = a.employee_id
            """;

    private final JdbcTemplate jdbc;
    private final InsuranceAuditEventMapper auditMapper;

    @Autowired
    public InsuranceAssignmentService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            InsuranceAuditEventMapper auditMapper
    ) {
        this.jdbc = jdbc;
        this.auditMapper = auditMapper;
    }

    /** Constructor kept for focused service tests that use a mocked JDBC template. */
    public InsuranceAssignmentService(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    @Transactional
    public InsuranceAssignmentResponse.Claimed claim(int tenantId, String operatorId, InsuranceAssignmentRequest.Claim request) {
        String phone = required(request.phone(), "phone", 45);
        String roleType = roleType(request.roleType());
        String userId = userIdByPhone(phone);
        requireActiveTenantMember(tenantId, userId, "该账号不是当前保险租户的活跃成员");
        EnrollmentRow enrollment = activeEnrollment(tenantId, userId);

        ActivePrimary existing = activePrimary(enrollment.id());
        if (existing != null) {
            if (operatorId.equals(existing.employeeId())) {
                return new InsuranceAssignmentResponse.Claimed(assignment(existing.assignmentId()), false);
            }
            throw InsuranceApiException.conflict("该用户已有主负责人，请通过转移流程变更");
        }

        InsuranceAssignmentResponse.Assignment created = createAssignment(
                tenantId, operatorId, enrollment, operatorId, roleType, "assign", null);
        return new InsuranceAssignmentResponse.Claimed(created, true);
    }

    @Transactional
    public InsuranceAssignmentResponse.TransferResult transfer(
            int tenantId, String operatorId, InsuranceAssignmentRequest.Transfer request
    ) {
        List<String> enrollmentIds = request.enrollmentIds();
        if (enrollmentIds == null || enrollmentIds.isEmpty()) {
            throw InsuranceApiException.badRequest("enrollmentIds must contain at least one enrollment");
        }
        if (enrollmentIds.size() > MAX_BATCH_SIZE) {
            throw InsuranceApiException.badRequest("enrollmentIds must not exceed " + MAX_BATCH_SIZE + " records");
        }
        String toEmployeeId = required(request.toEmployeeId(), "toEmployeeId", 64);
        String roleType = roleType(request.roleType());
        String reason = required(request.reason(), "reason", 64);
        String fromEmployeeId = request.fromEmployeeId() == null || request.fromEmployeeId().isBlank()
                ? null : required(request.fromEmployeeId(), "fromEmployeeId", 64);
        requireActiveTenantMember(tenantId, toEmployeeId,
                "toEmployeeId 不是当前保险租户的活跃成员");

        int transferred = 0;
        List<String> errors = new ArrayList<>();
        for (String enrollmentId : enrollmentIds) {
            try {
                EnrollmentRow enrollment = requireEnrollment(tenantId, required(enrollmentId, "enrollmentId", 64));
                ActivePrimary existing = activePrimary(enrollment.id());
                if (existing == null) {
                    errors.add(enrollmentId + ": 没有活跃的主负责人，无需转移");
                    continue;
                }
                if (fromEmployeeId != null && !fromEmployeeId.equals(existing.employeeId())) {
                    errors.add(enrollmentId + ": 当前主负责人与 fromEmployeeId 不一致");
                    continue;
                }
                if (existing.employeeId().equals(toEmployeeId)) {
                    errors.add(enrollmentId + ": 目标员工已是当前主负责人");
                    continue;
                }
                endAssignmentRow(tenantId, operatorId, existing.assignmentId(), enrollment,
                        "transfer", reason, existing.employeeId());
                createAssignment(tenantId, operatorId, enrollment, toEmployeeId, roleType, "transfer", reason);
                transferred++;
            } catch (InsuranceApiException e) {
                errors.add(enrollmentId + ": " + e.getMessage());
            }
        }
        return new InsuranceAssignmentResponse.TransferResult(enrollmentIds.size(), transferred, errors);
    }

    @Transactional
    public InsuranceAssignmentResponse.EndResult end(
            int tenantId, String operatorId, String assignmentId,
            InsuranceAssignmentScope scope, String reason
    ) {
        String normalizedReason = required(reason, "reason", 64);
        AssignmentRow row = activeAssignment(tenantId, required(assignmentId, "assignmentId", 64));
        if (!withinScope(tenantId, row.employeeId(), scope)) {
            throw InsuranceApiException.forbidden("当前账号无权结束该服务关系");
        }
        EnrollmentRow enrollment = requireEnrollment(tenantId, row.enrollmentId());
        endAssignmentRow(tenantId, operatorId, row.assignmentId(), enrollment, "end", normalizedReason, row.employeeId());
        return new InsuranceAssignmentResponse.EndResult(
                assignment(row.assignmentId()), "服务关系已结束，历史保留");
    }

    public InsuranceAssignmentResponse.Page mine(int tenantId, String operatorId, int pageNo, int pageSize) {
        return page(tenantId, new InsuranceAssignmentScope(operatorId, InsuranceAssignmentScope.MODE_SELF),
                pageNo, pageSize, true);
    }

    public InsuranceAssignmentResponse.Page department(int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize) {
        if (scope != null && !scope.team()) {
            throw InsuranceApiException.forbidden("部门视图需要主管或管理员权限");
        }
        return page(tenantId, scope, pageNo, pageSize, true);
    }

    public InsuranceAssignmentResponse.History history(
            int tenantId, String enrollmentId, InsuranceAssignmentScope scope
    ) {
        String normalizedEnrollmentId = required(enrollmentId, "enrollmentId", 64);
        requireEnrollment(tenantId, normalizedEnrollmentId);
        if (scope != null && !anyAssignmentWithinScope(tenantId, normalizedEnrollmentId, scope)) {
            throw InsuranceApiException.forbidden("当前账号无权查看该用户的服务关系历史");
        }
        List<InsuranceAssignmentResponse.Assignment> assignments = jdbc.query(
                ASSIGNMENT_SELECT + " WHERE a.tenant_id = ? AND a.enrollment_id = ? ORDER BY a.start_time DESC, a.id DESC",
                (rs, rowNum) -> mapAssignment(rs),
                tenantId, normalizedEnrollmentId);
        List<InsuranceAssignmentResponse.ChangeLog> logs = jdbc.query("""
                SELECT id, enrollment_id, assignment_id, change_type, before_json, after_json,
                       reason, operator_id, created_at
                FROM rehealth_insurance_assignment_change_log
                WHERE tenant_id = ? AND enrollment_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """, (rs, rowNum) -> new InsuranceAssignmentResponse.ChangeLog(
                        rs.getString("id"),
                        rs.getString("enrollment_id"),
                        rs.getString("assignment_id"),
                        rs.getString("change_type"),
                        rs.getString("before_json"),
                        rs.getString("after_json"),
                        rs.getString("reason"),
                        rs.getString("operator_id"),
                        ts(rs.getTimestamp("created_at"))
                ), tenantId, normalizedEnrollmentId, MAX_HISTORY_LOGS);
        return new InsuranceAssignmentResponse.History(assignments, logs);
    }

    public InsuranceAssignmentResponse.MobileContact mobileContact(String userId) {
        try {
            return jdbc.queryForObject("""
                    SELECT e.tenant_id, p.name AS project_name, emp.realname AS employee_name,
                           a.role_type, a.start_time
                    FROM rehealth_insurance_user_assignment a
                    JOIN rehealth_insurance_enrollment e ON e.id = a.enrollment_id
                    LEFT JOIN rehealth_insurance_project p ON p.id = e.project_id
                    LEFT JOIN sys_user emp ON emp.id = a.employee_id
                    WHERE e.rehealth_user_id = ? AND a.status = 'active'
                    ORDER BY CASE WHEN a.role_type = 'PRIMARY' THEN 0 ELSE 1 END, a.start_time DESC, a.id DESC
                    LIMIT 1
                    """, (rs, rowNum) -> new InsuranceAssignmentResponse.MobileContact(
                            rs.getInt("tenant_id"),
                            rs.getString("project_name"),
                            rs.getString("employee_name"),
                            rs.getString("role_type"),
                            ts(rs.getTimestamp("start_time"))
                    ), userId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private InsuranceAssignmentResponse.Page page(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, boolean activeOnly
    ) {
        int normalizedPageNo = Math.max(1, pageNo);
        int normalizedPageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        StringBuilder sql = new StringBuilder(ASSIGNMENT_SELECT)
                .append(" WHERE a.tenant_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (scope != null) {
            sql.append(" AND (a.employee_id = ?");
            args.add(scope.userId());
            if (scope.team()) {
                sql.append(" OR EXISTS (SELECT 1 FROM sys_user_depart my_dept")
                        .append(" JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id = my_dept.dep_id")
                        .append(" WHERE my_dept.user_id = ? AND assignee_dept.user_id = a.employee_id)");
                args.add(scope.userId());
            }
            sql.append(")");
        }
        if (activeOnly) {
            sql.append(" AND a.status = 'active'");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + sql + ") scoped_assignments",
                Long.class, args.toArray());
        List<InsuranceAssignmentResponse.Assignment> records = jdbc.query(
                sql + " ORDER BY a.start_time DESC, a.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapAssignment(rs),
                concat(args, normalizedPageSize, (normalizedPageNo - 1) * normalizedPageSize));
        return new InsuranceAssignmentResponse.Page(total == null ? 0 : total, records);
    }

    private InsuranceAssignmentResponse.Assignment createAssignment(
            int tenantId, String operatorId, EnrollmentRow enrollment, String employeeId, String roleType,
            String changeReason, String contextReason
    ) {
        String id = uuid();
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbc.update("""
                    INSERT INTO rehealth_insurance_user_assignment
                        (id, tenant_id, enrollment_id, employee_id, role_type, start_time, end_time,
                         status, start_time_source, change_reason, operator_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, 'active', 'system', ?, ?, ?)
                    """, id, tenantId, enrollment.id(), employeeId, roleType, now, changeReason, operatorId, now);
        } catch (DuplicateKeyException e) {
            throw InsuranceApiException.conflict("该参与记录已存在活跃主负责人");
        }
        logChange(tenantId, operatorId, enrollment.id(), id, changeReason,
                null, assignmentJson(id, employeeId, roleType, now, null, "active", changeReason), contextReason, now);
        audit(tenantId, operatorId, "assignment_" + changeReason, id,
                metadata(enrollment.subjectRef(), employeeId, roleType));
        return new InsuranceAssignmentResponse.Assignment(
                id, tenantId, enrollment.id(), enrollment.projectId(), enrollment.projectName(),
                enrollment.subjectRef(), enrollment.userId(), enrollment.userName(),
                employeeId, employeeName(employeeId), roleType, ts(now), null, "active",
                "system", changeReason);
    }

    private void endAssignmentRow(
            int tenantId, String operatorId, String assignmentId, EnrollmentRow enrollment,
            String changeType, String reason, String ownedBy
    ) {
        LocalDateTime now = LocalDateTime.now();
        AssignmentRow before = assignmentRow(tenantId, assignmentId);
        jdbc.update("""
                UPDATE rehealth_insurance_user_assignment
                SET end_time = ?, status = 'ended', change_reason = ?
                WHERE id = ? AND tenant_id = ? AND status = 'active'
                """, now, changeType, assignmentId, tenantId);
        logChange(tenantId, operatorId, enrollment.id(), assignmentId, changeType,
                assignmentJson(assignmentId, ownedBy, before.roleType(), before.startTime(), null, "active", before.changeReason()),
                assignmentJson(assignmentId, ownedBy, before.roleType(), before.startTime(), now, "ended", changeType),
                reason, now);
        audit(tenantId, operatorId, "assignment_" + changeType, assignmentId,
                metadata(enrollment.subjectRef(), ownedBy, before.roleType()));
    }

    private void logChange(
            int tenantId, String operatorId, String enrollmentId, String assignmentId, String changeType,
            String beforeJson, String afterJson, String reason, LocalDateTime now
    ) {
        jdbc.update("""
                INSERT INTO rehealth_insurance_assignment_change_log
                    (id, tenant_id, enrollment_id, assignment_id, change_type,
                     before_json, after_json, reason, operator_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, uuid(), tenantId, enrollmentId, assignmentId, changeType,
                beforeJson, afterJson, reason, operatorId, now);
    }

    private void audit(int tenantId, String operatorId, String action, String resourceId, String metadataJson) {
        if (auditMapper == null) {
            return;
        }
        InsuranceAuditEventEntity event = new InsuranceAuditEventEntity();
        event.setId(uuid());
        event.setTenantId(tenantId);
        event.setActorUserId(operatorId);
        event.setAction(action);
        event.setResourceType("user_assignment");
        event.setResourceId(resourceId);
        event.setMetadataJson(metadataJson);
        event.setCreatedAt(LocalDateTime.now());
        auditMapper.insert(event);
    }

    private boolean withinScope(int tenantId, String employeeId, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return true;
        }
        if (scope.userId().equals(employeeId)) {
            return true;
        }
        if (scope.team()) {
            Integer shared = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM sys_user_depart my_dept
                    JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id = my_dept.dep_id
                    WHERE my_dept.user_id = ? AND assignee_dept.user_id = ?
                    """, Integer.class, scope.userId(), employeeId);
            return shared != null && shared > 0;
        }
        return false;
    }

    private boolean anyAssignmentWithinScope(int tenantId, String enrollmentId, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return true;
        }
        Integer visible = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM rehealth_insurance_user_assignment a
                WHERE a.tenant_id = ? AND a.enrollment_id = ?
                  AND (
                      a.employee_id = ?
                      OR EXISTS (
                          SELECT 1 FROM sys_user_depart my_dept
                          JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id = my_dept.dep_id
                          WHERE my_dept.user_id = ? AND assignee_dept.user_id = a.employee_id
                      )
                  )
                """, Integer.class, tenantId, enrollmentId, scope.userId(), scope.userId());
        return visible != null && visible > 0;
    }

    private void requireActiveTenantMember(int tenantId, String userId, String message) {
        Integer active = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_tenant membership
                JOIN sys_user account ON account.id = CONVERT(membership.user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                JOIN sys_tenant tenant ON tenant.id = membership.tenant_id
                WHERE membership.tenant_id = ? AND membership.user_id = ?
                  AND membership.status = '1' AND account.status = 1 AND account.del_flag = 0
                  AND tenant.status = 1 AND tenant.del_flag = 0
                """, Integer.class, tenantId, userId);
        if (active == null || active < 1) {
            throw InsuranceApiException.notFound(message);
        }
    }

    private String userIdByPhone(String phone) {
        try {
            return jdbc.queryForObject("""
                    SELECT u.id
                    FROM sys_user u
                    WHERE u.phone = ? AND u.del_flag = 0
                    """ + EXCLUDE_PLATFORM_ADMINS + " LIMIT 1",
                    String.class, phone);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("没有注册账号与该手机号匹配");
        }
    }

    private EnrollmentRow activeEnrollment(int tenantId, String userId) {
        try {
            return enrollmentRow("""
                    SELECT e.id, e.project_id, p.name AS project_name, e.subject_ref, e.rehealth_user_id,
                           COALESCE(profile.name, u.realname, '未命名用户') AS user_name
                    FROM rehealth_insurance_enrollment e
                    LEFT JOIN rehealth_insurance_project p ON p.id = e.project_id
                    LEFT JOIN sys_user u ON u.id = CONVERT(e.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    LEFT JOIN rehealth_patient_profile profile ON profile.user_id = e.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                    WHERE e.tenant_id = ? AND e.rehealth_user_id = ? AND e.enrollment_status = 'active'
                    ORDER BY e.created_at DESC, e.id DESC
                    LIMIT 1
                    """, tenantId, userId);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("该用户没有活跃的保险项目参与记录");
        }
    }

    private EnrollmentRow requireEnrollment(int tenantId, String enrollmentId) {
        try {
            return enrollmentRow("""
                    SELECT e.id, e.project_id, p.name AS project_name, e.subject_ref, e.rehealth_user_id,
                           COALESCE(profile.name, u.realname, '未命名用户') AS user_name
                    FROM rehealth_insurance_enrollment e
                    LEFT JOIN rehealth_insurance_project p ON p.id = e.project_id
                    LEFT JOIN sys_user u ON u.id = CONVERT(e.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    LEFT JOIN rehealth_patient_profile profile ON profile.user_id = e.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                    WHERE e.tenant_id = ? AND e.id = ?
                    LIMIT 1
                    """, tenantId, enrollmentId);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("参与记录不存在");
        }
    }

    private EnrollmentRow enrollmentRow(String sql, Object... args) {
        return jdbc.queryForObject(sql, (rs, rowNum) -> new EnrollmentRow(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("subject_ref"),
                rs.getString("rehealth_user_id"),
                rs.getString("user_name")
        ), args);
    }

    /** Package-visible for focused service tests. */
    record EnrollmentRow(
            String id, String projectId, String projectName, String subjectRef, String userId, String userName
    ) {
    }

    /** Package-visible for focused service tests. */
    record ActivePrimary(String assignmentId, String employeeId) {
    }

    /** Package-visible for focused service tests. */
    record AssignmentRow(
            String assignmentId, String enrollmentId, String employeeId,
            String roleType, LocalDateTime startTime, String changeReason
    ) {
    }

    private ActivePrimary activePrimary(String enrollmentId) {
        List<ActivePrimary> rows = jdbc.query("""
                SELECT id, employee_id
                FROM rehealth_insurance_user_assignment
                WHERE enrollment_id = ? AND status = 'active' AND role_type = 'PRIMARY'
                LIMIT 1
                """, (rs, rowNum) -> new ActivePrimary(rs.getString("id"), rs.getString("employee_id")), enrollmentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private AssignmentRow activeAssignment(int tenantId, String assignmentId) {
        return assignmentRow(tenantId, assignmentId, true);
    }

    private AssignmentRow assignmentRow(int tenantId, String assignmentId) {
        return assignmentRow(tenantId, assignmentId, false);
    }

    private AssignmentRow assignmentRow(int tenantId, String assignmentId, boolean activeOnly) {
        String statusClause = activeOnly ? " AND a.status = 'active'" : "";
        List<AssignmentRow> rows = jdbc.query("""
                SELECT a.id, a.enrollment_id, a.employee_id, a.role_type, a.start_time, a.change_reason
                FROM rehealth_insurance_user_assignment a
                WHERE a.id = ? AND a.tenant_id = ?
                """ + statusClause + " LIMIT 1",
                (rs, rowNum) -> new AssignmentRow(
                        rs.getString("id"),
                        rs.getString("enrollment_id"),
                        rs.getString("employee_id"),
                        rs.getString("role_type"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getString("change_reason")
                ), assignmentId, tenantId);
        if (rows.isEmpty()) {
            throw InsuranceApiException.notFound(activeOnly ? "活跃的服务关系不存在" : "服务关系不存在");
        }
        return rows.get(0);
    }

    private InsuranceAssignmentResponse.Assignment assignment(String assignmentId) {
        List<InsuranceAssignmentResponse.Assignment> rows = jdbc.query(
                ASSIGNMENT_SELECT + " WHERE a.id = ?",
                (rs, rowNum) -> mapAssignment(rs), assignmentId);
        if (rows.isEmpty()) {
            throw InsuranceApiException.notFound("服务关系不存在");
        }
        return rows.get(0);
    }

    private InsuranceAssignmentResponse.Assignment mapAssignment(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InsuranceAssignmentResponse.Assignment(
                rs.getString("id"),
                rs.getInt("tenant_id"),
                rs.getString("enrollment_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("subject_ref"),
                rs.getString("rehealth_user_id"),
                rs.getString("user_name"),
                rs.getString("employee_id"),
                rs.getString("employee_name"),
                rs.getString("role_type"),
                ts(rs.getTimestamp("start_time")),
                ts(rs.getTimestamp("end_time")),
                rs.getString("status"),
                rs.getString("start_time_source"),
                rs.getString("change_reason")
        );
    }

    private String employeeName(String employeeId) {
        try {
            return jdbc.queryForObject("SELECT COALESCE(realname, username) FROM sys_user WHERE id = ? LIMIT 1",
                    String.class, employeeId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private String roleType(String roleType) {
        String normalized = roleType == null || roleType.isBlank() ? "PRIMARY" : roleType.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_TYPES.contains(normalized)) {
            throw InsuranceApiException.badRequest("roleType must be one of PRIMARY, BACKUP, TEMPORARY, SUPERVISOR");
        }
        return normalized;
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw InsuranceApiException.badRequest(field + " is required and must be at most " + maxLength + " characters");
        }
        return value.trim();
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String ts(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static String ts(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private static String assignmentJson(
            String assignmentId, String employeeId, String roleType,
            LocalDateTime startTime, LocalDateTime endTime, String status, String changeReason
    ) {
        return "{\"id\":\"" + assignmentId + "\",\"employeeId\":\"" + employeeId
                + "\",\"roleType\":\"" + roleType + "\",\"startTime\":\"" + ts(startTime)
                + "\",\"endTime\":\"" + ts(endTime) + "\",\"status\":\"" + status
                + "\",\"changeReason\":\"" + changeReason + "\"}";
    }

    private static String metadata(String subjectRef, String employeeId, String roleType) {
        return "{\"subjectRef\":\"" + subjectRef + "\",\"employeeId\":\"" + employeeId
                + "\",\"roleType\":\"" + roleType + "\"}";
    }

    private static Object[] concat(List<Object> args, Object... tail) {
        Object[] result = new Object[args.size() + tail.length];
        for (int i = 0; i < args.size(); i++) {
            result[i] = args.get(i);
        }
        System.arraycopy(tail, 0, result, args.size(), tail.length);
        return result;
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系服务-----------
