# complete-rehealth-backend-model-service - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** 一套可部署、可回滚、可审计的云端健康平台：网关分流业务与设备请求，独立设备服务把戒指数据可靠写入 TimescaleDB 并通过 Kafka 分发，JeecgBoot 承载业务后台，模型服务与真实 PIAS/健康智能体形成清晰边界。同时交付医生、机构、保险、随访、健康任务、设备运维、风险与模型审计后台。

**Why this approach:** 保留已经真实跑通的 Android、Jeecg、CatBoost 和 PIAS 契约，只抽离硬件写入职责；上传事务先持久化数据与 Outbox，再异步发布 Kafka，因此 Kafka 故障不会丢失已接收数据。真实 PIAS 始终是生产归因源，Mock 只允许显式 Demo 模式。

**What it will NOT do:** 不修改 Android Demo Compose UI；不让 Android 直连数据库、Kafka、模型或供应商密钥；不把 Mock 静默伪装成真实医学结果；不破坏或删除现有 MySQL 硬件数据。

**Effort:** XL
**Risk:** High - 涉及服务拆分、数据库迁移、消息一致性、认证边界和医疗数据治理，但采用兼容路由、影子校验与可回滚切换降低风险。
**Decisions I made for you:** software_db 保持 MySQL；hardware_db 使用 PostgreSQL 17 + TimescaleDB；设备上传采用“Timescale 事务 + Outbox + Kafka”而非同步依赖 Kafka；Kafka 使用 KRaft；公网只暴露 Gateway；医生等后台按完整纵向功能实施但排在数据平台之后；生产禁止 Demo Mock，开发演示必须显式标记。

Your next move: 双重高精度审查通过后，使用 `$start-work` 开始执行。完整执行细节如下。

---

> TL;DR (machine): XL/high-risk architecture delivery: Gateway + independent Device Service + TimescaleDB + Kafka outbox, PIAS/demo attribution modes, health-agent backend, full Jeecg operations domains/admin, and production QA gates without Android UI changes.

## Scope
### Must have
- Preserve current Android public paths and response compatibility for `/rehealth/mobile/measurements/batch`, `/measurements/recent`, risk, intervention and attribution.
- Add a Spring Boot 3.5.5 / Java 17 Device Service under `backend/device-service` owning telemetry validation, idempotency, TimescaleDB writes, recent queries, reconciliation and health endpoints.
- Route public traffic through the existing Jeecg Cloud Gateway; expose no Jeecg, Device Service, model-service, PIAS, Kafka or database port publicly in the production compose profile.
- Keep `software_db` on MySQL; migrate `hardware_db` additively from MySQL to PostgreSQL 17 + TimescaleDB with backfill, shadow comparison, cutover and rollback.
- Add Kafka 4.3.1 KRaft deployment, versioned schemas/topics, transactional outbox publishing, retry/DLQ and idempotent consumers.
- Keep real PIAS as the default/only production attribution authority. Keep model-service Mock attribution only as explicit `demo_mock`, with `is_mock=true`, visible provenance and production startup rejection.
- Add authenticated, rate-limited health-agent backend endpoints with server-held provider secrets, curated health context, conservative medical policy and audit metadata.
- Implement doctor, institution, insurance program, follow-up and health-task domains in `software_db`, tenant-scoped APIs and Jeecg admin pages.
- Implement device fleet, failed-upload reconciliation, data-quality review, risk stratification and model-call audit administration.
- Add RBAC permissions/menu seeds, observability, log redaction, load tests, backup/restore drill and release gates.
- Productionize PIAS behind an internal-only allowlisted app; do not expose research, insurance-demo or test routers.
- Preserve local-first Android collection and upload queue behavior; no Compose UI edits.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- Must not move CatBoost, SHAP, PIAS or LLM inference into Android or Jeecg Java.
- Must not make Kafka availability a prerequisite for returning success after telemetry is durably stored; Outbox remains in the same Timescale transaction.
- Must not dual-write TimescaleDB and Kafka without an Outbox, delete MySQL source rows, or perform destructive migration/reset.
- Must not accept `userId`, tenant, clinician or organization ownership from a client body; identity and scope come from validated authentication/authorization.
- Must not silently fall back from PIAS to Mock, enable `demo_mock` in production, or omit Mock provenance from responses/audit.
- Must not log raw health payloads, access tokens, phone numbers, provider prompts/responses or direct identifiers in production.
- Must not use Jeecg low-code for ingest, dedupe, outbox, model orchestration or clinical safety rules; only administrative CRUD/forms/lists/reports use generated/low-code surfaces.
- Must not modify Android Compose pages, navigation, attribution animation or Demo UI layout.
- Must not introduce ClickHouse, a service mesh or additional microservices beyond the approved Device Service.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after for migrations/CRUD/deployment; TDD for authentication ownership, idempotency, Outbox retry, demo-mode rejection and migration cutover. Frameworks: JUnit 5 + Spring Boot Test + Testcontainers (MySQL/PostgreSQL/TimescaleDB/Kafka), Pytest/FastAPI TestClient, Vue Vitest where present, Playwright for admin UI, k6 for load.
- Every task captures command, exit code and assertion-bearing output under `<attemptDir>/task-<N>-complete-rehealth-backend-model-service/`; screenshots alone never prove backend correctness.
- Integration containers use ephemeral credentials and synthetic records labelled `qa_*`; teardown verifies no QA rows/tokens remain in tracked files or persistent staging data.
- Final E2E replays normalized MR11 capture fixtures only as labelled software validation data; it must never be described as current-user physiology.
- Each todo's `QA scenarios` invokes its exact `Tn` command below. The implementation for that todo must create the referenced runner/fixture before invoking it; exit 0 plus the named machine-readable assertion artifact is required. A log containing only text such as `PASS` without assertion JSON/JUnit/Playwright/k6 results is rejected.

