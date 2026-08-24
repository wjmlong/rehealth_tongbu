package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.AdherenceRow;
import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.Factor;
import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.HighRiskPerson;
import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.Outcome;
import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.ReportData;
import static org.jeecg.modules.rehealth.insurance.InsuranceInterventionReportResponse.Suggestion;

/**
 * Population-level intervention effect report aggregation.
 *
 * Reuses the insurer intervention workbench's responsibility-scoped subject
 * summaries and adds population windows (risk movement, adherence funnel,
 * RHI/RDI mean deltas and RDI contribution means) so the website report
 * template can render a manager-friendly outcome report from real data.
 *
 * 口径约束:
 * <ul>
 *   <li>风险迁移只统计窗口起点前与当前都有真实风险等级的主体;</li>
 *   <li>依从性漏斗基于近 28 天版本化计划执行事实;</li>
 *   <li>"完成客观复测"复用工作台改善判定(非演练、证据充分);</li>
 *   <li>收缩压/LDL-C/月均赔付未形成人群前后对比口径时输出占位文案,不伪造数据;</li>
 *   <li>范围内存在演练/合成数据时封面强制标注数据状态标签;</li>
 *   <li>演练快照默认不进入因素贡献与 RDI 差值统计,仅本地联调可经
 *       {@code rehealth.insurance.report.include-mock=true} 放开,封面演练标签不受影响。</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceInterventionReportService {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MIN_PERIOD_DAYS = 7;
    private static final int MAX_PERIOD_DAYS = 90;
    private static final int FUNNEL_WINDOW_DAYS = 28;
    private static final int MAX_HIGH_RISK_ROWS = 10;
    private static final int MAX_FACTOR_ROWS = 6;
    private static final List<String> DISTRIBUTION_LABELS = List.of("高风险", "中风险", "低风险");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InsuranceInterventionWorkbenchService workbench;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zoneId;
    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调可经配置纳入演练快照,生产默认排除------------
    private final boolean includeMock;
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调可经配置纳入演练快照,生产默认排除------------

    @Autowired
    public InsuranceInterventionReportService(
            InsuranceInterventionWorkbenchService workbench,
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            @Value("${rehealth.insurance.report.include-mock:false}") boolean includeMock
    ) {
        this(workbench, jdbc, Clock.systemUTC(), DEFAULT_ZONE, includeMock);
    }

    InsuranceInterventionReportService(
            InsuranceInterventionWorkbenchService workbench,
            JdbcTemplate jdbc,
            Clock clock,
            ZoneId zoneId
    ) {
        this(workbench, jdbc, clock, zoneId, false);
    }

    InsuranceInterventionReportService(
            InsuranceInterventionWorkbenchService workbench,
            JdbcTemplate jdbc,
            Clock clock,
            ZoneId zoneId,
            boolean includeMock
    ) {
        this.workbench = workbench;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zoneId = zoneId;
        this.includeMock = includeMock;
    }

    public ReportData reportData(int tenantId, String managerUserId, int periodDays) {
        int window = Math.min(MAX_PERIOD_DAYS, Math.max(MIN_PERIOD_DAYS, periodDays <= 0 ? DEFAULT_PERIOD_DAYS : periodDays));
        LocalDate today = LocalDate.ofInstant(clock.instant(), zoneId);
        LocalDate windowStart = today.minusDays(window);
        LocalDate funnelStart = today.minusDays(FUNNEL_WINDOW_DAYS);

        List<InsuranceInterventionWorkbenchService.Identity> identities =
                workbench.identities(tenantId, managerUserId, null, Integer.MAX_VALUE, 0);
        List<InsuranceInterventionWorkbenchResponse.SubjectSummary> summaries = identities.stream()
                .map(identity -> workbench.summary(tenantId, identity))
                .toList();

        List<String> userIds = identities.stream()
                .map(InsuranceInterventionWorkbenchService.Identity::userId)
                .distinct()
                .toList();
        List<String> subjectRefs = identities.stream()
                .map(InsuranceInterventionWorkbenchService.Identity::subjectRef)
                .distinct()
                .toList();

        Map<String, Integer> levelCounts = new LinkedHashMap<>();
        long pendingAction = 0;
        long inProgress = 0;
        long pendingReview = 0;
        long improved = 0;
        int movedDown = 0;
        int movedUp = 0;
        boolean mockPresent = false;
        List<HighRiskPerson> highRiskPeople = new ArrayList<>();

        for (int index = 0; index < identities.size(); index++) {
            InsuranceInterventionWorkbenchService.Identity identity = identities.get(index);
            InsuranceInterventionWorkbenchResponse.SubjectSummary summary = summaries.get(index);
            String level = InsuranceInterventionWorkbenchService.normalizeLevel(summary.riskLevel());
            if (level != null) {
                levelCounts.merge(levelLabel(level), 1, Integer::sum);
            }
            String status = summary.workflowStatus();
            if ("pending_action".equals(status)) {
                pendingAction++;
            } else if ("in_progress".equals(status)) {
                inProgress++;
            } else if ("improved".equals(status)) {
                improved++;
            } else {
                pendingReview++;
            }
            if (Boolean.TRUE.equals(summary.riskIsMock()) || Boolean.TRUE.equals(summary.rdiIsMock())) {
                mockPresent = true;
            }
            String previous = riskLevelOn(identity.userId(), windowStart);
            if (previous != null && level != null && !previous.equals(level)) {
                if (levelRank(level) < levelRank(previous)) {
                    movedDown++;
                } else {
                    movedUp++;
                }
            }
            if ("high".equals(level) && ("pending_action".equals(status) || "in_progress".equals(status))) {
                highRiskPeople.add(person(identity, summary, status));
            }
        }

        highRiskPeople.sort(Comparator
                .comparing((HighRiskPerson person) -> "in_progress".equals(personPriorityStatus(person)))
                .thenComparing(HighRiskPerson::id));
        List<HighRiskPerson> topPeople = highRiskPeople.stream().limit(MAX_HIGH_RISK_ROWS).toList();

        Map<String, Integer> funnel = adherenceFunnel(tenantId, subjectRefs, funnelStart);
        int planned = funnel.getOrDefault("planned", 0);

        String generatedDate = today.format(DATE_FMT);
        String studyId = "RH-" + tenantId + "-" + generatedDate.replace("-", "");

        return new ReportData(
                "干预效果评估报告",
                "看清人群风险,推动行动落地,验证健康改善",
                "管理规模 · 风险分层 · 优先行动 · 依从执行 · 健康改善",
                studyId,
                generatedDate,
                mockPresent ? "演练数据 · 不可作为真实改善结论" : "",
                identities.size(),
                pendingAction,
                inProgress,
                improved,
                riskDistribution(levelCounts, identities.size(), pendingAction),
                movement(movedDown, movedUp, pendingReview, pendingAction),
                adherenceRows(funnel, planned, improved),
                topPeople,
                suggestions(),
                outcomes(userIds, windowStart, window, mockPresent),
                factors(userIds, mockPresent)
        );
    }

    private static String personPriorityStatus(HighRiskPerson person) {
        return "健康管理师跟进".equals(person.priority()) ? "in_progress" : "pending_action";
    }

    private HighRiskPerson person(
            InsuranceInterventionWorkbenchService.Identity identity,
            InsuranceInterventionWorkbenchResponse.SubjectSummary summary,
            String status
    ) {
        List<String> factorNames = summary.factors() == null
                ? List.of()
                : summary.factors().stream()
                .map(InsuranceInterventionWorkbenchResponse.Factor::key)
                .filter(value -> value != null && !value.isBlank())
                .limit(3)
                .toList();
        boolean bloodPressureSignal = factorNames.stream().anyMatch(this::isBloodPressureFactor);
        StringBuilder signal = new StringBuilder();
        if (summary.riskScore() != null) {
            signal.append("风险分 ").append(percent(summary.riskScore())).append(" · 高风险");
        } else {
            signal.append("高风险");
        }
        if (summary.rhiScore() != null) {
            signal.append(" · RHI ").append(signed(summary.rhiScore(), 1));
        }
        if (summary.rdiScore() != null) {
            signal.append(" · RDI ").append(signed(summary.rdiScore(), 1));
        }
        if (summary.adherenceScore() != null) {
            signal.append(" · 依从性 ").append(percent(summary.adherenceScore()));
        }
        if (!factorNames.isEmpty()) {
            signal.append(" · 主要因素:").append(String.join("、", factorNames));
        }
        String action = summary.currentIntervention();
        if (action == null || action.isBlank()) {
            action = bloodPressureSignal
                    ? "完成 3-7 天血压复测,医生核对处方与实际用药"
                    : "补齐近期睡眠、运动记录并进入人工复核";
        }
        String priority = "pending_action".equals(status)
                ? (bloodPressureSignal ? "今天找医生" : "尽快复核")
                : "健康管理师跟进";
        return new HighRiskPerson(identity.subjectRef(), priority, signal.toString(), action, "高风险");
    }

    private boolean isBloodPressureFactor(String factor) {
        String value = factor == null ? "" : factor.toLowerCase(Locale.ROOT);
        return value.contains("bp") || value.contains("sbp") || value.contains("dbp") || value.contains("blood_pressure");
    }

    private String riskLevelOn(String userId, LocalDate before) {
        return jdbc.query("""
                        SELECT risk_level FROM rehealth_cvd_risk_result
                        WHERE user_id=? AND evaluated_at < ?
                        ORDER BY evaluated_at DESC, id DESC LIMIT 1
                        """,
                (rs, row) -> InsuranceInterventionWorkbenchService.normalizeLevel(rs.getString(1)),
                userId, before.atStartOfDay()
        ).stream().findFirst().orElse(null);
    }

    private Map<String, Integer> adherenceFunnel(int tenantId, List<String> subjectRefs, LocalDate funnelStart) {
        Set<String> planned = new LinkedHashSet<>();
        if (!subjectRefs.isEmpty()) {
            String marks = placeholders(subjectRefs.size());
            Object[] refs = subjectRefs.toArray();
            planned.addAll(jdbc.query("""
                            SELECT DISTINCT occurrence.subject_ref
                            FROM rehealth_care_plan_occurrence occurrence
                            WHERE occurrence.tenant_id=? AND occurrence.status='scheduled'
                              AND occurrence.scheduled_at >= ? AND occurrence.subject_ref IN (%s)
                            """.formatted(marks),
                    (rs, row) -> rs.getString(1),
                    merge(new Object[]{tenantId, funnelStart.atStartOfDay()}, refs)));
            planned.addAll(jdbc.query("""
                            SELECT DISTINCT plan.subject_ref
                            FROM rehealth_care_plan plan
                            WHERE plan.tenant_id=? AND plan.owner_type='insurance'
                              AND plan.status='active' AND plan.subject_ref IN (%s)
                            """.formatted(marks),
                    (rs, row) -> rs.getString(1),
                    merge(new Object[]{tenantId}, refs)));
        }

        Map<String, Integer> funnel = new LinkedHashMap<>();
        funnel.put("planned", planned.size());
        funnel.put("started", 0);
        funnel.put("seven_days", 0);
        funnel.put("full_window", 0);
        if (!subjectRefs.isEmpty()) {
            String marks = placeholders(subjectRefs.size());
            Object[] refs = subjectRefs.toArray();
            jdbc.query("""
                            SELECT execution.subject_ref,
                                   COUNT(*) AS execution_count,
                                   COUNT(DISTINCT DATE(execution.occurred_at)) AS active_days
                            FROM rehealth_care_plan_execution execution
                            WHERE execution.tenant_id=? AND execution.occurred_at >= ?
                              AND execution.subject_ref IN (%s)
                            GROUP BY execution.subject_ref
                            """.formatted(marks),
                    (ResultSet rs, int row) -> {
                        int activeDays = rs.getInt(3);
                        funnel.merge("started", 1, Integer::sum);
                        if (activeDays >= 7) {
                            funnel.merge("seven_days", 1, Integer::sum);
                        }
                        if (activeDays >= FUNNEL_WINDOW_DAYS) {
                            funnel.merge("full_window", 1, Integer::sum);
                        }
                        return null;
                    },
                    merge(new Object[]{tenantId, funnelStart.atStartOfDay()}, refs));
        }
        return funnel;
    }

    private List<AdherenceRow> adherenceRows(Map<String, Integer> funnel, int planned, long improved) {
        return List.of(
                new AdherenceRow("收到行动建议", grouped(planned), "100%", "近 28 天有已安排的计划任务或生效计划"),
                new AdherenceRow("开始行动", grouped(funnel.getOrDefault("started", 0)),
                        share(funnel.getOrDefault("started", 0), planned), "近 28 天至少执行过一次计划任务"),
                new AdherenceRow("连续记录 7 天", grouped(funnel.getOrDefault("seven_days", 0)),
                        share(funnel.getOrDefault("seven_days", 0), planned), "近 28 天至少 7 个不同自然日有执行记录"),
                new AdherenceRow("连续记录 30 天", grouped(funnel.getOrDefault("full_window", 0)),
                        share(funnel.getOrDefault("full_window", 0), planned), "近 28 天全勤口径(28/28 天)"),
                new AdherenceRow("完成客观复测", grouped(improved),
                        share((int) improved, planned), "有可验证的客观效果信号(非演练、证据充分)"),
                new AdherenceRow("全人群用药确认", "待真实接口聚合", "当前未形成口径", "本报告不拿“已改善”代替服药依从")
        );
    }

    private Map<String, Map<String, String>> riskDistribution(
            Map<String, Integer> levelCounts,
            int total,
            long pendingAction
    ) {
        Map<String, String> meaning = Map.of(
                "高风险", "总体风险层级;其中 " + grouped(pendingAction) + " 人进入当前待行动队列",
                "中风险", "需要持续观察,风险上升时进入人工审核",
                "低风险", "以维持和低频复测为主"
        );
        Map<String, Map<String, String>> distribution = new LinkedHashMap<>();
        int assessed = levelCounts.values().stream().mapToInt(Integer::intValue).sum();
        for (String label : DISTRIBUTION_LABELS) {
            int count = levelCounts.getOrDefault(label, 0);
            distribution.put(label, Map.of(
                    "count", grouped(count),
                    "share", share(count, assessed == 0 ? total : assessed),
                    "meaning", meaning.get(label)
            ));
        }
        return distribution;
    }

    private Map<String, Map<String, String>> movement(int down, int up, long pendingReview, long pendingAction) {
        Map<String, Map<String, String>> movement = new LinkedHashMap<>();
        movement.put("down", Map.of("value", grouped(down), "label", "向下迁移",
                "note", "高风险→中/低 与 中风险→低风险 迁移合计"));
        movement.put("up", Map.of("value", grouped(up), "label", "向上迁移",
                "note", "中风险→高风险等向上迁移,需要回到优先队列"));
        movement.put("net", Map.of("value", grouped(down - up), "label", "净向下",
                "note", "向下迁移减去向上迁移"));
        movement.put("review", Map.of("value", grouped(pendingReview), "label", "待人工确认",
                "note", "其中优先处理 " + grouped(pendingAction) + " 人"));
        return movement;
    }

    private List<Outcome> outcomes(List<String> userIds, LocalDate windowStart, int window, boolean mockPresent) {
        List<Outcome> outcomes = new ArrayList<>();
        outcomes.add(meanDeltaOutcome("RHI 综合健康状态", "rehealth_rhi_daily_snapshot",
                "display_score", userIds, windowStart, false, window, "整体健康状态向好"));
        Outcome rdiOutcome = meanDeltaOutcome("RDI 近期风险负荷", "rehealth_rdi_daily_snapshot",
                "display_score", userIds, windowStart, true, window, "近期可干预风险负荷下降");
        if (mockPresent && !includeMock && "数据不足".equals(rdiOutcome.change())) {
            rdiOutcome = new Outcome(rdiOutcome.name(), rdiOutcome.change(),
                    "演练快照不计入统计,当前无真实样本(" + rdiOutcome.meaning() + ")");
        }
        outcomes.add(rdiOutcome);
        outcomes.add(new Outcome("收缩压", "待复测口径",
                "可穿戴无袖带血压不作为效果评估口径,需血压复测数据接入"));
        outcomes.add(new Outcome("LDL-C", "待复测口径",
                "血检为单次快照,尚未形成人群前后对比口径"));
        outcomes.add(new Outcome("月均赔付", "未接入", "仅用于保险公式演示,不是结算承诺"));
        return outcomes;
    }

    private Outcome meanDeltaOutcome(
            String name,
            String table,
            String scoreColumn,
            List<String> userIds,
            LocalDate windowStart,
            boolean excludeMock,
            int window,
            String meaning
    ) {
        if (userIds.isEmpty()) {
            return new Outcome(name, "数据不足", meaning + ";样本不足");
        }
        String marks = placeholders(userIds.size());
        Object[] ids = userIds.toArray();
        //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调放开演练快照,生产默认排除------------
        String mockFilter = excludeMock && !includeMock ? " AND is_mock=0" : "";
        //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调放开演练快照,生产默认排除------------
        String currentSql = """
                SELECT AVG(current.%s)
                FROM %s current
                JOIN (
                  SELECT user_id, MAX(scored_on) AS latest
                  FROM %s WHERE user_id IN (%s)%s
                  GROUP BY user_id
                ) latest ON latest.user_id=current.user_id AND latest.latest=current.scored_on
                """.formatted(scoreColumn, table, table, marks, mockFilter);
        String baselineSql = """
                SELECT AVG(baseline.%s)
                FROM %s baseline
                JOIN (
                  SELECT user_id, MAX(scored_on) AS latest
                  FROM %s WHERE user_id IN (%s) AND scored_on < ?%s
                  GROUP BY user_id
                ) latest ON latest.user_id=baseline.user_id AND latest.latest=baseline.scored_on
                """.formatted(scoreColumn, table, table, marks, mockFilter);
        Double current = jdbc.query(currentSql, (rs, row) -> nullableDouble(rs, 1), ids)
                .stream().filter(value -> value != null).findFirst().orElse(null);
        Double baseline = jdbc.query(baselineSql, (rs, row) -> nullableDouble(rs, 1),
                        merge(ids, new Object[]{windowStart.atStartOfDay()}))
                .stream().filter(value -> value != null).findFirst().orElse(null);
        Integer sample = jdbc.queryForObject("""
                        SELECT COUNT(DISTINCT user_id) FROM %s
                        WHERE user_id IN (%s) AND scored_on < ?%s
                        """.formatted(table, marks, mockFilter),
                Integer.class,
                merge(ids, new Object[]{windowStart.atStartOfDay()}));
        if (current == null || baseline == null || sample == null || sample < 1) {
            return new Outcome(name, "数据不足", meaning + ";样本不足");
        }
        double delta = current - baseline;
        return new Outcome(name, signed(delta, 1) + " 分",
                "近 " + window + " 日人群均值变化 · 样本 " + sample + " 人(" + meaning + ")");
    }

    private List<Factor> factors(List<String> userIds, boolean mockPresent) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        String marks = placeholders(userIds.size());
        Object[] ids = userIds.toArray();
        //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调放开演练快照,生产默认排除------------
        String rdiMockFilter = includeMock ? "" : " AND snapshot.is_mock=0";
        String latestMockFilter = includeMock ? "" : " AND latest.is_mock=0";
        //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】本地联调放开演练快照,生产默认排除------------
        List<Factor> factors = jdbc.query("""
                        SELECT contribution.factor_code, contribution.domain_code,
                               AVG(contribution.final_points) AS mean_points
                        FROM rehealth_rdi_contribution contribution
                        WHERE contribution.snapshot_id IN (
                          SELECT snapshot.id
                          FROM rehealth_rdi_daily_snapshot snapshot
                          WHERE snapshot.user_id IN (%s)%s
                            AND snapshot.scored_on = (
                              SELECT MAX(latest.scored_on)
                              FROM rehealth_rdi_daily_snapshot latest
                              WHERE latest.user_id=snapshot.user_id%s
                            )
                        )
                        GROUP BY contribution.factor_code, contribution.domain_code
                        ORDER BY ABS(AVG(contribution.final_points)) DESC, contribution.factor_code
                        LIMIT %d
                        """.formatted(marks, rdiMockFilter, latestMockFilter, MAX_FACTOR_ROWS),
                (rs, row) -> new Factor(
                        factorName(rs.getString(1)),
                        signed(nullableDoubleOrZero(rs, 3), 2),
                        "领域:" + domainName(rs.getString(2)) + " · 人群均值贡献分,负值表示降低风险负荷"
                ),
                ids);
        if (factors.isEmpty()) {
            return List.of(new Factor("暂无贡献数据", "—", includeMock
                    ? "范围内暂无 RDI 贡献数据(需 RDI 每日快照与结构化贡献)"
                    : (mockPresent
                    ? "当前范围内 RDI 快照均为演练数据,按口径不纳入贡献统计;真实快照上传后自动填充"
                    : "范围内暂无满足口径的 RDI 贡献数据(需非演练 RDI 每日快照与结构化贡献)")));
        }
        return factors;
    }

    private static List<Suggestion> suggestions() {
        return List.of(
                new Suggestion("血压偏高", "家庭血压连续记录 3-7 天,医生确认用药和复测时间", "有连续血压记录 + 医生确认"),
                new Suggestion("LDL-C 偏高", "补齐有效期内血脂,不用过期数据直接下药物结论", "最新血脂可追溯 + 方案已复核"),
                new Suggestion("睡眠不规律", "固定入睡时间,连续记录 7 天睡眠时长和效率", "睡眠记录连续 7 天"),
                new Suggestion("运动不足 / 久坐", "每周至少 4 天中等强度活动,记录步数和运动天数", "运动天数和步数有记录"),
                new Suggestion("用药执行不清", "确认实际有没有吃,不能只看处方是否开出", "患者确认 + 用药记录闭合"),
                new Suggestion("数据缺口", "先补设备同步和日常记录,再判断是否真的恶化", "数据连续且来源明确")
        );
    }

    private static String levelLabel(String level) {
        return switch (level) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            case "low" -> "低风险";
            default -> level;
        };
    }

    private static int levelRank(String level) {
        return switch (level) {
            case "low" -> 0;
            case "medium" -> 1;
            case "high" -> 2;
            default -> -1;
        };
    }

    private static String factorName(String code) {
        if (code == null) return "未知因素";
        String value = code.toLowerCase(Locale.ROOT);
        //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】补齐 RDI 因素码与领域码中文映射,报告不出现英文------------
        if (value.startsWith("diet_")) {
            return switch (value.substring("diet_".length())) {
                case "breakfast" -> "早餐结构";
                case "lunch" -> "午餐结构";
                case "dinner" -> "晚餐结构";
                case "snack" -> "加餐结构";
                default -> "饮食·" + value.substring("diet_".length());
            };
        }
        return switch (value) {
            case "steps" -> "步数";
            case "activity", "exercise" -> "运动天数";
            case "verified_activity_minutes" -> "有效活动时长";
            case "sleep" -> "睡眠";
            case "sleep_duration" -> "睡眠时长";
            case "sleep_efficiency" -> "睡眠效率";
            case "sleep_regularity" -> "睡眠规律";
            case "sleep_consistency_reward" -> "睡眠规律奖励";
            case "nocturnal_hrv" -> "夜间 HRV";
            case "hrv_personal_trend" -> "HRV 个人趋势";
            case "hrv" -> "HRV";
            case "resting_hr" -> "静息心率";
            case "sedentary" -> "久坐时长";
            case "sbp", "dbp", "blood_pressure", "bp" -> "血压";
            case "glucose", "blood_glucose" -> "空腹血糖";
            case "weight", "bmi" -> "体重/BMI";
            case "smoking", "smoke" -> "吸烟";
            case "alcohol", "drinking" -> "饮酒";
            case "hr", "heart_rate" -> "心率";
            case "spo2", "blood_oxygen" -> "血氧";
            case "ldl", "ldl_c" -> "LDL-C";
            default -> code;
        };
        //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】补齐 RDI 因素码与领域码中文映射,报告不出现英文------------
    }

    private static String domainName(String code) {
        if (code == null) return "未分类";
        String value = code.toLowerCase(Locale.ROOT);
        //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】补齐 RDI 因素码与领域码中文映射,报告不出现英文------------
        return switch (value) {
            case "exercise", "activity", "sport" -> "运动";
            case "sleep" -> "睡眠";
            case "recovery" -> "恢复";
            case "diet", "nutrition", "food" -> "饮食";
            case "lab", "laboratory" -> "实验室检查";
            case "blood_pressure", "bp", "pressure" -> "血压";
            case "blood_glucose", "glucose", "metabolic" -> "代谢";
            case "weight", "bmi", "body" -> "体重";
            case "smoking", "smoke" -> "吸烟";
            case "alcohol", "drinking" -> "饮酒";
            case "heart_rate", "hr", "vital" -> "生命体征";
            default -> code;
        };
        //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】补齐 RDI 因素码与领域码中文映射,报告不出现英文------------
    }

    private static String share(int count, int total) {
        if (total <= 0) return "—";
        return Math.round(count * 100.0 / total) + "%";
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static String signed(double value, int digits) {
        return String.format(Locale.ROOT, "%+." + digits + "f", value);
    }

    private static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private static Object[] merge(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static Double nullableDouble(ResultSet rs, int column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.doubleValue();
    }

    private static double nullableDoubleOrZero(ResultSet rs, int column) throws SQLException {
        Double value = nullableDouble(rs, column);
        return value == null ? 0.0 : value;
    }
}
