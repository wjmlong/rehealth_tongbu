package org.jeecg.modules.rehealth.service;

import com.alibaba.fastjson.JSONArray;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RehealthUserHealthService {
    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VERIFIED_REAL_PROVENANCE = Set.of(
            "hband_wearable",
            "hband_cloud_restore",
            "viomi_cloud",
            "mrd_ring",
            "mrd-sdk",
            "rwfit"
    );
    private static final Set<String> SYNTHETIC_PROVENANCE_MARKERS = Set.of(
            "synthetic", "mock", "test_seed", "ring_sim", "demo", "sample"
    );

    static final String BASE_SELECT = """
            SELECT u.id, u.sex, u.status, u.create_time,
                   p.name, p.gender, p.age, p.height_cm, p.weight_kg, p.bmi,
                   p.family_history, p.smoking, p.drinking,
                   p.diabetes_history, p.hypertension_history, p.updated_at,
                   risk.risk_score, risk.risk_level, risk.model_version, risk.evaluated_at,
                   risk.is_mock, risk.factor_contribution_json,
                   plan.priority_intervention, plan.rationale AS intervention_rationale,
                   plan.expected_impact AS intervention_expected_impact,
                   plan.confidence AS intervention_confidence,
                   plan.model_version AS intervention_model_version,
                   plan.generated_at AS intervention_generated_at,
                   plan.is_mock AS intervention_is_mock,
                   plan.medical_disclaimer AS intervention_medical_disclaimer
            FROM sys_user_tenant sut
            JOIN sys_user u ON u.id = sut.user_id AND u.del_flag = 0
            JOIN rehealth_patient_profile p ON p.user_id = u.id
            LEFT JOIN (
                SELECT user_id, risk_score, risk_level, model_version, evaluated_at,
                       is_mock, factor_contribution_json
                FROM (
                    SELECT user_id, risk_score, risk_level, model_version, evaluated_at,
                           is_mock, factor_contribution_json,
                           ROW_NUMBER() OVER (
                               PARTITION BY user_id
                               ORDER BY COALESCE(is_mock, 1) ASC, evaluated_at DESC, id DESC
                           ) AS risk_row_num
                    FROM rehealth_cvd_risk_result
                ) ranked_risk
                WHERE risk_row_num = 1
            ) risk ON risk.user_id = u.id
            LEFT JOIN (
                SELECT user_id, priority_intervention, rationale, expected_impact,
                       confidence, model_version, generated_at, is_mock, medical_disclaimer
                FROM (
                    SELECT user_id, priority_intervention, rationale, expected_impact,
                           confidence, model_version, generated_at, is_mock, medical_disclaimer,
                           ROW_NUMBER() OVER (
                               PARTITION BY user_id
                               ORDER BY COALESCE(is_mock, 1) ASC, generated_at DESC, id DESC
                           ) AS intervention_row_num
                    FROM rehealth_intervention_plan
                ) ranked_intervention
                WHERE intervention_row_num = 1
            ) plan ON plan.user_id = u.id
            WHERE sut.tenant_id = ? AND sut.status = '1'
              AND NOT EXISTS (
                  SELECT 1
                  FROM sys_user_tenant other_membership
                  WHERE other_membership.user_id = u.id
                    AND other_membership.status = '1'
                    AND other_membership.tenant_id <> sut.tenant_id
              )
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
        attachTelemetry(patient, telemetry);
        attachDailyIndices(patient, jdbc);
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
        if (telemetry == null) {
            return false;
        }
        if (telemetry.getBooleanValue("isSynthetic")) {
            return true;
        }
        var provenance = telemetry.getJSONArray("provenance");
        if (provenance == null) {
            return false;
        }
        return provenance.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(source -> source.toLowerCase(Locale.ROOT))
                .anyMatch(source -> SYNTHETIC_PROVENANCE_MARKERS.stream().anyMatch(source::contains));
    }

    static void attachTelemetry(
            RehealthUserHealthVO patient,
            com.alibaba.fastjson.JSONObject telemetry
    ) {
        patient.setTelemetry(telemetry);
        String provenanceStatus = provenanceStatus(telemetry);
        patient.setProvenanceStatus(provenanceStatus);
        RehealthUserHealthVO.RiskSummary risk = patient.getLatestRisk();
        boolean verifiedRealRisk = "verified_real".equals(provenanceStatus)
                && risk != null && !Boolean.TRUE.equals(risk.getIsMock());
        boolean syntheticPreviewRisk = "synthetic".equals(provenanceStatus)
                && risk != null && Boolean.TRUE.equals(risk.getIsMock());
        if (!verifiedRealRisk && !syntheticPreviewRisk) {
            patient.setLatestRisk(null);
        }
        RehealthUserHealthVO.InterventionSummary intervention = patient.getLatestIntervention();
        boolean verifiedRealIntervention = "verified_real".equals(provenanceStatus)
                && intervention != null && !Boolean.TRUE.equals(intervention.getIsMock());
        boolean syntheticPreviewIntervention = "synthetic".equals(provenanceStatus)
                && intervention != null && Boolean.TRUE.equals(intervention.getIsMock());
        if (!verifiedRealIntervention && !syntheticPreviewIntervention) {
            patient.setLatestIntervention(null);
        }
    }

    private void attachDailyIndices(RehealthUserHealthVO patient, JdbcTemplate jdbc) {
        String provenanceStatus = patient.getProvenanceStatus();
        if (!Set.of("verified_real", "synthetic").contains(provenanceStatus)) {
            return;
        }
        List<RehealthUserHealthVO.RhiSummary> rhiRows = jdbc.query("""
                SELECT display_score, data_confidence, status, scored_on,
                       algorithm_version, calculation_source
                FROM rehealth_rhi_daily_snapshot
                WHERE user_id = ?
                ORDER BY scored_on DESC, updated_at DESC
                LIMIT 1
                """, (result, rowNumber) -> {
            RehealthUserHealthVO.RhiSummary summary = new RehealthUserHealthVO.RhiSummary();
            summary.setDisplayScore(result.getDouble("display_score"));
            summary.setDataConfidence(result.getDouble("data_confidence"));
            summary.setStatus(result.getString("status"));
            summary.setScoredOn(result.getDate("scored_on"));
            summary.setAlgorithmVersion(result.getString("algorithm_version"));
            summary.setCalculationSource(result.getString("calculation_source"));
            summary.setIsMock(isSyntheticSource(summary.getCalculationSource()));
            return summary;
        }, patient.getId());
        if (!rhiRows.isEmpty() && previewMatchesProvenance(provenanceStatus, rhiRows.get(0).getIsMock())) {
            patient.setLatestRhi(rhiRows.get(0));
        }

        List<RehealthUserHealthVO.RdiSummary> rdiRows = jdbc.query("""
                SELECT display_score, data_confidence, status, scored_on, is_mock,
                       algorithm_version, calculation_source
                FROM rehealth_rdi_daily_snapshot
                WHERE user_id = ?
                ORDER BY scored_on DESC, updated_at DESC
                LIMIT 1
                """, (result, rowNumber) -> {
            RehealthUserHealthVO.RdiSummary summary = new RehealthUserHealthVO.RdiSummary();
            summary.setDisplayScore(result.getDouble("display_score"));
            summary.setDataConfidence(result.getDouble("data_confidence"));
            summary.setStatus(result.getString("status"));
            summary.setScoredOn(result.getDate("scored_on"));
            summary.setIsMock(result.getBoolean("is_mock"));
            summary.setAlgorithmVersion(result.getString("algorithm_version"));
            summary.setCalculationSource(result.getString("calculation_source"));
            return summary;
        }, patient.getId());
        if (!rdiRows.isEmpty() && previewMatchesProvenance(provenanceStatus, rdiRows.get(0).getIsMock())) {
            patient.setLatestRdi(rdiRows.get(0));
        }
    }

    static boolean previewMatchesProvenance(String provenanceStatus, Boolean isMock) {
        return ("verified_real".equals(provenanceStatus) && !Boolean.TRUE.equals(isMock))
                || ("synthetic".equals(provenanceStatus) && Boolean.TRUE.equals(isMock));
    }

    static boolean isSyntheticSource(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        String normalized = source.strip().toLowerCase(Locale.ROOT);
        return SYNTHETIC_PROVENANCE_MARKERS.stream().anyMatch(normalized::contains)
                || normalized.contains("local_test");
    }

    static String provenanceStatus(com.alibaba.fastjson.JSONObject telemetry) {
        if (isSyntheticTelemetry(telemetry)) {
            return "synthetic";
        }
        if (telemetry == null || telemetry.getJSONArray("provenance") == null
                || telemetry.getJSONArray("provenance").isEmpty()) {
            return "unknown";
        }
        Set<String> normalized = new HashSet<>();
        for (Object source : telemetry.getJSONArray("provenance")) {
            if (!(source instanceof String value) || value.isBlank()) {
                return "unknown";
            }
            normalized.add(value.strip().toLowerCase(Locale.ROOT));
        }
        return !normalized.isEmpty() && VERIFIED_REAL_PROVENANCE.containsAll(normalized)
                ? "verified_real"
                : "unknown";
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
            risk.setIsMock(nullableBoolean(result, "is_mock"));
            String factorContributions = result.getString("factor_contribution_json");
            if (factorContributions != null && !factorContributions.isBlank()) {
                try {
                    risk.setFactorContributions(JSONArray.parseArray(factorContributions));
                } catch (RuntimeException invalidStoredJson) {
                    risk.setFactorContributions(new JSONArray());
                }
            }
            patient.setLatestRisk(risk);
        }
        if (result.getString("priority_intervention") != null) {
            RehealthUserHealthVO.InterventionSummary intervention =
                    new RehealthUserHealthVO.InterventionSummary();
            intervention.setPriorityIntervention(result.getString("priority_intervention"));
            intervention.setRationale(result.getString("intervention_rationale"));
            intervention.setExpectedImpact(result.getString("intervention_expected_impact"));
            intervention.setConfidence(nullableDouble(result, "intervention_confidence"));
            intervention.setModelVersion(result.getString("intervention_model_version"));
            intervention.setGeneratedAt(result.getTimestamp("intervention_generated_at"));
            intervention.setIsMock(nullableBoolean(result, "intervention_is_mock"));
            intervention.setMedicalDisclaimer(result.getString("intervention_medical_disclaimer"));
            patient.setLatestIntervention(intervention);
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