| Gate | Happy command and fixture | Failure command and required assertion |
| --- | --- | --- |
| T1 | `python backend/contracts/scripts/validate_contracts.py --all --fixtures backend/contracts/fixtures/valid --report <attemptDir>/task-1-complete-rehealth-backend-model-service/contracts.json` | `python backend/contracts/scripts/validate_contracts.py --fixtures backend/contracts/fixtures/forbidden --expect-rejected token,raw_signal,client_owner`; report lists all three rejected reasons |
| T2 | `python backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production --report <attemptDir>/task-2-complete-rehealth-backend-model-service/topology.json` | `python backend/qa/rehealth_stack_gate.py topology-failures --cases timescale_down,auth_down,kafka_down,bad_model_hash`; JSON asserts ingest unready only for first two and publisher degraded for Kafka |
| T3 | `mvn -f backend/contracts/telemetry/pom.xml test -Dfixtures=src/test/resources/legacy-valid` | `mvn -f backend/contracts/telemetry/pom.xml test -Dtest=TelemetryContractRejectionTest`; JUnit asserts unsupported schema/client owner rejected |
| T4 | `mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am -Dtest=InternalIdentityAndDeviceAuthorizationIT -Dsurefire.failIfNoSpecifiedTests=false test` | same test with `-Dcases=revoked,cross_device,unbound,spoofed_header,auth_unavailable`; JUnit asserts 401/403 and zero writes |
| T5 | `python backend/qa/rehealth_stack_gate.py config-matrix --valid production,staging,development,demo` | same command with `--invalid production_demo,disabled_software_db,http_external,embedded_secret`; JSON asserts startup rejection codes |
| T6 | `mvn -f backend/device-service/pom.xml test` | `mvn -f backend/device-service/pom.xml -Dtest=DeviceApiBoundaryIT -Dcases=unauthenticated,cross_user,malformed test`; JUnit asserts stable 4xx and zero rows |
| T7 | `mvn -f backend/device-service/pom.xml -Dtest=TimescaleMigrationIT test` | same test with `-Dcases=duplicate_source,unsupported_extension,timezone_roundtrip`; JUnit plus SQL snapshot asserts expected rejection/conversion |
| T8 | `mvn -f backend/device-service/pom.xml -Dtest=TelemetryIngestionIT test` | same test with `-Dcases=concurrent_duplicate,mid_batch_failure,db_down`; JUnit/SQL counts assert one logical batch or atomic zero rows |
| T9 | `python backend/qa/rehealth_stack_gate.py kafka --fixture backend/contracts/fixtures/valid/mixed-batch.json --report <attemptDir>/task-9-complete-rehealth-backend-model-service/kafka.json` | same command with `--cases broker_down,publisher_poison,consumer_poison,duplicate_event`; JSON asserts pending recovery/quarantine/DLQ/idempotent projection |
| T10 | `python backend/device-service/scripts/migrate_hardware.py reconcile --source-fixture backend/device-service/src/test/resources/mysql-hardware.sql --output <attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.json && cosign sign-blob --key env://REHEALTH_CUTOVER_SIGNING_KEY --output-signature <attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.sig <attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.json` | repeat reconcile with `--inject-target-drift drop_one_row --expect-blocked`; JSON includes exact differing key/count/hash and `eligible=false`, and signing is forbidden for ineligible reports |
| T11 | `python backend/qa/rehealth_stack_gate.py cutover --reconciliation <attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.json --signature <attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.sig --verify-key-env REHEALTH_CUTOVER_VERIFY_KEY` | same with `--cases bad_signature,expired_report,dirty_reconciliation,route_collision`; JSON asserts route unchanged and cutover denied |
| T12 | `python backend/qa/rehealth_stack_gate.py attribution --modes pias,demo_mock --fixture backend/contracts/fixtures/attribution/ready.json` | same with `--cases accumulating,pias_down,production_demo,client_history_spoof`; JSON asserts provenance/status/no fallback/startup rejection |
| T13 | `pytest -q model-service/tests/test_api.py model-service/tests/test_risk_scorer.py model-service/tests/test_observability.py` | `pytest -q model-service/tests/test_readiness_failures.py`; asserts bad/missing artifact, schema mismatch and timeout remain unavailable, never fake-real |
| T14 | `python backend/qa/rehealth_stack_gate.py health-agent --fixture backend/contracts/fixtures/agent/conservative.json` | same with `--cases cross_user,diagnosis_prompt,prompt_injection,rate_limit,provider_down`; JSON asserts safe status/disclaimer/no leaked context/key |
| T15 | `python backend/qa/rehealth_stack_gate.py model-matrix --real-services --report <attemptDir>/task-15-complete-rehealth-backend-model-service/model-matrix.json` | same with `--failure-matrix malformed_json,http_4xx,http_5xx,timeout,partial_payload`; every dependency has asserted stable error/audit outcome |
| T16 | `mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -Dtest=CareInsuranceSchemaIT test` | same with `-Dcases=cross_tenant_link,duplicate_assignment,invalid_transition,duplicate_claim_key`; JUnit asserts constraint failures |
| T17 | `mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -Dtest=CareInsuranceWorkflowIT test` | same with `-Dcases=cross_tenant,stale_version,auto_approval,replayed_submission`; asserts 403/409/idempotent receipt/zero unauthorized mutation |
| T18 | `mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -Dtest=ReHealthRolePermissionMatrixIT test` | same with `-Dcases=direct_url,wildcard_non_admin,hidden_menu_api`; JUnit asserts 403 for every denied cell and no duplicate seeds |
| T19 | `python backend/qa/rehealth_stack_gate.py operations --roles operations,auditor --fixture backend/contracts/fixtures/operations/cases.json` | same with `--cases unauthorized_replay,cross_tenant,device_service_down`; JSON asserts zero mutation/redaction/stale-dependency error |
| T20 | `pnpm --dir backend/jeecgboot-vue3 test && pnpm --dir backend/jeecgboot-vue3 playwright test tests/rehealth/admin-workflows.spec.ts` | `pnpm --dir backend/jeecgboot-vue3 playwright test tests/rehealth/forbidden-and-errors.spec.ts`; traces assert forbidden direct action and recoverable dependency error |
| T21 | `python backend/qa/rehealth_stack_gate.py observability --fixture backend/contracts/fixtures/valid/mixed-batch.json` | same with `--inject kafka_down,model_down,timescale_down --scan-logs`; JSON asserts alert fire/recover, correlation chain and zero sensitive matches |
| T22 | `docker run --rm --network rehealth k6 run /qa/k6/release.js --summary-export /evidence/load.json` | `python backend/qa/rehealth_stack_gate.py resilience --cases kafka_outage,device_restart,timescale_restart,model_restart`; k6/JSON asserts thresholds, exact counts and backlog drain |
| T23 | `python backend/deploy/rehealth/scripts/dr_verify.py --full --evidence <attemptDir>/task-23-complete-rehealth-backend-model-service/restore.json` | same with `--cases truncated_backup,wrong_key,schema_mismatch`; JSON asserts promotion blocked and source untouched |
| T24 | `python backend/qa/release_gate.py --profile production-like --artifact-bundle-env REHEALTH_ARTIFACT_BUNDLE --evidence <attemptDir>/task-24-complete-rehealth-backend-model-service` | same with `--negative-gates unsafe_config,mock_in_production,missing_migration,cross_user,secret_leak,load_fail,restore_fail,ui_diff`; release JSON must mark every negative gate blocked |

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.

