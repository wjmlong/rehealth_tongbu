# ADR-003: Model, attribution, and health-agent authority

Status: Accepted, version 2, 2026-07-31.

`model-service` owns production CVD risk scoring and SHAP. Its legacy
intervention and health-agent endpoints remain compatibility paths.
`rehealth-algorithms` remains training and research except for its separately
hardened PIAS production app. PIAS is the only production attribution engine.
Jeecg owns authenticated orchestration, persistence, the configurable
LangChain4j health chat, and the explicit LangChain4j structured wellness-plan
generation path. The latter is not CVD scoring, diagnosis, medication logic or
causal attribution.

Attribution mode is explicit: `pias` requires the hardened PIAS app and becomes unavailable when PIAS is unavailable; `demo_mock` is visibly mock and is forbidden in production. There is no silent fallback between modes. Provenance, mode, model version, readiness, and unavailable reason are returned and audited.

The health agent receives the minimum tenant-scoped context selected by Jeecg, never cross-user history, raw telemetry, credentials, or hidden prompts. It resists prompt injection, rate limits per authenticated subject, and returns a conservative status when a provider is unavailable. Output must include a medical disclaimer, must not diagnose, prescribe, approve insurance/clinical actions, or claim to replace a clinician. Safety filtering is authoritative even when a provider returns unsafe prose.

Every personalized-plan generation reloads the authenticated user's profile,
latest interview and risk from `software_db`, then requests a bounded
tenant/user/local-day activity, sleep, measurement, diet and recent-change
summary from Device Service. Client-provided health context is ignored.
Device Service remains the TimescaleDB owner; Jeecg never queries it directly.
Generation is fail-closed and persists only validated structured JSON with
1–5 conservative actions and explicit context freshness.
