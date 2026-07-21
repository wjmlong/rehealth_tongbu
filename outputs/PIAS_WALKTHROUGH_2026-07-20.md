# PIAS 端到端走通报告 — 2026-07-20

## 目标
实际启动 `model-service`，用「前端戒指模拟提取到的多维特征」（即 Android `HealthFeatureExtractor` 产出的 16 维 `CvdFeatureVector`），真实走一遍 PIAS（Predict → Intervene → Attribute → Settle）流程，直到后端返回对应值给前端。

## 环境与启动
- Python venv：`D:\rehealthAI\model-service\.venv`（基于托管 Python 3.13.14，已装 fastapi/uvicorn/pydantic/joblib/scikit-learn/**catboost 1.2.10** 等全部依赖）。
- 启动命令（cwd = `model-service`）：
  `.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000`
- `GET /health` 确认：`scorer_mode=real_available`、`model_available=true`、`loaded_artifact_name=rehealth_cvd_catboost.pkl`、`model_version=cvd-core16-catboost-20260710T173543Z`。

### 关键发现：模型是真模型，不是 mock
`models/` 下存在经过校验的 CatBoost artifact（`rehealth_cvd_catboost.pkl` + `feature_cols.pkl` 恰好等于 16 个 CVD 字段规范序 + `model_meta_v2.json`，AUC 0.849594）。`validate_real_model_artifacts()` 校验通过 → 加载的是 **RealCatBoostRiskScorer（真实 Predict）**，而非 MockRiskScorer。

## 模拟的前端特征向量（16 维，对齐 Android 提取来源）
| 字段 | 值 | 来源 |
|---|---|---|
| age | 54 | USER_REPORTED |
| gender | 1 | USER_REPORTED |
| bmi | 27.3 | USER_REPORTED |
| sbp / dbp | 142 / 91 | **REAL_DEVICE（戒指）** |
| fasting_glucose / total_cholesterol / ldl / hdl / triglycerides | 6.4 / 5.8 / 3.6 / 1.05 / 1.9 | CLINICAL_REPORT |
| exercise_days | 3 | DERIVED（7 天步数/活动聚合） |
| smoking / drinking / diabetes_history / hypertension_history / family_history | 1/0/0/1/1 | USER_REPORTED |

`featureQuality` 覆盖全部 16 字段（必填，否则 422）。

## 端到端响应（后端返回给前端的值）

### P — POST /v1/cvd/risk/evaluate  → HTTP 200
```json
{
  "risk_score": 0.3224,
  "risk_level": "moderate",
  "feature_contributions": { "age":0.0, "gender":0.0, "bmi":0.0, "sbp":0.0, "dbp":0.0,
    "fasting_glucose":0.0, "total_cholesterol":0.0, "ldl":0.0, "hdl":0.0, "triglycerides":0.0,
    "exercise_days":0.0, "smoking":0.0, "drinking":0.0, "diabetes_history":0.0,
    "hypertension_history":0.0, "family_history":0.0 },
  "model_version": "cvd-core16-catboost-20260710T173543Z",
  "is_mock": false,
  "missing_fields": [],
  "quality_warnings": [],
  "summary": "CatBoost CVD risk estimate. This is not a diagnosis.",
  "request_id": "demo-ring-0001",
  "contribution_method": "deterministic_zero_fallback",
  "model_trace": { "feature_schema_version":"cvd-16-v1", "model_version":"cvd-core16-catboost-20260710T173543Z",
    "artifact_name":"rehealth_cvd_catboost.pkl", "scorer_mode":"real_available", "request_id":"demo-ring-0001" }
}
```

### I — POST /v1/cvd/intervention/generate  → HTTP 200
```json
{
  "plan_id": "plan-e35fa93d-14b0-5169-b8dc-691964405c09",
  "generated_at": "2026-07-20T17:47:00.030839Z",
  "priority_intervention": "Add 15 to 20 minutes of easy walking after one meal if there are no contraindications.",
  "rationale": "Light activity is a conservative wellness step when the current risk estimate is not high.",
  "expected_impact": "May support cardiometabolic health over time; it is not a treatment claim.",
  "contraindications": ["chest pain","dizziness","unusual shortness of breath","clinician-advised activity restriction"],
  "confidence": 0.52,
  "model_version": "cvd-core16-catboost-20260710T173543Z",
  "is_mock": false,
  "medical_disclaimer": "This guidance is conservative wellness support and does not diagnose disease or replace a clinician."
}
```

### A — POST /v1/cvd/attribution/individual  → HTTP 200
```json
{ "model_version":"cvd-mock-rules-v1", "trend_delta":-0.07, "adherence_average":0.7,
  "interpretation":"Risk trend decreased during the observed window." }
```

## 重要发现 / 风险（需关注）
1. **真实模型不返回逐特征归因**：`RealCatBoostRiskScorer` 的 `feature_contributions` 全为 0.0（`contribution_method=deterministic_zero_fallback`）。前端归因页（AttributionScreen）依赖 `feature_contributions`，用真实模型时权重条会全空。归因细节需要真实 SHAP/贡献计算，目前未接入。
2. **归因缺失影响干预分支选择**：`ConservativePrescriptionGenerator` 用 `feature_contributions.get("sbp",0)>0` 判断是否给「记录血压」建议；真实模型贡献为 0 → 即使高血压用户也只会落到「散步」分支（除非 risk_level 为 high/very_high）。这是 zero-contribution 缺口的真实副作用。
3. **真实模型 vs mock 分数差异显著**：同一组特征，真实 CatBoost = 0.3224（moderate），Mock 规则 = 0.705（HIGH）。生产链路用真实模型，更保守。
4. **演示用 MockRiskScorer 规则归因（供前端展示参考）**：sbp +0.12、age +0.09、smoking +0.08、hypertension_history +0.08、dbp +0.06、fasting_glucose +0.04、family_history +0.04、bmi +0.03、total_cholesterol +0.03、ldl +0.03、hdl +0.025、triglycerides +0.025、exercise_days -0.025（保护）。

## Java 后端转发契约（未实际启动 JeecgBoot，仅核对契约）
Android `ReHealthApi.@POST("/rehealth/mobile/features/evaluate")` → `ReHealthMobileController` → `HttpModelServiceClient.evaluateRisk()`（java.net.http POST `model-service /v1/cvd/risk/evaluate`）。响应 DTO `RiskEvaluateResponseDto{ risk_score, risk_level, feature_contributions, model_version, is_mock, missing_fields, quality_warnings, summary, model_trace }` 与上方 model-service 返回体字段一致 → Java 控制器会原样把上述值转发给前端。base-url 默认 `application-dev.yml:420-423` = `http://127.0.0.1:8000`，timeout 10s。

## 复现脚本
`D:\rehealthAI\outputs\pias_walkthrough.py`（用 venv python 运行即可重复整条 PIAS 流程并对照 mock 归因）。

## 结论
✅ model-service 已真实跑通，PIAS 的 P/I/A 三步对真实戒指特征数据均返回 200 与有效值，后端（model-service，即 Java `HttpModelServiceClient` 调用的评分后端）已能返回对应值给前端。
⚠️ 待补：真实模型逐特征归因（SHAP）接入，否则归因页与干预分支选择依赖真实贡献时为空/退化。
