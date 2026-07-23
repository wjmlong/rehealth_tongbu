---
slug: complete-rehealth-backend-model-service
status: awaiting-approval
intent: unclear
review_required: true
plan_path: .omo/plans/complete-rehealth-backend-model-service.md
plan_sha256: 8b78bbc9c415fc94653f60c9eafdfff0e412ac04edd600fd034097cb3fd1f9b1
review_round_id: r20260723100000-b7c4e219
round_status: active
pending-action: review .omo/plans/complete-rehealth-backend-model-service.md
review:
  momus:
    status: in_flight
    workspace_root: D:/rehealthAI
    runtime_home: null
    target: .omo/plans/complete-rehealth-backend-model-service.md
    round_id: r20260723100000-b7c4e219
    plan_sha256: 8b78bbc9c415fc94653f60c9eafdfff0e412ac04edd600fd034097cb3fd1f9b1
    launch_id: momus-r20260723100000-b7c4e219-l1
    session: /root/review_backend_model_plan_r3
    result: null
  independent:
    status: in_flight
    workspace_root: /tmp/omo-ulw-review-r20260723100000-b7c4e219/workspace
    runtime_home: /tmp/omo-ulw-review-r20260723100000-b7c4e219/codex-home
    target: .omo/plans/complete-rehealth-backend-model-service.md
    round_id: r20260723100000-b7c4e219
    plan_sha256: 8b78bbc9c415fc94653f60c9eafdfff0e412ac04edd600fd034097cb3fd1f9b1
    launch_id: independent-r20260723100000-b7c4e219-l1
    session: exec_session_23667
    result: null
approach: Evolve the already-working authenticated pipeline into API Gateway + JeecgBoot + independent Device Service + Kafka + MySQL software_db + PostgreSQL/TimescaleDB hardware_db + model-service/PIAS, preserve mock attribution only as an explicit non-production demo switch, and complete the lower-priority doctor/institution/insurance/follow-up/health-task domains with Jeecg RBAC, menus and maintainable low-code administration surfaces.
---

# Draft: complete-rehealth-backend-model-service

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->

| id | outcome | status | evidence path |
|---|---|---|---|
| A | Canonical production profile starts API Gateway, JeecgBoot, independent Device Service, Kafka, software MySQL, TimescaleDB, model-service and PIAS with fail-closed health/readiness and secret injection | active | `backend/docker-compose*.yml`; `backend/jeecg-boot/.../ReHealthMobileServiceImpl.java` |
| B | software_db gains complete doctor, institution, insurance, follow-up and health-task domains plus authenticated web-admin APIs | active | `backend/jeecg-boot/.../db/software/mysql/V1__create_rehealth_software_tables.sql` |
| C | A standalone Device Service owns telemetry validation, idempotency, TimescaleDB persistence, recent queries, reconciliation and Kafka outbox/events | active | current source: `backend/jeecg-boot/.../ingest/`; target: `backend/device-service/` |
| D | Model authority is explicit: model-service owns CVD risk/intervention/registry; real PIAS is production attribution; model-service mock attribution survives only behind a visible demo-only switch | active | `model-service/app/main.py`; `model-service/app/attribution.py`; `HttpModelServiceClient.java` |
| E | Security, RBAC, audit, observability, backup/restore and capacity gates are production-verifiable | active | `ACCEPTANCE_REVIEW_2026-07-16.md`; `outputs/demo_ui_live_api_integration_report.md` |
| F | Jeecg RBAC, menus and low-code/admin surfaces cover doctor workbench, organizations, insurance programs, follow-ups, health tasks, device operations, risk stratification and audit views | active | `backend/jeecgboot-vue3/`; `jeecg-module-rehealth` |
| G | Existing MySQL hardware data is migrated additively to TimescaleDB with shadow verification and rollback; no destructive cutover | active | `.../db/hardware/mysql/V1__create_hardware_telemetry_tables.sql`; `backend/docs/DATABASE_SPLIT_ARCHITECTURE.md` |