- Wave 1, contracts and platform foundation: Todos 1-5.
- Wave 2, Device Service data path: Todos 6-11.
- Wave 3, model/agent authority: Todos 12-15.
- Wave 4, business and administration: Todos 16-20.
- Wave 5, operational proof and release: Todos 21-24.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | none | 2-24 | none |
| 2 | 1 | 6, 10, 21 | 3, 4, 5 |
| 3 | 1 | 6-11 | 2, 4, 5 |
| 4 | 1 | 6, 10 | 2, 3, 5 |
| 5 | 1 | 12-15 | 2, 3, 4 |
| 6 | 2, 3, 4 | 7-11 | none |
| 7 | 3, 6 | 8-11 | none |
| 8 | 2, 3, 6, 7 | 9-11 | none |
| 9 | 7, 8 | 10, 11, 19, 21 | none |
| 10 | 7-9 | 11, 21, 24 | none |
| 11 | 2, 4, 8-10 | 21, 24 | none |
| 12 | 5 | 14, 15, 24 | 13 |
| 13 | 5 | 14, 15, 24 | 12 |
| 14 | 12, 13 | 15, 24 | none |
| 15 | 12-14 | 21, 24 | none |
| 16 | 1 | 17-20, 24 | 12, 13 |
| 17 | 16 | 18-20, 24 | none |
| 18 | 16, 17 | 20, 24 | 19 |
| 19 | 9, 16, 17 | 20, 24 | 18 |
| 20 | 17-19 | 24 | none |
| 21 | 2, 9, 10, 11, 15 | 22-24 | none |
| 22 | 11, 15, 20, 21 | 24 | 23 |
| 23 | 9, 11, 20, 21 | 24 | 22 |
| 24 | 10, 11, 15, 20-23 | final wave | none |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Freeze architecture, compatibility and data-governance contracts
  What to do / Must NOT do: Add ADRs and versioned JSON/OpenAPI contracts defining Gateway route ownership, service trust, current Android-compatible response shapes, Timescale ownership, Kafka topics, Outbox semantics, attribution modes, admin tenancy and health-agent safety. Resolve repository-document conflict explicitly: `rehealth-algorithms` remains training/research except its hardened PIAS production app; model-service owns risk/intervention/health-agent plus Demo Mock attribution; Jeecg owns authenticated orchestration. Define `rehealth.telemetry.persisted.v1`, `rehealth.telemetry.quality.v1` and `rehealth.telemetry.dlq.v1`; Kafka events contain only event/batch/schema IDs, opaque tenant/user/device IDs, time range, record counts, quality summary and persistence status, never normalized metric values, tokens or raw PPG/RRI. Partition by opaque device ID to preserve per-device order; consumer processing is at-least-once/idempotent. Do not change public Android routes.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 2-24
  References: `AGENTS.md`; `ENGINEERING.md`; `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md`; `backend/docs/DATABASE_SPLIT_ARCHITECTURE.md`; `ReHealthMobileController.java:62-183`; `outputs/demo_ui_live_api_integration_report.md:243-254`.
  Acceptance criteria: JSON Schema/OpenAPI lint exits 0; route-contract test proves existing request/response fields remain accepted; ADR explicitly states durable-write-before-success, at-least-once Kafka and idempotent consumer rules.
  QA scenarios: Execute exact gate **T1** above. happy: validate all schemas and example payloads; failure: payload containing `token`, raw signal bytes or client-controlled owner/tenant fails schema/policy test. Evidence `<attemptDir>/task-1-complete-rehealth-backend-model-service/`.
  Commit: Y | `docs(architecture): freeze gateway device kafka timescale contracts`

- [x] 2. Build the reproducible production/staging deployment topology
  What to do / Must NOT do: Add `backend/deploy/rehealth/` compose profiles for Gateway, JeecgBoot cloud start with `jeecg-module-rehealth` registered, Device Service, MySQL software_db, TimescaleDB on PostgreSQL 17, Apache Kafka 4.3.1 KRaft, Redis, model-service, hardened PIAS app, Prometheus, Grafana and built `jeecg-vue` SPA served behind Gateway/reverse proxy. Add ReHealth service discovery and exact `/jeecg-boot/rehealth/**`/telemetry route seeds. Pin production images by digest in `images.lock`; model artifacts arrive through an approved read-only artifact bundle whose manifest/hash/signature is verified before readiness; use environment/secret files ignored by Git. Only the public edge is published. Device Service readiness depends on Timescale and auth resolution, not Kafka: Kafka outage sets publisher-degraded/outbox-lag health while ingest remains ready. Do not add credentials or bind internal services publicly.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 6, 10, 21
  References: `backend/docker-compose.yml`; `backend/docker-compose-cloud.yml`; `model-service/Dockerfile`; `rehealth-algorithms/docker/docker-compose.prod.yml`; Apache Kafka supported image `apache/kafka:4.3.1`; Timescale official PostgreSQL 17 image guidance.
  Acceptance criteria: `docker compose config` succeeds for dev/staging/prod; secret scan finds zero values; prod port audit shows only the public edge; exact gateway path/service discovery tests pass; killing Timescale/auth makes Device Service not-ready, while killing Kafka leaves ingest ready but publisher degraded; missing/invalid model artifact makes model readiness false.
  QA scenarios: Execute exact gate **T2** above. happy: clean checkout plus approved artifact/secret bundle reaches all required health/readiness checks and serves admin SPA through the edge; failure: missing DB/PIAS/secret or bad artifact hash prevents the owning capability from ready, while Kafka-only outage proves durable ingest remains available. Evidence `<attemptDir>/task-2-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(deploy): add production rehealth service topology`

- [x] 3. Define reusable telemetry and event contract modules
  What to do / Must NOT do: Extract the existing validated telemetry DTO/validation rules into a small versioned Java contract module consumable by Jeecg compatibility tests and Device Service. Preserve batch limits, normalized measurement/sleep/activity fields and response semantics; keep persistence/business logic out of the shared module.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 6-11
  References: `TelemetryBatchRequestDto.java`; `TelemetryBatchResponseDto.java`; `RecentTelemetryResponseDto.java`; `TelemetryBatchValidator.java`; `HardwareTelemetryIngestionService.java`; current ReHealth module tests.
  Acceptance criteria: old captured contract fixtures deserialize unchanged in both modules; validator boundary tests cover empty, oversized, malformed timestamp/type and valid mixed batch; module has no Spring JDBC/Kafka dependency.
  QA scenarios: Execute exact gate **T3** above. happy: existing Android/Jeecg fixtures round-trip; failure: invalid owner field and unsupported schema version are rejected deterministically. Evidence `<attemptDir>/task-3-complete-rehealth-backend-model-service/`.
  Commit: Y | `refactor(telemetry): extract versioned shared contracts`

