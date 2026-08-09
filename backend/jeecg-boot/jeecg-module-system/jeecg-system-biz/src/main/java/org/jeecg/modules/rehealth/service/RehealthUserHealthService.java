package org.jeecg.modules.rehealth.service;

import org.jeecg.modules.rehealth.vo.RehealthPatientPageVO;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.jeecg.modules.system.entity.SysUserTenant;
import org.jeecg.modules.system.service.ISysUserTenantService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RehealthUserHealthService {
    static final int MAX_PAGE_SIZE = 100;

    private static final String BASE_SELECT = """
            SELECT u.id, u.sex, u.status, u.create_time,
                   p.name, p.gender, p.age, p.height_cm, p.weight_kg, p.bmi,
                   p.family_history, p.smoking, p.drinking,
                   p.diabetes_history, p.hypertension_history, p.updated_at,
                   risk.risk_score, risk.risk_level, risk.model_version, risk.evaluated_at
            FROM sys_user_tenant sut
            JOIN sys_user u ON u.id = sut.user_id AND u.del_flag = 0
            LEFT JOIN rehealth_patient_profile p ON p.user_id = u.id
            LEFT JOIN (
                SELECT user_id, risk_score, risk_level, model_version, evaluated_at
                FROM (
                    SELECT user_id, risk_score, risk_level, model_version, evaluated_at,
                           ROW_NUMBER() OVER (
                               PARTITION BY user_id ORDER BY evaluated_at DESC, id DESC
                           ) AS row_number
                    FROM rehealth_cvd_risk_result
                    WHERE COALESCE(is_mock, 0) = 0
                ) ranked_risk
                WHERE row_number = 1
            ) risk ON risk.user_id = u.id
            WHERE sut.tenant_id = ? AND sut.status = '1'
            """;

    private final ISysUserTenantService userTenantService;
    private final JdbcTemplate softwareJdbc;
    private final RehealthDeviceHealthClient deviceHealthClient;

    @Autowired
    public RehealthUserHealthService(
            ISysUserTenantService userTenantService,
            @Qualifier("rehealthSoftwareJdbcTemplate") ObjectProvider<JdbcTemplate> softwareJdbc,
            RehealthDeviceHealthClient deviceHealthClient
    ) {
        this(userTenantService, softwareJdbc.getIfAvailable(), deviceHealthClient);
    }

    RehealthUserHealthService(
            ISysUserTenantService userTenantService,
            JdbcTemplate softwareJdbc,
            RehealthDeviceHealthClient deviceHealthClient
    ) {
        this.userTenantService = userTenantService;
        this.softwareJdbc = softwareJdbc;
        this.deviceHealthClient = deviceHealthClient;
    }

    public RehealthPatientPageVO listPatients(
            String requestedTenantId,
            String currentUserId,
            Integer requestedPageNo,
            Integer requestedPageSize,
            String keyword,
            String requestedRiskLevel
    ) {
        int tenantId = resolveTenant(requestedTenantId, currentUserId);
        JdbcTemplate jdbc = requireSoftwareJdbc();
        int pageNo = requestedPageNo == null ? 1 : Math.max(1, requestedPageNo);
        int pageSize = boundPageSize(requestedPageSize);
        String normalizedKeyword = keyword == null ? "" : keyword.strip();
        if (normalizedKeyword.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KEYWORD_TOO_LONG");
        }
        boolean hasKeyword = !normalizedKeyword.isBlank();
        String riskLevel = requestedRiskLevel == null
                ? ""
                : requestedRiskLevel.strip().toLowerCase(Locale.ROOT);
        if (!riskLevel.isEmpty() && !Set.of("high", "medium", "low").contains(riskLevel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_RISK_LEVEL");
        }
        String keywordClause = hasKeyword
                ? " AND (LOWER(COALESCE(p.name, '')) LIKE ? OR u.id = ?)"
                : "";
        String riskClause = riskLevel.isEmpty() ? "" : " AND risk.risk_level = ?";
        List<Object> countArgs = new ArrayList<>();
        countArgs.add(tenantId);
        if (hasKeyword) {
            countArgs.add("%" + normalizedKeyword.toLowerCase() + "%");
            countArgs.add(normalizedKeyword);
        }
        if (!riskLevel.isEmpty()) {
            countArgs.add(riskLevel);
        }
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM (" + BASE_SELECT + keywordClause + riskClause + ") scoped_patients",
                Long.class,
                countArgs.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(countArgs);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<RehealthUserHealthVO> records = jdbc.query(
                BASE_SELECT + keywordClause + riskClause
                        + " ORDER BY u.create_time DESC, u.id LIMIT ? OFFSET ?",
                this::mapPatient,
                pageArgs.toArray()
        );

        RehealthPatientPageVO page = new RehealthPatientPageVO();
        page.setRecords(records);
        page.setTotal(total == null ? 0 : total);
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page.setTotalPages(page.getTotal() == 0 ? 0 : (page.getTotal() + pageSize - 1) / pageSize);
        return page;
    }

    public RehealthUserHealthVO patientDetail(
            String requestedTenantId,
            String currentUserId,
            String patientId
    ) {
        int tenantId = resolveTenant(requestedTenantId, currentUserId);
        JdbcTemplate jdbc = requireSoftwareJdbc();
        if (patientId == null || patientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PATIENT_ID_REQUIRED");
        }
        List<RehealthUserHealthVO> matches = jdbc.query(
                BASE_SELECT + " AND u.id = ? LIMIT 1",
                this::mapPatient,
                tenantId,
                patientId
        );
        if (matches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_IN_TENANT");
        }
        RehealthUserHealthVO patient = matches.get(0);
        var telemetry = deviceHealthClient.fetch(String.valueOf(tenantId), patientId);
        patient.setTelemetry(telemetry);
        if (isSyntheticTelemetry(telemetry)) {
            patient.setLatestRisk(null);
        }
        return patient;
    }

    int resolveTenant(String requestedTenantId, String currentUserId) {
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TENANT_ID_REQUIRED");
        }
        final int tenantId;
        try {
            tenantId = Integer.parseInt(requestedTenantId);
        } catch (NumberFormatException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TENANT_ID");
        }
        SysUserTenant membership = userTenantService.getUserTenantByTenantId(currentUserId, tenantId);
        if (membership == null || !"1".equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED");
        }
        return tenantId;
    }

    static int boundPageSize(Integer requestedPageSize) {
        if (requestedPageSize == null) {
            return 20;
        }
        return Math.max(1, Math.min(requestedPageSize, MAX_PAGE_SIZE));
    }

    static boolean isSyntheticTelemetry(com.alibaba.fastjson.JSONObject telemetry) {
        return telemetry != null && telemetry.getBooleanValue("isSynthetic");
    }

    private JdbcTemplate requireSoftwareJdbc() {
        if (softwareJdbc == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SOFTWARE_DB_UNAVAILABLE");
        }
        return softwareJdbc;
    }

    private RehealthUserHealthVO mapPatient(ResultSet result, int rowNumber) throws SQLException {
        RehealthUserHealthVO patient = new RehealthUserHealthVO();
        patient.setId(result.getString("id"));
        patient.setDisplayName(displayLabel(result.getString("name"), patient.getId()));
        patient.setSex(nullableInteger(result, "sex"));
        patient.setStatus(nullableInteger(result, "status"));
        patient.setCreateTime(result.getTimestamp("create_time"));

        if (result.getString("name") != null || result.getString("gender") != null
                || result.getObject("age") != null) {
            RehealthUserHealthVO.ProfileSummary profile = new RehealthUserHealthVO.ProfileSummary();
            profile.setName(result.getString("name"));
            profile.setGender(result.getString("gender"));
            profile.setAge(nullableInteger(result, "age"));
            profile.setHeightCm(nullableDouble(result, "height_cm"));
            profile.setWeightKg(nullableDouble(result, "weight_kg"));
            profile.setBmi(nullableDouble(result, "bmi"));
            profile.setFamilyHistory(nullableBoolean(result, "family_history"));
            profile.setSmoking(nullableBoolean(result, "smoking"));
            profile.setDrinking(nullableBoolean(result, "drinking"));
            profile.setDiabetesHistory(nullableBoolean(result, "diabetes_history"));
            profile.setHypertensionHistory(nullableBoolean(result, "hypertension_history"));
            profile.setUpdatedAt(result.getTimestamp("updated_at"));
            patient.setProfile(profile);
        }

        if (result.getObject("risk_score") != null) {
            RehealthUserHealthVO.RiskSummary risk = new RehealthUserHealthVO.RiskSummary();
            risk.setScore(result.getDouble("risk_score"));
            risk.setLevel(result.getString("risk_level"));
            risk.setModelVersion(result.getString("model_version"));
            risk.setEvaluatedAt(result.getTimestamp("evaluated_at"));
            patient.setLatestRisk(risk);
        }
        return patient;
    }

    private static String displayLabel(String profileName, String userId) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName.strip();
        }
        String suffix = userId == null ? "未知" : userId.substring(Math.max(0, userId.length() - 6));
        return "患者-" + suffix;
    }

    private static Integer nullableInteger(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet result, String column) throws SQLException {
        boolean value = result.getBoolean(column);
        return result.wasNull() ? null : value;
    }
}