## Open assumptions (announced defaults)
<!-- Intent is UNCLEAR: research resolves ambiguity, defaults are adopted (not asked), and each is surfaced in the plan's human TL;DR for veto. -->
<!-- assumption | adopted default | rationale | reversible? -->

| assumption | adopted default | rationale | reversible? |
|---|---|---|---|
| Service shape | Extract telemetry ingestion/query into `backend/device-service`, while JeecgBoot retains users, business workflows and model orchestration | Explicit user decision; keeps the hardware write path independently scalable | yes |
| Data stores | Keep software_db on MySQL and move hardware_db to PostgreSQL + TimescaleDB | Explicit user decision; hypertables and retention fit wearable time-series workloads | yes |
| Kafka role | Use a transactional TimescaleDB outbox to publish versioned telemetry lifecycle events to Kafka; HTTP ingestion remains durable before success is returned | Prevents dual-write loss while making Kafka useful for downstream consumers | yes |
| Gateway | Use the existing Jeecg Cloud Gateway as public routing boundary; preserve existing Android URLs and `X-Access-Token` contract | Explicit target architecture plus backward compatibility | yes |
| Low-code boundary | Use Jeecg Online/codegen for admin CRUD, forms, lists and reports only | Dedupe, ownership, telemetry writes, model calls and clinical rules require reviewed handwritten code | yes |
| Attribution | Production defaults to real PIAS; retain model-service mock attribution behind `demo_mock`, visibly marked and forbidden in production profile | Explicit user decision; preserves demos without allowing silent clinical fallback | yes |
| Health agent | Add a backend-authenticated orchestration boundary; never expose provider keys to Android and never claim diagnosis | Repository rules and release privacy gate require server-side secrets and conservative output | yes |
| Compatibility | Additive migrations and backward-compatible mobile contracts; no destructive rewrite | Existing Android and staging pipeline are working | yes |
| UI | No Android Compose UI changes in this backend/model-service program | User explicitly requires preserving Demo UI | yes |
| Production mocks | Fail closed or return explicit degraded metadata; never silently substitute mock results | Medical traceability requirement | yes |
| Future web backends | Implement the backend domains, RBAC/menu seeds and low-code/admin surfaces in the same plan, after the ingestion/platform foundation | Latest explicit user instruction requires all confirmed gaps to be completed | yes |

## Findings (cited - path:lines)

- The pasted assessment is stale for the core pipeline. Authenticated mobile routes already cover profile, interview, device bind, telemetry upload/recent query, risk, intervention, feedback and attribution (`ReHealthMobileController.java:62-183`).
- `software_db` already has a real JDBC repository selected when `rehealth.software-db.enabled=true` (`JdbcSoftwareDbReHealthBusinessRepository.java:34-52`) and tables for profile, interview, binding, features, risk, interventions, feedback, attribution and model audit (`V1__create_rehealth_software_tables.sql:1-133`).
- `hardware_db` already has upload-batch, measurement, sleep, activity, signal metadata and data-quality tables (`V1__create_hardware_telemetry_tables.sql:1-107`), plus direct JDBC writer/query implementations.
- Real staging evidence covers authenticated Jeecg login, dual MySQL persistence, restart readback, batch idempotency, real CatBoost `isMock=false`, intervention/feedback and real PIAS (`outputs/demo_ui_live_api_integration_report.md:243-254`).
- Production configuration is not closed: when `rehealth.software-db.enabled` is missing/false, the no-op E1 fallback is still selected and reports persistence pending (`E1PendingSoftwareDbReHealthBusinessRepository.java:19-84`). Canonical backend compose files do not include model-service, PIAS or explicit ReHealth dual-database services/configuration.
- The current ReHealth Java module exposes only a mobile controller; no ReHealth doctor, institution, insurance, follow-up, health-task or operations controller/service packages were found under `jeecg-module-rehealth/src/main`.
- Backend already routes attribution to the real PIAS v2 endpoint (`HttpModelServiceClient.java:67-86`). In contrast, model-service still exposes `/v1/cvd/attribution/individual` (`model-service/app/main.py:74-77`) whose implementation labels all outputs `cvd-mock-rules-v1` and only computes a simple trend/adherence mean (`model-service/app/attribution.py:9-35`).
- Model-service already owns a real/model-fallback scorer, intervention generator and registry/trace health metadata (`model-service/app/main.py:31-71`); the missing work is operational hardening and responsibility cleanup, not rebuilding CatBoost integration.
- Remaining deployment evidence gaps are concurrency/load, failover, backup/restore and production capacity testing (`ACCEPTANCE_REVIEW_2026-07-16.md:422-428`). MuMu and physical MR11 QA remain separate Android/hardware release gates (`outputs/demo_ui_live_api_integration_report.md:253-254`).
- Metis gap analysis required: Kafka-degraded ingest readiness, device-binding authorization, hardened PIAS production entrypoint, a real Kafka projection consumer, Timescale layout/retention, pre-cutover shadow migration, post-cutover authority-safe rollback, complete insurance lifecycles, explicit RPO/RTO and overall-product physical-MR11 release authority. All are incorporated in the complete plan.