- [x] 4. Establish service-to-service identity, device authorization and Gateway routing contract
  What to do / Must NOT do: Add internal Jeecg endpoints protected by a distinct environment-injected service credential: resolve `X-Access-Token` to immutable user/tenant claims and authorize `(tenant,user,device)` against active device binding. Device Service never logs/caches token material and fails closed if identity or binding resolution is unavailable/mismatched. Gateway strips spoofable internal headers and routes measurement batch/recent/ops paths to Device Service while leaving business/model paths on JeecgBoot. Positive identity/binding cache max 30 seconds; logout, unbind/rebind and negative results bypass/evict cache. Do not share a hardcoded JWT secret or trust client user/device IDs.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 6, 10
  References: `ReHealthMobileController.java:102-132`; Jeecg Shiro/JWT filters under `backend/jeecg-boot`; `Android-apk` `X-Access-Token` route tests; `backend/docker-compose-cloud.yml` Gateway module.
  Acceptance criteria: integration tests prove valid token maps to correct claims/binding, revoked/expired token returns 401, unbound/mismatched device returns 403, wrong service credential returns 403, spoofed headers are removed and no token appears in logs/cache serialization.
  QA scenarios: Execute exact gate **T4** above. happy: authenticated owner uploads for an active binding; failure: body userId, another user's deviceId, stale binding, fake internal header and revoked token cannot create/query rows. Evidence `<attemptDir>/task-4-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(auth): add internal identity resolution for device service`

- [x] 5. Make runtime modes explicit and fail closed
  What to do / Must NOT do: Add validated configuration objects/startup validators for `production|staging|development|demo`, DB/service URLs, attribution mode and provider credentials. Production requires software persistence, Device Service, Timescale, real model availability and `pias`; rejects `demo_mock`, blank/insecure URLs and no-op repositories. Development may use Demo mode only with visible metadata.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 12-15
  References: `E1PendingSoftwareDbReHealthBusinessRepository.java:19-84`; `JdbcSoftwareDbReHealthBusinessRepository.java:34-52`; `HttpModelServiceClient.java:29-36`; `model-service/app/main.py:31-77`; `Android-apk/docs/DEEPSEEK_LOCAL_CONFIG.md` if present.
  Acceptance criteria: configuration matrix tests cover every mode; production startup fails before serving traffic when any required dependency/mode is unsafe; Demo starts only with explicit flag and reports Demo provenance.
  QA scenarios: Execute exact gate **T5** above. happy: staging/production safe configuration becomes ready; failure: `production + demo_mock`, disabled software DB, HTTP external URL or embedded provider key fails startup. Evidence `<attemptDir>/task-5-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(config): enforce fail-closed production modes`

- [x] 6. Scaffold the independent Device Service with preserved API surface
  What to do / Must NOT do: Create `backend/device-service` as Spring Boot 3.5.5/Java 17 with controller, application service, ports, Actuator and OpenAPI. Implement `/rehealth/mobile/measurements/batch`, `/measurements/recent`, health/readiness and internal operations namespaces using shared contracts and resolved claims. Do not copy Jeecg business/model responsibilities.
  Parallelization: Wave 2 | Blocked by: 2, 3, 4 | Blocks: 7-11
  References: current `jeecg-module-rehealth/ingest/**`; `ReHealthMobileController.java:102-125`; `HardwareIngestionPort.java`; `HardwareTelemetryQuery.java`; parent Maven Java/Spring versions.
  Acceptance criteria: Device Service builds independently; MockMvc contract tests match existing mobile response envelope/status/error semantics; architecture test forbids dependencies on Jeecg repository/model packages.
  QA scenarios: Execute exact gate **T6** above. happy: valid authenticated mixed batch accepted and recent endpoint scoped; failure: unauthenticated, cross-user and malformed batch requests return stable 4xx without writes. Evidence `<attemptDir>/task-6-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(device-service): add standalone telemetry service`

- [x] 7. Create TimescaleDB hardware schema, hypertables and lifecycle policies
  What to do / Must NOT do: Add Flyway migrations for upload batches, measurements, sleep/activity, signal metadata, quality events, reconciliation, Outbox and migration checkpoints. Use UTC `TIMESTAMPTZ`: measurement `observed_at` (1-day chunks), sleep/activity `started_at` (7-day chunks), quality `event_at` (7-day chunks) as hypertable time columns; upload batch, reconciliation, Outbox and checkpoint remain ordinary tables. Unique source keys include tenant/user/device/time/type/source-record ID as required by Timescale partition rules. Compress telemetry/session/quality after 7 days. Configurable default retention: normalized measurement/sleep/activity 730 days, signal metadata 90 days, quality/batch/reconciliation 1095 days, published Outbox 30 days; unresolved/failed Outbox is never auto-deleted. Interpret legacy MySQL `DATETIME(3)` as UTC values originally derived from epoch milliseconds and test timezone conversion. Store no raw signal payload while raw upload is disabled.
  Parallelization: Wave 2 | Blocked by: 3, 6 | Blocks: 8-11
  References: `db/hardware/mysql/V1__create_hardware_telemetry_tables.sql:1-107`; `JdbcHardwareTelemetryWriter.java`; `JdbcHardwareTelemetryQuery.java`; `backend/docs/REHEALTH_DB_SCHEMA.md`.
  Acceptance criteria: migrations apply twice safely on clean and upgraded PostgreSQL 17 + TimescaleDB; `timescaledb_information.hypertables` and policy views contain expected objects; indexes support scoped recent queries; Flyway validates.
  QA scenarios: Execute exact gate **T7** above. happy: Testcontainers migration + sample query succeeds; failure: duplicate batch/event constraints reject duplicates and a migration against unsupported extension/version fails before writes. Evidence `<attemptDir>/task-7-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(timescale): add hardware schema and policies`

- [x] 8. Port durable telemetry ingestion to Timescale transaction plus Outbox
  What to do / Must NOT do: Port current validation/idempotency writer into Device Service. One transaction must insert/resolve batch receipt, normalized telemetry/session rows, quality/rejection events, reconciliation state and versioned Outbox events. Define reconciliation states `RECEIVED -> PERSISTED -> EVENT_PENDING -> EVENT_PUBLISHED`, with `REJECTED`, `RETRYABLE_FAILURE`, `DLQ_REVIEW`, `RESOLVED`; operator replay is idempotent and records actor/reason. Replaying a batch returns the original receipt and creates neither duplicate telemetry nor duplicate event. HTTP success means Timescale commit succeeded; Kafka state is not part of that transaction.
  Parallelization: Wave 2 | Blocked by: 2, 3, 6, 7 | Blocks: 9-11
  References: `JdbcHardwareTelemetryWriter.java`; `HardwareTelemetryIngestionService.java`; `TelemetryBatchValidator.java`; existing idempotency tests; Todo 1 event schemas.
  Acceptance criteria: Testcontainers tests cover commit, rollback, mixed payload, concurrent duplicate batch, owner scoping and stable receipt; row/outbox counts are exact; injected DB failure returns retryable 503 and zero partial rows.
  QA scenarios: Execute exact gate **T8** above. happy: first and replayed uploads return identical receipt with one persisted batch/event set; failure: kill DB or violate one record and verify atomic rollback/no false success. Evidence `<attemptDir>/task-8-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(device-service): persist telemetry with transactional outbox`

