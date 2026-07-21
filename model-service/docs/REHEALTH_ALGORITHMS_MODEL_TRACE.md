# ReHealth Android Model Trace

Status: F2e docs-driven artifact alias policy.

This note records the model artifact evidence found in `rehealth-algorithms` docs and how model-service uses it without changing the Android C1 16-feature API contract.

## Source Evidence

Reviewed files:

- `D:\rehealthAI\rehealth-algorithms\docs\工程日记_CVD模型训练.md`
- `D:\rehealthAI\rehealth-algorithms\docs\V1_工程实施版_端云协同系统开发框架说明书.md`
- `D:\rehealthAI\rehealth-algorithms\docs\CTO技术决策对话记录_20260525.md`

Key evidence:

- `工程日记_CVD模型训练.md` line 31 lists V2 as a CatBoost single-model baseline with test AUC `~0.767`.
- `工程日记_CVD模型训练.md` line 36 lists V8 as `0.8615` test AUC and `0.8671` CV AUC.
- `工程日记_CVD模型训练.md` lines 117-125 describe the V8 feature construction as 97 features.
- `工程日记_CVD模型训练.md` line 229 says the full flow used `rehealth_v2_final.pkl` with 16 features and AUC `~0.767` as the V1 production model.
- `工程日记_CVD模型训练.md` line 282 flags old `risk_scorer.py` behavior that hardcoded `model_auc: 0.847`; the note says this was fixed by reading/passing metadata.
- `工程日记_CVD模型训练.md` line 283 says V2 AUC was low at `0.767` and needs 16-feature retraining rather than short-term tuning.
- `V1_工程实施版_端云协同系统开发框架说明书.md` lines 386 and 400 mention `rehealth_v2_final.pkl` with AUC `0.847`.
- `V1_工程实施版_端云协同系统开发框架说明书.md` line 439 shows an example response with hardcoded `model_auc: 0.847`.
- `CTO技术决策对话记录_20260525.md` lines 78 and 130 mention CatBoost risk scoring and the old AUC `0.847` trace.

## Artifact Names

Normalized model-service artifact name:

```text
models/rehealth_cvd_catboost.pkl
```

Historical artifact alias from `rehealth-algorithms` docs:

```text
models/rehealth_v2_final.pkl
```

The loader searches in this order:

```text
models/rehealth_cvd_catboost.pkl
models/rehealth_v2_final.pkl
```

The preferred deployment name remains `rehealth_cvd_catboost.pkl`. The historical alias exists only to let a reviewed V2 artifact be validated before the artifact handoff is normalized.

## Feature Order

The current model-service contract is the Android C1 CVD 16-field order:

```text
age
gender
bmi
sbp
dbp
fasting_glucose
total_cholesterol
ldl
hdl
triglycerides
exercise_days
smoking
drinking
diabetes_history
hypertension_history
family_history
```

Feature-order artifact search order:

```text
models/feature_cols.pkl
models/feature_cols_v2.pkl
models/cvd_features.json
```

If no feature-order artifact exists, model-service may report the built-in C1 feature order for diagnostics, but it must not enable real scoring. Real scoring requires a local feature-order artifact that validates exactly against the C1 16-field order.

## AUC Conflict

The docs contain conflicting AUC traces:

- `0.847` appears in older architecture and CTO docs and in a historical hardcoded response example.
- `~0.767` is the later V2 16-feature MVP trace for `rehealth_v2_final.pkl`.
- `0.8615` is the V8 97-feature research trace, not the current Android C1 16-feature model-service contract.

F2 policy:

- Do not hardcode `model_auc=0.847`.
- Omit AUC from runtime responses unless verified metadata provides it.
- If metadata is absent and a real model loads, use `model_version=cvd-catboost-v2` and report the actual loaded artifact filename through health diagnostics.

## Final F2 Artifact Policy

- Do not train a model in model-service.
- Do not copy model binaries into git.
- Do not return `is_mock=false` unless a real local artifact loads and produces a prediction.
- Keep Android C1 as the stable request contract.
- Prefer `models/rehealth_cvd_catboost.pkl`; accept `models/rehealth_v2_final.pkl` as a historical alias.
- Prefer `models/feature_cols.pkl`; accept `models/feature_cols_v2.pkl` and `models/cvd_features.json` as feature-order aliases.
- Accept optional metadata from `models/model_meta_v2.json` or `models/model_metadata.json`.
