# ADR-003: Model, attribution, and health-agent authority

Status: Accepted, version 1, 2026-07-23.

`model-service` owns production CVD risk scoring, intervention generation, the health agent, and Demo Mock attribution. `rehealth-algorithms` remains training and research except for its separately hardened PIAS production app. PIAS is the only production attribution engine. Jeecg owns authenticated orchestration and persistence, not inference.

Attribution mode is explicit: `pias` requires the hardened PIAS app and becomes unavailable when PIAS is unavailable; `demo_mock` is visibly mock and is forbidden in production. There is no silent fallback between modes. Provenance, mode, model version, readiness, and unavailable reason are returned and audited.

The health agent receives the minimum tenant-scoped context selected by Jeecg, never cross-user history, raw telemetry, credentials, or hidden prompts. It resists prompt injection, rate limits per authenticated subject, and returns a conservative status when a provider is unavailable. Output must include a medical disclaimer, must not diagnose, prescribe, approve insurance/clinical actions, or claim to replace a clinician. Safety filtering is authoritative even when a provider returns unsafe prose.