- [x] 9. Add Kafka publisher, projection consumer, schemas, retry and DLQ operations
  What to do / Must NOT do: Publish unpublished Outbox rows using idempotent producer settings (`acks=all`, idempotence enabled), event IDs and schema version. Mark published only after broker ack; backoff on failure; quarantine publisher serialization/contract poison rows as Outbox `DLQ_REVIEW` instead of pretending they are consumer DLQ records. Add a Jeecg idempotent consumer that projects minimal upload status and quality-case summaries into software_db for admin queries, keyed by event ID; consumer failures use bounded retry and `rehealth.telemetry.dlq.v1`. Configure TLS/SASL-ready secrets, least-privilege ACLs, 7-day main-topic retention and 30-day DLQ retention. Do not put metric values, tokens, phone numbers or raw PPG/RRI in messages; do not trigger model inference directly from ingest.
  Parallelization: Wave 2 | Blocked by: 7, 8 | Blocks: 10, 11, 21
  References: Todo 1 schemas; Todo 2 Kafka compose; Todo 8 Outbox; `ReHealthIngestProperties.java` current queue settings.
  Acceptance criteria: Kafka Testcontainers tests prove eventual publish, Jeecg projection creation, duplicate consumer idempotency, per-device order, broker outage leaves Outbox pending, recovery drains once logically, publisher poison is quarantined and consumer poison reaches DLQ with redacted metadata.
  QA scenarios: Execute exact gate **T9** above. happy: commit while broker down then recover and observe one logical event; failure: poison record exhausts bounded retries, lands in DLQ and remains replayable without blocking later events. Evidence `<attemptDir>/task-9-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(kafka): publish durable telemetry lifecycle events`

- [x] 10. Migrate MySQL hardware history with shadow verification before cutover
  What to do / Must NOT do: Dark-deploy Device Service and add a checkpointed migration CLI/job mapping every existing hardware table to Timescale. Backfill in deterministic time/ID pages, upsert idempotently, compare per-user/device/day counts plus canonical hashes and sample values, and shadow-read/compare while public writes remain on MySQL. The CLI must emit `<attemptDir>/task-10-complete-rehealth-backend-model-service/cutover/reconciliation.json` with immutable source/target DSN fingerprints (no credentials), schema versions, UTC window, per-table/per-scope counts and hashes, mismatch count, `eligible`, final Git SHA and creation/expiry timestamps. CI's dedicated `rehealth-cutover` release identity signs the exact bytes using `cosign sign-blob --key env://REHEALTH_CUTOVER_SIGNING_KEY --output-signature .../reconciliation.sig`; the public verification key is injected separately. Do not switch Gateway in this task and never delete source data automatically.
  Parallelization: Wave 2 | Blocked by: 7-9 | Blocks: 11, 21, 24
  References: `V1__create_hardware_telemetry_tables.sql`; `JdbcHardwareTelemetryWriter.java`; `JdbcHardwareTelemetryQuery.java`; 2026-07-23 staging evidence in `outputs/demo_ui_live_api_integration_report.md:243-254`.
  Acceptance criteria: synthetic and staging-copy migrations resume after interruption and are idempotent; reconciliation reaches exact counts/hashes or emits `eligible=false`; `cosign verify-blob --key env://REHEALTH_CUTOVER_VERIFY_KEY --signature .../reconciliation.sig .../reconciliation.json` exits 0; timezone and null/source-ID mappings are proven. Signing private key never enters the workspace/evidence.
  QA scenarios: Execute exact gate **T10** above. happy: migrate/restart/re-run and shadow-compare exact datasets; failure: mutate/drop one target row and verify approval artifact refuses cutover with a pinpointed discrepancy. Evidence `<attemptDir>/task-10-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(migration): backfill hardware mysql into timescale`

- [x] 11. Cut Gateway telemetry routes to Device Service without Android changes
  What to do / Must NOT do: `backend/qa/rehealth_stack_gate.py cutover` is the sole route-switch gate. It descriptor-reads Todo 10's exact JSON/signature, verifies the cosign public key, final Git SHA, non-expired window, expected DSN/schema fingerprints, `eligible=true` and zero mismatch, then writes an atomic deployment audit record containing reconciliation SHA-256 and changes the Gateway route; any failed check leaves route/config unchanged. Configure cloud Gateway service discovery and route precedence so public measurement batch/recent paths reach Device Service; preserve Result envelope/error codes. Keep Jeecg legacy writer unreachable externally. Before authority cutover, rollback may route to MySQL. After Timescale becomes authoritative, application rollback must continue using Timescale; do not promise reverse routing unless a separately signed Timescale-to-MySQL reconciliation exists. Update Android API contract tests/base deployment URL only if needed, never Compose UI.
  Parallelization: Wave 2 | Blocked by: 2, 4, 8-10 | Blocks: 21, 24
  References: `ReHealthMobileController.java:102-125`; Android ReHealth route tests; `backend/docker-compose-cloud.yml`; `jeecg-gateway-router.json`; Todo 4 and Todo 10.
  Acceptance criteria: T11 consumes the signed artifact deterministically and records its SHA-256/CI signer/Git SHA; gateway integration test proves cloud module registration, exact path ownership, auth/binding propagation, response compatibility and no double writer; direct production ports unavailable; every invalid/expired/dirty/stale artifact leaves route unchanged; pre/post-authority rollback rules are executable and documented.
  QA scenarios: Execute exact gate **T11** above. happy: unchanged Android fixture uploads/queries through Gateway into Timescale; failure: dirty reconciliation or route collision blocks cutover, and post-cutover app rollback retains Timescale authority. Evidence `<attemptDir>/task-11-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(gateway): cut telemetry authority to device service`

- [x] 12. Harden PIAS and unify production attribution while preserving explicit Demo Mock
  What to do / Must NOT do: Create a production PIAS FastAPI entrypoint exposing only health/readiness and `/api/pias/v2/attribute/individual`; exclude wildcard CORS, insurance/research/test routers and mock endpoints. Require internal service authentication, request ID/idempotency, stable error taxonomy and engine/version provenance; Jeecg builds risk history from authenticated persisted records rather than trusting client ownership/history labels. Add typed mode `pias|demo_mock`: `pias` calls hardened PIAS; `demo_mock` calls model-service `/v1/cvd/attribution/individual`. Extend response/audit with `attributionMode`, `isMock`, provider/model version and `accumulating|ready|error`. Never automatically fall back; production rejects `demo_mock`.
  Parallelization: Wave 3 | Blocked by: 5 | Blocks: 14, 15, 24
  References: `HttpModelServiceClient.java:67-86`; `model-service/app/main.py:74-77`; `model-service/app/attribution.py:9-35`; `AttributionResponseDto.java`; `ReHealthMobileServiceImpl.java` attribution path.
  Acceptance criteria: unit/integration matrix proves pias-ready, pias-accumulating, pias-error, demo-ready and production-demo-startup rejection; UI-compatible arrays/ATT fields remain; audit records provenance; no silent fallback.
  QA scenarios: Execute exact gate **T12** above. happy: same labelled history returns real PIAS in production-like mode and explicit Mock in Demo; failure: PIAS outage returns understandable error/retry state, never Mock. Evidence `<attemptDir>/task-12-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(attribution): make pias authoritative with demo mock switch`

