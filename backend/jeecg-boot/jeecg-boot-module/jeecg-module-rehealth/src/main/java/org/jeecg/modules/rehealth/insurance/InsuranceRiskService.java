package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Dashboard;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.InsuredDetail;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.InsuredPage;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Intervention;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.PositiveFactor;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Risk;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.RiskDistribution;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Subject;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.UnavailableMetric;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceRiskService {
    public static final String DEV_SCOPE_MODE = "tenant_membership_dev_only";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final InsuranceRiskRepository repository;
    private final ObjectMapper objectMapper;
    private final boolean devTenantMembershipScopeEnabled;
    private final boolean developmentRuntime;

    public InsuranceRiskService(
            InsuranceRiskRepository repository,
            ObjectMapper objectMapper,
            @Value("${rehealth.insurance.tenant-membership-dev-scope-enabled:false}")
            boolean devTenantMembershipScopeEnabled,
            @Value("${rehealth.runtime.mode:development}") String runtimeMode
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.devTenantMembershipScopeEnabled = devTenantMembershipScopeEnabled;
        this.developmentRuntime = runtimeMode != null
                && "development".equals(runtimeMode.trim().toLowerCase(Locale.ROOT));
    }

    public Dashboard dashboard(int tenantId) {
        requireScopeEnabled();
        InsuranceRiskRepository.DashboardSnapshot snapshot = query(() -> repository.dashboard(tenantId));
        return new Dashboard(
                DEV_SCOPE_MODE,
                snapshot.totalInsured(),
                snapshot.assessedInsured(),
                snapshot.syntheticInsured(),
                snapshot.unassessedInsured(),
                new RiskDistribution(snapshot.highRisk(), snapshot.mediumRisk(), snapshot.lowRisk()),
                instant(snapshot.latestEvaluatedAt()),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected()
        );
    }

    public InsuredPage insureds(
            int tenantId,
            int pageNo,
            int pageSize,
            String keyword,
            String riskLevel
    ) {
        validatePage(pageNo, pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedRiskLevel = normalizeFilterRiskLevel(riskLevel);
        requireScopeEnabled();
        InsuranceRiskRepository.SubjectPage page = query(() -> repository.subjects(
                tenantId,
                pageNo,
                pageSize,
                normalizedKeyword,
                normalizedRiskLevel
        ));
        return new InsuredPage(
                DEV_SCOPE_MODE,
                pageNo,
                pageSize,
                page.total(),
                page.records().stream().map(this::subject).toList()
        );
    }

    public InsuredDetail insured(int tenantId, String subjectId) {
        String normalizedSubjectId = normalizeSubjectId(subjectId);
        requireScopeEnabled();
        InsuranceRiskRepository.SubjectSnapshot snapshot = query(() -> repository.subject(tenantId, normalizedSubjectId))
                .orElseThrow(() -> InsuranceApiException.notFound("insured subject was not found in the requested tenant"));
        return new InsuredDetail(DEV_SCOPE_MODE, subject(snapshot));
    }

    private Subject subject(InsuranceRiskRepository.SubjectSnapshot snapshot) {
        return new Subject(
                snapshot.subjectId(),
                maskedName(snapshot.name(), snapshot.subjectId()),
                snapshot.age(),
                normalizeGender(snapshot.gender()),
                snapshot.bmi(),
                risk(snapshot),
                intervention(snapshot)
        );
    }

    private Risk risk(InsuranceRiskRepository.SubjectSnapshot snapshot) {
        if (!snapshot.hasRisk()) {
            return new Risk("unassessed", null, null, null, null, null);
        }
        if (!snapshot.hasVerifiedRisk()) {
            return new Risk(
                    "synthetic",
                    null,
                    null,
                    snapshot.modelVersion(),
                    instant(snapshot.evaluatedAt()),
                    null
            );
        }
        return new Risk(
                "assessed",
                snapshot.riskScore(),
                normalizeStoredRiskLevel(snapshot.riskLevel()),
                snapshot.modelVersion(),
                instant(snapshot.evaluatedAt()),
                positiveFactors(snapshot.contributionJson())
        );
    }

    private Intervention intervention(InsuranceRiskRepository.SubjectSnapshot snapshot) {
        if (!snapshot.hasVerifiedIntervention()) {
            return new Intervention("not_available", null, null);
        }
        return new Intervention(
                "available",
                null,
                instant(snapshot.interventionGeneratedAt())
        );
    }

    private List<PositiveFactor> positiveFactors(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return List.of();
            }
            List<PositiveFactor> factors = new ArrayList<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (entry.getKey() != null
                        && !entry.getKey().isBlank()
                        && entry.getKey().length() <= 128
                        && value != null
                        && value.isNumber()) {
                    double contribution = value.doubleValue();
                    if (Double.isFinite(contribution) && contribution > 0) {
                        factors.add(new PositiveFactor(entry.getKey(), contribution));
                    }
                }
            });
            return factors.stream()
                    .sorted(Comparator.comparingDouble(PositiveFactor::contribution).reversed())
                    .limit(5)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String maskedName(String name, String subjectId) {
        if (name == null || name.isBlank()) {
            return "受保人-" + shortSubjectToken(subjectId);
        }
        int[] codePoints = name.trim().codePoints().toArray();
        if (codePoints.length == 1) {
            return "*";
        }
        String first = new String(codePoints, 0, 1);
        if (codePoints.length == 2) {
            return first + "*";
        }
        String last = new String(codePoints, codePoints.length - 1, 1);
        return first + "*".repeat(Math.min(codePoints.length - 2, 6)) + last;
    }

    private String shortSubjectToken(String subjectId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(subjectId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        return switch (gender.trim().toLowerCase(Locale.ROOT)) {
            case "1", "male", "m", "男" -> "male";
            case "2", "female", "f", "女" -> "female";
            default -> null;
        };
    }

    private String normalizeStoredRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        return switch (riskLevel.trim().toLowerCase(Locale.ROOT)) {
            case "high", "very_high", "severe" -> "high";
            case "medium", "moderate" -> "medium";
            case "low" -> "low";
            default -> null;
        };
    }

    private String normalizeFilterRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        String normalized = riskLevel.trim().toLowerCase(Locale.ROOT);
        if (!List.of("high", "medium", "low").contains(normalized)) {
            throw InsuranceApiException.badRequest("riskLevel must be one of high, medium, or low");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw InsuranceApiException.badRequest("keyword must not exceed 100 characters");
        }
        return normalized;
    }

    private String normalizeSubjectId(String subjectId) {
        if (subjectId == null || !subjectId.trim().matches("(?i)[0-9a-f]{64}")) {
            throw InsuranceApiException.badRequest("subjectId must be a 64-character pseudonymous reference");
        }
        return subjectId.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePage(int pageNo, int pageSize) {
        if (pageNo < 1) {
            throw InsuranceApiException.badRequest("pageNo must be at least 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw InsuranceApiException.badRequest("pageSize must be between 1 and 100");
        }
    }

    private void requireScopeEnabled() {
        if (!devTenantMembershipScopeEnabled || !developmentRuntime) {
            throw InsuranceApiException.serviceUnavailable(
                    "insured membership data source is not connected; the development-only tenant scope is unavailable"
            );
        }
    }

    private <T> T query(Supplier<T> query) {
        try {
            return query.get();
        } catch (DataAccessException e) {
            throw InsuranceApiException.serviceUnavailable("insurance risk data source is temporarily unavailable");
        }
    }

    private String instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
