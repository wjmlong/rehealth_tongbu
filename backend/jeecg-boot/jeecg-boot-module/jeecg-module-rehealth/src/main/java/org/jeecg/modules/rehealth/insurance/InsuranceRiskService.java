package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Dashboard;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.BusinessSummary;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.InsuredDetail;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.InsuredFilterOptions;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.InsuredPage;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Intervention;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.PositiveFactor;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Risk;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.RiskDistribution;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.Subject;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.UnavailableMetric;
import static org.jeecg.modules.rehealth.insurance.InsuranceRiskResponse.SubjectBusiness;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceRiskService {
    public static final String RESPONSIBILITY_SCOPE_MODE = "assigned_app_users";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_CHANNEL_LENGTH = 100;

    private final InsuranceRiskRepository repository;
    private final InsuranceBusinessRepository businessRepository;
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
        this(repository, null, objectMapper, devTenantMembershipScopeEnabled, runtimeMode);
    }

    @Autowired
    public InsuranceRiskService(
            InsuranceRiskRepository repository,
            InsuranceBusinessRepository businessRepository,
            ObjectMapper objectMapper,
            @Value("${rehealth.insurance.tenant-membership-dev-scope-enabled:false}")
            boolean devTenantMembershipScopeEnabled,
            @Value("${rehealth.runtime.mode:development}") String runtimeMode
    ) {
        this.repository = repository;
        this.businessRepository = businessRepository;
        this.objectMapper = objectMapper;
        this.devTenantMembershipScopeEnabled = devTenantMembershipScopeEnabled;
        this.developmentRuntime = runtimeMode != null
                && "development".equals(runtimeMode.trim().toLowerCase(Locale.ROOT));
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】风险查询接入三级范围-----------
    public Dashboard dashboard(int tenantId, InsuranceAssignmentScope scope) {
        requireScopeEnabled();
        InsuranceRiskRepository.DashboardSnapshot snapshot = query(() -> dashboardSnapshot(tenantId, scope));
        InsuranceRiskRepository.BusinessSnapshot business = query(() -> businessRepository == null
                ? new InsuranceRiskRepository.BusinessSnapshot(0, 0, 0, null, null, 0, "unknown", null)
                : businessRepository.tenant(tenantId));
        return new Dashboard(
                RESPONSIBILITY_SCOPE_MODE,
                snapshot.totalInsured(),
                snapshot.assessedInsured(),
                snapshot.syntheticInsured(),
                snapshot.unassessedInsured(),
                new RiskDistribution(snapshot.highRisk(), snapshot.mediumRisk(), snapshot.lowRisk()),
                instant(snapshot.latestEvaluatedAt()),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected(),
                UnavailableMetric.notConnected(),
                businessSummary(business)
        );
    }

    private InsuranceRiskRepository.DashboardSnapshot dashboardSnapshot(int tenantId, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return repository.dashboard(tenantId);
        }
        if (scope.team()) {
            return repository.dashboard(tenantId, scope);
        }
        return repository.dashboard(tenantId, scope.userId());
    }

    public Dashboard dashboard(int tenantId, String managerUserId) {
        return dashboard(tenantId, managerUserId == null
                ? null
                : new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF));
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】风险查询接入三级范围-----------

    public Dashboard dashboard(int tenantId) {
        return dashboard(tenantId, (InsuranceAssignmentScope) null);
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】风险查询接入三级范围-----------
    public InsuredPage insureds(
            int tenantId,
            InsuranceAssignmentScope scope,
            int pageNo,
            int pageSize,
            String keyword,
            String riskLevel,
            String channel,
            Integer minAge,
            Integer maxAge
    ) {
        validatePage(pageNo, pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedRiskLevel = normalizeFilterRiskLevel(riskLevel);
        String normalizedChannel = normalizeChannel(channel);
        validateAgeRange(minAge, maxAge);
        requireScopeEnabled();
        boolean usesMemberFilters = normalizedChannel != null || minAge != null || maxAge != null;
        InsuranceRiskRepository.SubjectPage page = query(() -> subjectsPage(
                tenantId, scope, pageNo, pageSize, normalizedKeyword, normalizedRiskLevel,
                normalizedChannel, minAge, maxAge, usesMemberFilters));
        return new InsuredPage(
                RESPONSIBILITY_SCOPE_MODE,
                pageNo,
                pageSize,
                page.total(),
                page.records().stream().map(this::subject).toList()
        );
    }

    private InsuranceRiskRepository.SubjectPage subjectsPage(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize,
            String keyword, String riskLevel, String channel, Integer minAge, Integer maxAge,
            boolean usesMemberFilters
    ) {
        if (scope == null) {
            return usesMemberFilters
                    ? repository.subjects(tenantId, (String) null, pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge)
                    : repository.subjects(tenantId, pageNo, pageSize, keyword, riskLevel);
        }
        if (scope.team()) {
            return usesMemberFilters
                    ? repository.subjects(tenantId, scope, pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge)
                    : repository.subjects(tenantId, scope, pageNo, pageSize, keyword, riskLevel);
        }
        return usesMemberFilters
                ? repository.subjects(tenantId, scope.userId(), pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge)
                : repository.subjects(tenantId, scope.userId(), pageNo, pageSize, keyword, riskLevel);
    }

    public InsuredPage insureds(
            int tenantId,
            InsuranceAssignmentScope scope,
            int pageNo,
            int pageSize,
            String keyword,
            String riskLevel
    ) {
        return insureds(tenantId, scope, pageNo, pageSize, keyword, riskLevel, null, null, null);
    }

    public InsuredFilterOptions filterOptions(int tenantId, InsuranceAssignmentScope scope) {
        requireScopeEnabled();
        InsuranceRiskRepository.FilterOptions options = query(() -> {
            if (scope == null) {
                return repository.filterOptions(tenantId, (String) null);
            }
            if (scope.team()) {
                return repository.filterOptions(tenantId, scope);
            }
            return repository.filterOptions(tenantId, scope.userId());
        });
        return new InsuredFilterOptions(RESPONSIBILITY_SCOPE_MODE, options.channels(), options.minAge(), options.maxAge());
    }

    public InsuredDetail insured(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        String normalizedSubjectId = normalizeSubjectId(subjectId);
        requireScopeEnabled();
        InsuranceRiskRepository.SubjectSnapshot snapshot = query(() -> {
            if (scope == null) {
                return repository.subject(tenantId, normalizedSubjectId);
            }
            if (scope.team()) {
                return repository.subject(tenantId, scope, normalizedSubjectId);
            }
            return repository.subject(tenantId, scope.userId(), normalizedSubjectId);
        })
                .orElseThrow(() -> InsuranceApiException.notFound("insured subject was not found in the requested tenant"));
        return new InsuredDetail(RESPONSIBILITY_SCOPE_MODE, subject(tenantId, snapshot));
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】风险查询接入三级范围-----------

    public InsuredPage insureds(
            int tenantId,
            String managerUserId,
            int pageNo,
            int pageSize,
            String keyword,
            String riskLevel
    ) {
        return insureds(tenantId, selfScope(managerUserId), pageNo, pageSize, keyword, riskLevel, null, null, null);
    }

    public InsuredPage insureds(
            int tenantId,
            String managerUserId,
            int pageNo,
            int pageSize,
            String keyword,
            String riskLevel,
            String channel,
            Integer minAge,
            Integer maxAge
    ) {
        return insureds(tenantId, selfScope(managerUserId), pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge);
    }

    public InsuredPage insureds(int tenantId, int pageNo, int pageSize, String keyword, String riskLevel) {
        return insureds(tenantId, (InsuranceAssignmentScope) null, pageNo, pageSize, keyword, riskLevel);
    }

    public InsuredFilterOptions filterOptions(int tenantId, String managerUserId) {
        return filterOptions(tenantId, selfScope(managerUserId));
    }

    public InsuredDetail insured(int tenantId, String managerUserId, String subjectId) {
        return insured(tenantId, selfScope(managerUserId), subjectId);
    }

    public InsuredDetail insured(int tenantId, String subjectId) {
        return insured(tenantId, (InsuranceAssignmentScope) null, subjectId);
    }

    private static InsuranceAssignmentScope selfScope(String managerUserId) {
        return managerUserId == null
                ? null
                : new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF);
    }

    private Subject subject(InsuranceRiskRepository.SubjectSnapshot snapshot) {
        return new Subject(
                snapshot.subjectId(),
                displayName(snapshot.name()),
                snapshot.age(),
                normalizeGender(snapshot.gender()),
                snapshot.bmi(),
                snapshot.productName(),
                snapshot.channelName(),
                risk(snapshot),
                intervention(snapshot),
                SubjectBusiness.unavailable()
        );
    }

    private Subject subject(int tenantId, InsuranceRiskRepository.SubjectSnapshot snapshot) {
        InsuranceRiskRepository.BusinessSnapshot business = query(() -> businessRepository == null
                ? null
                : businessRepository.subject(tenantId, snapshot.subjectId()));
        return new Subject(
                snapshot.subjectId(),
                displayName(snapshot.name()),
                snapshot.age(),
                normalizeGender(snapshot.gender()),
                snapshot.bmi(),
                snapshot.productName(),
                snapshot.channelName(),
                risk(snapshot),
                intervention(snapshot),
                subjectBusiness(business)
        );
    }

    private BusinessSummary businessSummary(InsuranceRiskRepository.BusinessSnapshot snapshot) {
        if (snapshot == null) {
            return BusinessSummary.unavailable();
        }
        return new BusinessSummary(
                snapshot.activePolicies(),
                snapshot.activeCoverages(),
                snapshot.claimCount(),
                snapshot.billedAmount(),
                snapshot.paidAmount(),
                snapshot.activeInterventions(),
                instant(snapshot.latestUpdatedAt())
        );
    }

    private SubjectBusiness subjectBusiness(InsuranceRiskRepository.BusinessSnapshot snapshot) {
        if (snapshot == null) {
            return SubjectBusiness.unavailable();
        }
        String interventionStatus = snapshot.activeInterventions() > 0 ? "active" : "none";
        return new SubjectBusiness(
                snapshot.activePolicies(),
                snapshot.activeCoverages(),
                snapshot.claimCount(),
                snapshot.billedAmount(),
                snapshot.paidAmount(),
                snapshot.consentStatus(),
                interventionStatus,
                instant(snapshot.latestUpdatedAt())
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

    private String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "未命名受保人";
        }
        return name.trim();
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

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        String normalized = channel.trim();
        if (normalized.length() > MAX_CHANNEL_LENGTH) {
            throw InsuranceApiException.badRequest("channel must not exceed 100 characters");
        }
        return normalized;
    }

    private void validateAgeRange(Integer minAge, Integer maxAge) {
        if ((minAge != null && (minAge < 0 || minAge > 130))
                || (maxAge != null && (maxAge < 0 || maxAge > 130))) {
            throw InsuranceApiException.badRequest("age filters must be between 0 and 130");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw InsuranceApiException.badRequest("minAge must not exceed maxAge");
        }
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