- [x] 13. Harden model registry, readiness and call observability
  What to do / Must NOT do: Separate liveness/readiness; readiness requires reviewed real artifact in production. Expose safe model registry/version/schema metadata, latency/error counters and correlation IDs. Persist model call audit through Jeecg without feature bodies/tokens. Add timeouts/circuit behavior with stable error taxonomy; do not relabel unavailable real model as Mock outside Demo.
  Parallelization: Wave 3 | Blocked by: 5 | Blocks: 14, 15, 24
  References: `model-service/app/model_registry.py`; `model-service/app/risk_scorer.py`; `model-service/app/main.py:37-65`; `rehealth_model_request_log`; `ReHealthMobileServiceImpl.java:237-238`.
  Acceptance criteria: Pytest and Java tests cover real available/unavailable, schema mismatch, timeout and redacted audit; Prometheus endpoint exposes bounded-label metrics; production readiness is false for Mock/unavailable artifact.
  QA scenarios: Execute exact gate **T13** above. happy: real CatBoost health/evaluate reports matching trace; failure: remove/wrong-order artifact and verify readiness false plus stable 503, no fake success. Evidence `<attemptDir>/task-13-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(model): harden registry readiness and telemetry`

- [ ] 14. Add the authenticated health-agent model-service boundary
  What to do / Must NOT do: Implement a new typed, stateless provider adapter at model-service `/v1/health-agent/respond` and Jeecg `/rehealth/mobile/agent/messages`; do not productionize/import the existing `rehealth-algorithms/healthagent` simulation or its local paths. Jeecg assembles a minimal, server-authorized context from profile, recent risk/intervention and selected normalized trends, applies per-user/tenant Redis rate limits, calls model-service, records metadata and returns conservative non-diagnostic output. Provider API key lives only in model-service environment/secrets. Add explicit provider-disabled Demo behavior only outside production.
  Parallelization: Wave 3 | Blocked by: 12, 13 | Blocks: 15, 24
  References: `model-service/app/main.py`; `ReHealthBusinessRepository.java`; `JdbcSoftwareDbReHealthBusinessRepository.java`; Android `DeepSeekClient.kt` and `DEEPSEEK_LOCAL_CONFIG.md`; repository medical/privacy rules.
  Acceptance criteria: typed validation, authentication, ownership, rate-limit, prompt-injection/context-boundary and provider-timeout tests pass; response includes disclaimer/request ID/model metadata; no provider key/health prompt appears in APK, logs or audit DB.
  QA scenarios: Execute exact gate **T14** above. happy: authenticated synthetic context receives conservative answer; failure: cross-user context request, diagnostic-demand prompt, rate-limit exceed and provider outage return safe bounded responses. Evidence `<attemptDir>/task-14-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(agent): add secure health agent backend entry`

- [ ] 15. Complete model/PIAS/agent contract and failure integration tests
  What to do / Must NOT do: Build cross-service contract tests for Jeecg -> model-service risk/intervention/health-agent and Jeecg -> PIAS/Mock attribution, including correlation IDs, response mappings and audit. Use stubs for deterministic failures plus real container services for happy paths. Do not treat HTTP 200 with mock/unready metadata as production success.
  Parallelization: Wave 3 | Blocked by: 12-14 | Blocks: 21, 24
  References: `HttpModelServiceClient.java`; all model DTOs; `model-service/tests/test_api.py`; PIAS tests under `rehealth-algorithms`; Todo 1 contracts.
  Acceptance criteria: one command runs the matrix and asserts provider, mode, version, mock flag, status and user-scoped audit; malformed/partial JSON, 4xx/5xx, timeout and accumulating PIAS are covered.
  QA scenarios: Execute exact gate **T15** above. happy: real CatBoost + real PIAS + configured test agent complete; failure: each downstream independently unavailable produces correct ready/error state without cascading data corruption. Evidence `<attemptDir>/task-15-complete-rehealth-backend-model-service/`.
  Commit: Y | `test(integration): cover model pias and agent boundaries`

- [ ] 16. Add software_db schemas and explicit state machines for care, insurance and program operations
  What to do / Must NOT do: Add additive migrations for institution extension, clinician profile, clinician-patient assignment, insurance program/enrollment, policy, premium/payment record, claim, claim evidence, manual review/approval, settlement, follow-up plan/session/outcome/form/result, health-task template/assignment/completion. Freeze state transitions: assignment `PENDING->ACTIVE->ENDED`; enrollment/policy `DRAFT->ACTIVE->SUSPENDED|EXPIRED|CANCELLED`; claim `DRAFT->SUBMITTED->UNDER_REVIEW->APPROVED|REJECTED->SETTLED` with manual approval before settlement; follow-up `PLANNED->SCHEDULED->COMPLETED|MISSED|CANCELLED`; task assignment `PENDING->IN_PROGRESS->COMPLETED|SKIPPED|EXPIRED`. Require tenant/organization scope, idempotency keys for submissions/completions/claims/payments, optimistic version, audit timestamps and logical deletion where Jeecg conventions require. Reference Jeecg user IDs without duplicating credentials. Do not store raw hardware telemetry in software_db.
  Parallelization: Wave 4 | Blocked by: 1 | Blocks: 17-20, 24
  References: `V1__create_rehealth_software_tables.sql`; `V20260723_2__upgrade_legacy_software_schema.sql`; Jeecg system user/tenant conventions; `JdbcSoftwareDbReHealthBusinessRepository.java`.
  Acceptance criteria: migrations apply idempotently to clean and legacy MySQL 8; constraints/indexes enforce tenant-scoped uniqueness and valid relationships; rollback is additive/forward-fix, not destructive.
  QA scenarios: Execute exact gate **T16** above. happy: representative organization-care-program graph persists/queries; failure: cross-tenant link, duplicate active assignment and invalid lifecycle transition fail. Evidence `<attemptDir>/task-16-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(software-db): add care and program domains`