## Decisions (with rationale)

1. Plan from the current verified state, not the older architecture description; do not redo Android networking, dual-database persistence or CatBoost/PIAS wiring.
2. First make production configuration fail closed and reproducible, because a default-disabled database can silently activate the pending/no-op repository despite working code.
3. Extract the existing validated telemetry logic rather than rewriting it; preserve the Android public contract through gateway routing and contract tests.
4. Make TimescaleDB the Device Service system of record, migrate MySQL hardware data via backfill + shadow comparison, and use a transactional outbox for Kafka delivery.
5. Keep duplicate mock attribution only as an explicit `demo_mock` path with response provenance; production startup must reject that mode.
6. Treat doctor/insurance conclusions as decision-support only, with tenant/user scoping, audit trails and conservative medical language.
7. Implement future web domains vertically (additive schema -> domain/service -> RBAC API -> menu/low-code page -> tests), after the device/data foundation stabilizes.
8. Keep Device Service ingest-ready during Kafka outages; Timescale data + Outbox are the durability boundary and Jeecg owns the idempotent Kafka projection.
9. Dark-deploy/backfill/shadow-compare before Gateway cutover; after cutover, Timescale remains authoritative even if application binaries roll back.
10. Treat the final gate as backend/model platform approval; physical MR11 evidence or an explicit release-authority waiver is still required for overall MVP release.

## Scope IN

- Reproducible production/staging deployment topology and readiness gates.
- Standalone Device Service, Jeecg Gateway routing, service authentication, Kafka topics/DLQ/outbox and TimescaleDB schema/migration/cutover.
- hardware operational support: upload failures, retry/reconciliation, quality review, retention and recent/device queries.
- Complete doctor, institution, insurance, follow-up and health-task backend domains, tenant-scoped APIs, Jeecg permissions/menu seeds and low-code/admin pages.
- Device fleet operations, failed-upload reconciliation, data-quality review, risk stratification and model-audit administration.
- Health-agent backend entry with authenticated context retrieval, conservative output policy, provider-secret isolation and audit/rate limits.
- Load/capacity tests, backup/restore drill, metrics/tracing/log redaction and release gates.
- model-service/PiAS authority cleanup, model registry/version visibility, health-agent backend boundary, timeouts/error taxonomy and observability.
- Additive migrations, unit/integration/E2E/load/backup-restore validation and documentation.
- Android API contract regression only; no Compose/UI work.

## Scope OUT (Must NOT have)

- No Android UI redesign or replacement of the preserved Demo Compose pages.
- No direct Android access to databases, CatBoost, PIAS, LLM providers or provider secrets.
- No ClickHouse migration and no Kafka-driven risk inference coupling.
- No destructive schema reset, history rewrite, embedded JWT/API key or production PII/raw-health logging.
- No low-code implementation of BLE parsing, idempotency, model inference/orchestration or clinical safety rules.
- No claim that MuMu/physical MR11 or production deployment was tested unless actually executed.

## Open questions

- None blocking planning. The defaults above are explicitly reversible and can be vetoed at approval.

## Approval gate
status: approved-plan-in-review
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