- [ ] 17. Implement tenant-safe domain services and admin APIs
  What to do / Must NOT do: Add handwritten services/controllers under `/rehealth/admin/v1` for institutions, clinicians, assignments, insurance programs/enrollments/policies/premiums/claims/evidence/approval/settlement, follow-up plans/sessions/outcomes and health-task templates/assignments/completions. Enforce Todo 16 transitions, idempotency, pagination/filtering, optimistic locking, audit, patient consent and row scope. Generate DTOs/mappers according to project conventions; never expose entities/raw health payloads directly and never auto-approve clinical/insurance decisions.
  Parallelization: Wave 4 | Blocked by: 16 | Blocks: 18-20, 24
  References: Todo 16 schema; Jeecg controller/service/MyBatis-Plus patterns; existing ReHealth mobile service ownership logic; `AGENTS.md` backend expectations.
  Acceptance criteria: Maven tests cover CRUD, lifecycle, pagination, concurrent update, tenant isolation and permission failures; OpenAPI contains every approved namespace with stable error envelope.
  QA scenarios: Execute exact gate **T17** above. happy: authorized clinician completes assignment/follow-up/task workflow; failure: clinician from another institution and insurance operator outside program scope receive 403 with zero mutations. Evidence `<attemptDir>/task-17-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(admin-api): add care operations services`

- [ ] 18. Seed Jeecg RBAC permissions and menus
  What to do / Must NOT do: Add idempotent SQL/migrations for roles/permissions/menu entries covering platform admin, institution admin, doctor, insurance operator, operations reviewer and read-only auditor. Map fine-grained permissions to each admin endpoint/action; least privilege by default. Do not grant wildcard ReHealth permission to non-platform roles.
  Parallelization: Wave 4 | Blocked by: 16, 17 | Blocks: 20, 24
  References: Jeecg `sys_permission`, role/menu seed conventions; Todo 17 endpoints; existing Shiro/JWT authorization annotations.
  Acceptance criteria: repeatable seed test yields no duplicates; role-permission matrix integration tests assert allow/deny for each resource/action; menu visibility matches API authorization.
  QA scenarios: Execute exact gate **T18** above. happy: each role sees and invokes only intended modules; failure: direct URL/API invocation without permission remains 403 even if menu is hidden. Evidence `<attemptDir>/task-18-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(rbac): add rehealth roles permissions and menus`

- [ ] 19. Implement device, failure, quality, risk and model-audit operations APIs
  What to do / Must NOT do: Add authenticated operator APIs/read models that aggregate Device Service fleet/binding status, failed/retry upload batches, DLQ/Outbox lag, data-quality events, software risk stratification and model request audit. Cross-service aggregation must use bounded APIs, not direct Jeecg reads of Timescale tables. Redact identifiers and never expose raw signals/model prompts.
  Parallelization: Wave 4 | Blocked by: 9, 16, 17 | Blocks: 20, 24
  References: Device Service reconciliation/quality tables from Todo 7; `hardware_data_quality_event`; `rehealth_cvd_risk_result`; `rehealth_model_request_log`; `GET /measurements/recent` user-scope pattern.
  Acceptance criteria: API tests cover filters/pagination/redaction/tenant scope, retry/replay authorization and stale dependency behavior; risk list exposes model/mock provenance and quality, not diagnostic claims.
  QA scenarios: Execute exact gate **T19** above. happy: operations user resolves a failed batch and reviews quality/risk/audit; failure: unauthorized retry, cross-tenant query and unavailable Device Service produce no mutation or fabricated empty success. Evidence `<attemptDir>/task-19-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(operations): add device quality risk audit APIs`

- [ ] 20. Build maintainable Jeecg admin and low-code surfaces
  What to do / Must NOT do: Use Jeecg codegen/Online-form conventions for straightforward organization, insurance, enrollment, follow-up form and health-task CRUD; add custom Vue workbench pages only for doctor queues, device/reconciliation, quality, risk stratification and model audit. Bind every page to Todo 17/19 APIs and Todo 18 permissions. Do not implement business rules in generated Vue/Online form expressions.
  Parallelization: Wave 4 | Blocked by: 17-19 | Blocks: 24
  References: `backend/jeecgboot-vue3/src/views`; Jeecg generated CRUD patterns; Todo 17/19 OpenAPI; Todo 18 menu seeds.
  Acceptance criteria: frontend typecheck/lint/test/build passes; Playwright role matrix verifies menus, list/detail/form/workbench actions, CJK rendering, empty/loading/error states and forbidden navigation; generated metadata/import artifacts are versioned and repeatable.
  QA scenarios: Execute exact gate **T20** above. happy: admin, doctor, insurer and operations roles complete their permitted workflows; failure: hidden/forbidden action cannot be invoked via UI or direct API, backend outage shows recoverable error. Evidence `<attemptDir>/task-20-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(admin-ui): add rehealth low-code and workbench pages`

- [ ] 21. Add end-to-end observability and privacy-safe operational alerts
  What to do / Must NOT do: Propagate correlation IDs Gateway -> Jeecg/Device Service -> Kafka events -> model-service/PIAS. Export Actuator/Micrometer and FastAPI Prometheus metrics for request latency/error, DB pools, ingest/outbox/Kafka lag, model/PIAS mode/readiness and agent rate limits. Add structured redacted logs, dashboards and alerts for persistence failures, backlog, DLQ, model unavailable and backup age. Avoid unbounded labels and health/identity data.
  Parallelization: Wave 5 | Blocked by: 2, 9-11, 15 | Blocks: 22-24
  References: Todo 2 monitoring services; current logger calls; G3 privacy evidence in `outputs/demo_ui_live_api_integration_report.md:233-241`; repository privacy rules.
  Acceptance criteria: metrics/dashboard provisioning tests pass; synthetic request trace correlates across services without raw health/token/PII; alert rules fire and recover on injected failures; cardinality budget test passes.
  QA scenarios: Execute exact gate **T21** above. happy: trace one synthetic upload through persistence/outbox/Kafka and one risk/PIAS call; failure: injected broker/model/DB outage produces the correct alert and redacted logs. Evidence `<attemptDir>/task-21-complete-rehealth-backend-model-service/`.
  Commit: Y | `feat(observability): add cross-service metrics traces and alerts`

- [ ] 22. Prove capacity, resilience and backlog recovery
  What to do / Must NOT do: Add k6/Testcontainers workload profiles for upload/replay/recent/admin/model paths. Default release gate: 100 concurrent upload VUs for 10 minutes, p95 < 500 ms, HTTP error < 1%, zero lost/duplicate persisted batches; model/PIAS 20 VUs, p95 < 2 s excluding declared provider rate limits; Kafka outage/recovery drains Outbox without blocking persisted uploads. Record hardware/environment with results; do not tune by weakening durability.
  Parallelization: Wave 5 | Blocked by: 11, 15, 20, 21 | Blocks: 24
  References: `ACCEPTANCE_REVIEW_2026-07-16.md:422-428`; Todos 8-10, 15, 19-21; current QA scripts.
  Acceptance criteria: automated report asserts thresholds, exact DB/Kafka counts and no cross-tenant leakage; restart/failure tests cover Device Service, Kafka, Timescale and model-service; all recovery objectives documented.
  QA scenarios: Execute exact gate **T22** above. happy: sustained load meets thresholds and drains cleanly; failure: Kafka unavailable during load keeps HTTP durable writes successful/outbox growing, then recovery drains with no logical duplicates. Evidence `<attemptDir>/task-22-complete-rehealth-backend-model-service/`.
  Commit: Y | `test(performance): add capacity and resilience gates`

- [ ] 23. Automate backup, restore and disaster-recovery verification
  What to do / Must NOT do: Add secret-safe scripts/runbooks for MySQL software_db, TimescaleDB schema/data, Kafka topic configuration/consumer offsets and approved model/PIAS artifact manifests/config. Target RPO <= 5 minutes via MySQL binlog and PostgreSQL WAL/PITR, platform RTO <= 60 minutes, and ingest service restoration <= 30 minutes; keep encrypted daily full backups 30 days plus PITR logs 7 days. Store encryption keys in external secret management, never beside backups. Restore order: software DB/identity dependencies, Timescale, Kafka config/offsets, Jeecg/Device Service, model/PIAS, Gateway/admin. Restore into isolated containers, validate migrations and compare counts/hashes. Do not include tokens/provider keys or claim Kafka backup replaces DB/Outbox backup.
  Parallelization: Wave 5 | Blocked by: 9, 11, 20, 21 | Blocks: 24
  References: Todo 2 deployment; Todo 7/16 schemas; `model-service/models/model_manifest.json`; `ACCEPTANCE_REVIEW_2026-07-16.md:426-428`.
  Acceptance criteria: one command creates encrypted test backups and restores into empty stack; automated comparison validates representative business/telemetry/audit data and service readiness; corrupted/missing backup fails clearly.
  QA scenarios: Execute exact gate **T23** above. happy: full isolated restore serves latest risk/intervention, recent telemetry and admin records; failure: truncated backup/wrong key/schema mismatch blocks promotion and leaves source untouched. Evidence `<attemptDir>/task-23-complete-rehealth-backend-model-service/`.
  Commit: Y | `ops(backup): add verified backup and restore workflow`

- [ ] 24. Run the backend/model platform release gate and publish truthful documentation
  What to do / Must NOT do: Add one release-gate entrypoint running Java/Python/frontend/Android contract tests, migrations, compose health, E2E, security/secret scans, load thresholds and backup restore from a clean checkout plus approved secret/model artifact bundle. Execute authenticated synthetic flow: Gateway login -> profile/interview/bind -> MR11 labelled replay upload -> Timescale -> Outbox/Kafka/software projection -> recent -> real CatBoost -> intervention/feedback -> real PIAS -> health agent -> complete admin/insurance workflows. Verify Demo Mock separately. Update conflicting architecture, API, deployment, migration, operations, release and integration docs. This gate may approve the backend/model platform; overall product/MVP release remains blocked on physical MR11 runtime evidence unless named release authority records an explicit waiver. Do not claim MuMu/physical MR11 validation unless run.
  Parallelization: Wave 5 | Blocked by: 10, 11, 15, 20-23 | Blocks: final wave
  References: all prior todos; `ENGINEERING.md`; `ACCEPTANCE_REVIEW_2026-07-16.md`; `outputs/demo_ui_live_api_integration_report.md`; Android Gradle commands; backend Maven and model Pytest suites.
  Acceptance criteria: release gate exits 0 from clean checkout with environment template; exact service/image/schema/model versions and Git SHA recorded; QA data cleaned; `git status` contains only intended product/test/docs changes; Android Compose source hash/diff proves UI untouched.
  QA scenarios: Execute exact gate **T24** above. happy: production-like real-mode flow and explicit Demo flow both pass with correct provenance; failure: unsafe config, Mock-in-production, missing migration, cross-user spoof, secret leak, failed load/restore or stale UI assertion blocks release. Evidence `<attemptDir>/task-24-complete-rehealth-backend-model-service/`.
  Commit: Y | `chore(release): enforce full cloud platform gate`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit
  Verify every approved requirement maps to shipped code/evidence, every Must-NOT rule holds, and all 24 task receipts bind to final SHA. Return APPROVE or actionable findings.
- [ ] F2. Code quality review
  Review Java/Python/Vue/migrations/compose/security/concurrency for correctness, ownership and maintainability; run final static/build/test suites. Return APPROVE only with no blocking findings.
- [ ] F3. Real manual QA
  Agent launches the final production-like stack and personally exercises the complete real-mode plus explicit Demo-mode flows through Gateway and admin browser, retaining API/DB/Kafka/metrics/screenshots evidence. No self-report-only approval.
- [ ] F4. Scope fidelity
  Compare final diff to this plan and user constraints: Android UI untouched, no silent Mock, no destructive migration, no unapproved service/database, all genuine gaps implemented. Return APPROVE or reject.

## Commit strategy
- Use atomic commits shown per todo; stage explicit allowlists only. Never run repository-wide sync/staging scripts.
- Never commit `.env`, JWTs, provider keys, DB dumps, raw health fixtures, generated APKs, `.omo` evidence or local IDE/cache files.
- Keep schema/data migrations separate from cutover config; tag the pre-cutover release and preserve rollback properties until Todo 24/F3 approval.
- Execute on the existing, already-pushed `codex/real-device` branch as explicitly required by the user. Before every commit/push, verify the branch name and inspect `git status`; preserve unrelated/uncommitted user changes with explicit staging allowlists. Push only after the applicable wave verification, and after final verification require local `codex/real-device` and `origin/codex/real-device` full SHA equality. Do not create/switch branches or rewrite history.

## Success criteria
- Gateway is the only public production entry and preserves existing Android contracts.
- Device Service independently persists authenticated telemetry to TimescaleDB; duplicate/restart/concurrency tests prove exactly one logical batch and stable receipt.
- Kafka 4.3.1 receives versioned Outbox events at least once; broker outage never loses committed telemetry and recovery drains backlog idempotently.
- MySQL hardware history migrates with zero unexplained count/hash drift and retains a tested rollback; source data is not deleted.
- Production risk/intervention use reviewed real model artifacts; attribution uses real PIAS only; Demo Mock remains usable only with explicit visible provenance and cannot start in production.
- Health-agent endpoints are authenticated, scoped, rate-limited, conservative, auditable and keep provider credentials off Android/Jeecg logs.
- Doctor, institution, insurance, follow-up and health-task domains, RBAC/menu seeds and admin pages pass tenant/role workflow tests.
- Device fleet, failed upload, reconciliation, data quality, Kafka projection, risk stratification and model audit operations are observable and actionable.
- Load, outage recovery, backup/restore, privacy/security, migration, full E2E and release gates all pass on the final SHA.
- Backend/model platform release gate passes from a clean checkout plus approved artifact/secret bundle; overall MVP remains blocked on physical MR11 evidence or an explicit release-authority waiver.
- Android Compose UI/layout/navigation/attribution animation files are unchanged; MuMu/physical MR11 remain honestly reported if not executed.
