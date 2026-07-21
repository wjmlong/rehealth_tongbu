# Backend Module Selection Review

Review: E0.5_backend_module_selection_and_database_split  
Date: 2026-07-09  
Scope: read-only architecture review before E1 backend mobile API implementation.

## Decision Summary

Use JeecgBoot as the account, admin, permission, gateway, configuration, and service-runtime foundation. Do not implement production ReHealth code inside `jeecg-module-demo`.

Recommended direction:

- E1 MVP: create a dedicated ReHealth business module in the JeecgBoot monolith path, backed by software and hardware database boundaries.
- E2/high-concurrency: add ReHealth cloud start modules, gateway route `/rehealth/**`, MQ-based ingestion, and operational rate/circuit rules.
- Python `model-service` remains the model authority. Java backend orchestrates calls and persistence only.

## Local JeecgBoot Module Map

| Area | Local module/path | Relevant role for ReHealth |
| --- | --- | --- |
| Parent | `jeecg-boot/pom.xml` | JeecgBoot 3.9.2 parent, Java 17, Spring Boot 3.5.5, Spring Cloud/Alibaba dependency management, starter versions for cloud, job, lock, MQ, sharding, online, database drivers. |
| Base core | `jeecg-boot/jeecg-boot-base-core` | Shared web runtime, MyBatis-Plus, Druid, dynamic datasource, Shiro/JWT, Redis, OpenAPI, validation, OkHttp, datasource drivers. Required foundation for ReHealth modules. |
| System parent | `jeecg-boot/jeecg-module-system` | Aggregates system API, business, and monolith start modules. |
| System API | `jeecg-module-system/jeecg-system-api` | Parent for local/cloud API contracts. |
| System local API | `jeecg-module-system/jeecg-system-api/jeecg-system-local-api` | Local system integration contracts used by monolith modules. |
| System cloud API | `jeecg-module-system/jeecg-system-api/jeecg-system-cloud-api` | Feign/cloud-facing system contracts used by microservices. |
| System business | `jeecg-module-system/jeecg-system-biz` | User, role, permission, menu, department, dictionary, online/report integration, system management. |
| Monolith start | `jeecg-module-system/jeecg-system-start` | Local monolith launcher. Currently depends on `jeecg-system-biz` and `jeecg-module-demo`. Best E1 validation runtime if ReHealth is added as a product module. |
| Business parent | `jeecg-boot/jeecg-boot-module` | Aggregates non-system business modules. Currently includes demo and AIRAG. |
| Demo module | `jeecg-boot-module/jeecg-module-demo` | Demo/sample module. Contains the only current ReHealth prototype, but should not be the production home. |
| AIRAG module | `jeecg-boot-module/jeecg-boot-module-airag` | Jeecg AI/RAG functions. Not needed for ReHealth MVP because model inference/generation stays in Python. |
| Cloud parent | `jeecg-server-cloud` | Microservice deployment aggregator. Includes gateway, Nacos, system cloud start, demo cloud start, visual/ops modules. |
| Gateway | `jeecg-server-cloud/jeecg-cloud-gateway` | Spring Cloud Gateway/WebFlux, Nacos discovery/config, Redis rate limiter dependency, Sentinel gateway integration, dynamic route loader, Swagger aggregation. |
| Nacos | `jeecg-server-cloud/jeecg-cloud-nacos` | Bundled Nacos server/config module, with example config and gateway route JSON. |
| System cloud start | `jeecg-server-cloud/jeecg-system-cloud-start` | System/auth/admin service runtime for cloud mode. It explicitly excludes `jeecg-module-demo`. |
| Demo cloud start | `jeecg-server-cloud/jeecg-demo-cloud-start` | Demo service runtime. Depends on `jeecg-module-demo`; do not use for production ReHealth. |
| Visual parent | `jeecg-server-cloud/jeecg-visual` | Parent for optional cloud operational modules and test examples. |
| Sentinel dashboard | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-sentinel` | Sentinel dashboard/rule publishing to Nacos. Useful when gateway/service flow rules need ops tuning. |
| Monitor | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-monitor` | Spring Boot Admin style monitoring module. Useful later. |
| XXLJob | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-xxljob` | Distributed job scheduler admin. Useful later for retention/aggregation jobs. |
| Cloud test parent | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-test` | Test/demo modules only. |
| RabbitMQ example | `jeecg-cloud-test-rabbitmq` | Example MQ module. Starter is managed in parent, but not active in ReHealth product. |
| RocketMQ example | `jeecg-cloud-test-rocketmq` | Example MQ module. Starter is managed in parent, but not active in ReHealth product. |
| Seata example | `jeecg-cloud-test-seata` | Distributed transaction examples. Not recommended for E1. |
| ShardingSphere example | `jeecg-cloud-test-shardingsphere` | Sharding example and config. Useful later only if measured hardware volume requires it. |

## Module Classification

| Classification | Modules/capabilities | Rationale |
| --- | --- | --- |
| Required for MVP | `jeecg-boot-base-core`, `jeecg-module-system/jeecg-system-api`, `jeecg-module-system/jeecg-system-biz`, `jeecg-module-system/jeecg-system-start`, new `jeecg-boot-module/jeecg-module-rehealth` | Gives E1 auth/system integration, OpenAPI, MyBatis, dynamic datasource, and a fast monolith validation path. |
| Required for high-concurrency/microservice deployment | `jeecg-cloud-gateway`, `jeecg-cloud-nacos`, `jeecg-system-cloud-start`, new `jeecg-rehealth-mobile-cloud-start`, new `jeecg-rehealth-ingest-cloud-start` when split, Sentinel gateway/service rules, one MQ starter after selection | Needed for gateway routing, discovery/config, rate limiting, circuit breaking, service split, and burst absorption. |
| Useful later | `jeecg-cloud-monitor`, `jeecg-cloud-xxljob`, one MQ example/starter, ShardingSphere example/starter, reporting/Jimu modules, OpenAPI management | Good for operations, retention jobs, async ingestion, partition/sharding, admin analytics, and API governance after E1 proves persistence. |
| Not needed now/postpone | `jeecg-module-demo` for production, `jeecg-demo-cloud-start`, `jeecg-boot-module-airag`, Seata, both MQs at once, Java model inference, direct Java SHAP/LLM/causal attribution | Demo/AIRAG do not match product boundaries. Distributed transactions and model logic would increase risk before the MVP contract is stable. |

## Existing ReHealth Backend Code

Existing untracked path:

```text
jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/mobile/ReHealthMobileController.java
```

Observed files:

| File | Current behavior | Assessment |
| --- | --- | --- |
| `ReHealthMobileController.java` | Exposes `/rehealth/mobile/**`; has health, ring snapshot upload/latest, model-info, patient profile, risk score, intervention plan, and check-in endpoints. | Prototype only. Do not adopt as production structure. |

Findings:

- Uses `@IgnoreAuth` on all endpoints.
- Uses static in-memory state: `ConcurrentHashMap` for snapshots, `ArrayList` for check-ins, and static patient profile.
- Performs Java-side mock risk scoring with model version `rehealth-mobile-mock-0.1`.
- Has an optional HTTP call to `rehealth.algorithm.base-url + /api/pias/predict`, which does not match `model-service/docs/API_CONTRACT.md`.
- Builds intervention plans in Java mock logic.
- Persists no software business data and no hardware telemetry.
- Does not implement idempotency, upload queue acknowledgement, database ownership, or model-service client abstraction.
- It is currently untracked in git; leave it unchanged during E0.5.

Recommendation:

- Treat it as a reference for rough endpoint intent only.
- E1 should replace the design with dedicated module/package boundaries and authenticated/persisted endpoints.
- Do not migrate `@IgnoreAuth`, in-memory persistence, `/api/pias/predict`, or Java mock scoring into production code.

## Proposed Service Architecture

### E1 Monolith Deployment Shape

For E1, keep deployment simple but code as if services may split later:

```text
jeecg-system-start
  -> jeecg-system-biz
  -> jeecg-module-rehealth
       mobile API package
       ingest package
       risk orchestration package
       admin package
       model client package
       software persistence package
       hardware persistence package

model-service remains external Python FastAPI
```

This gives a single Java process for validation while preserving service boundaries inside the module.

### Target Microservice Shape

| Service | Responsibility | Database access | E1/E2 recommendation |
| --- | --- | --- | --- |
| `rehealth-mobile-service` | Android-facing account-bound APIs: profile, device binding, upload acceptance, latest risk/intervention, feedback. | `software_db`; calls ingest boundary for telemetry. | Implement package/service boundary in E1. Dedicated cloud start later. |
| `rehealth-ingest-service` | High-concurrency wearable batch ingestion, idempotency, raw batch logs, telemetry normalization, quality flags. | `hardware_db`; optional MQ producer/consumer. | E1 package boundary with sync write if DB is available. E2 separate service/MQ. |
| `rehealth-risk-orchestration-service` | Receives/loads CVD feature vectors, calls model-service risk/intervention endpoints, stores model metadata/results, exposes latest result. | `software_db`; read-only summary access to hardware-derived features if needed. | E1 package boundary and Java client abstraction. No Java scoring. |
| `rehealth-admin-service` | Admin/doctor/business views, device/user operations, operational records. | `software_db`; read-side summaries from telemetry only through service APIs/materialized summaries. | Useful later; E1 can define docs/endpoints only if required. |
| `model-service` | Python FastAPI model authority: risk, intervention, attribution, health. | No direct Jeecg database ownership. | Already external contract. Java backend calls it over HTTP. |

## Recommended Module Placement

Create a dedicated product module:

```text
jeecg-boot/jeecg-boot-module/jeecg-module-rehealth
```

Suggested package boundaries:

```text
org.jeecg.modules.rehealth.mobile
org.jeecg.modules.rehealth.device
org.jeecg.modules.rehealth.ingest
org.jeecg.modules.rehealth.telemetry
org.jeecg.modules.rehealth.feature
org.jeecg.modules.rehealth.risk
org.jeecg.modules.rehealth.intervention
org.jeecg.modules.rehealth.feedback
org.jeecg.modules.rehealth.admin
org.jeecg.modules.rehealth.model
```

Future cloud start modules:

```text
jeecg-boot/jeecg-server-cloud/jeecg-rehealth-mobile-cloud-start
jeecg-boot/jeecg-server-cloud/jeecg-rehealth-ingest-cloud-start
```

For E2, one combined `jeecg-rehealth-cloud-start` is acceptable before splitting mobile/risk/ingest processes.

## Gateway And Nacos Fit

Current gateway config supports:

- Nacos config/discovery.
- Dynamic route loading.
- Redis-backed rate limiting dependency.
- Sentinel gateway flow/degrade/system/authority/param-flow/API rules via Nacos.
- Existing routes for `/sys/**`, `/jmreport/**`, `/online/**`, `/generic/**`, `/mock/**`, `/test/**`, and websocket paths.

Future ReHealth route:

```json
{
  "id": "jeecg-rehealth",
  "order": 10,
  "predicates": [
    {
      "name": "Path",
      "args": {
        "_genkey_0": "/rehealth/**"
      }
    }
  ],
  "filters": [],
  "uri": "lb://jeecg-rehealth"
}
```

E1 should not edit gateway/Nacos unless explicitly approved. E1 should document the route if it adds cloud docs.

## Model-Service Integration Boundary

Accepted Python service endpoints from `model-service/docs/API_CONTRACT.md`:

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /v1/cvd/attribution/individual`

Backend requirements:

- Add `ModelServiceClient` abstraction when E1 is approved.
- Configure base URL through app config/Nacos.
- Map Android CVD 16 feature vector fields without renaming stable model-service response fields unless DTOs explicitly document the mapping.
- Persist `risk_score`, `risk_level`, `feature_contributions`, `model_version`, `is_mock`, `missing_fields`, `quality_warnings`, and conservative `summary`.
- Never implement CatBoost, SHAP, LLM generation, or causal attribution in Java.
- Any dev fallback must be explicitly named development-only and must not masquerade as model output.

## Revised E1 Implementation Plan

E1 should implement now:

- Dedicated ReHealth module skeleton and monolith wiring only if approved after this review.
- Auth-compatible mobile API endpoints under `/rehealth/mobile/**`.
- Database entities/mappers/services for MVP software tables and hardware batch tables if local datasource access is available.
- Batch/idempotent telemetry upload contract.
- Feature vector submission/evaluation orchestration through `ModelServiceClient`.
- Latest risk/intervention retrieval from persisted records.
- Feedback persistence.
- OpenAPI/Swagger annotations and backend docs for endpoint contracts.

Move to E2:

- Separate ReHealth cloud start module and gateway/Nacos route changes.
- MQ producer/consumer ingestion path.
- Sentinel rate/degrade rule tuning.
- Hardware time-series/ClickHouse migration.
- ShardingSphere partitioning/sharding.
- Admin/doctor dashboards and reporting.
- Distributed job retention/aggregation with XXLJob.

Code E1 should not write yet:

- Production code in `jeecg-module-demo`.
- Java model scoring, SHAP, LLM, attribution, or intervention generation logic.
- Direct Android/model-service/rehealth-algorithms changes.
- Gateway/Nacos route edits unless separately approved.
- MQ, Seata, ShardingSphere production wiring.
- Broad admin/report UI.

Exact allowed files for E1, if approved:

```text
backend/jeecg-boot/pom.xml
backend/jeecg-boot/jeecg-boot-module/pom.xml
backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/**
backend/jeecg-boot/jeecg-module-system/jeecg-system-start/pom.xml
backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/**
backend/docs/**
backend/codex-runs/<date>/**
```

Conditionally allowed after explicit approval:

```text
backend/jeecg-boot/jeecg-server-cloud/**
backend/jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/**
```

Exact MVP endpoint list for E1:

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| `GET` | `/rehealth/mobile/health` | Lightweight backend health/config check. | May allow explicit dev/no-auth only if documented. |
| `GET` | `/rehealth/mobile/config` | Mobile feature flags, upload limits, server time, model-service status summary. | Authenticated. |
| `POST` | `/rehealth/mobile/devices/bind` | Bind ring/device to current user. | Authenticated. |
| `GET` | `/rehealth/mobile/profile` | Read ReHealth user profile/interview summary. | Authenticated. |
| `PUT` | `/rehealth/mobile/profile` | Update profile/interview fields. | Authenticated. |
| `POST` | `/rehealth/mobile/telemetry/batches` | Idempotent upload of wearable measurement batch. | Authenticated. |
| `POST` | `/rehealth/mobile/features/evaluate` | Submit or load CVD 16 feature vector and call model-service risk evaluation. | Authenticated. |
| `GET` | `/rehealth/mobile/risk/latest` | Return latest persisted risk result. | Authenticated. |
| `GET` | `/rehealth/mobile/interventions/today` | Return persisted/generated current intervention plan. | Authenticated. |
| `POST` | `/rehealth/mobile/interventions/{id}/feedback` | Persist intervention feedback. | Authenticated. |
| `POST` | `/rehealth/mobile/attribution/events` | Store attribution/behavior events or forward to model-service when ready. | Authenticated; can be docs-only in E1 if not implemented. |

Exact docs E1 should update:

```text
backend/docs/BACKEND_MODULE_SELECTION.md
backend/docs/DATABASE_SPLIT_ARCHITECTURE.md
backend/docs/REHEALTH_MOBILE_API.md
backend/docs/REHEALTH_DB_SCHEMA.md
backend/codex-runs/<date>/E1_status.md
```

## Validation Recommendation

For E0.5, no build is needed because only docs are changed.

For E1, run a targeted Maven validation from `backend/jeecg-boot` after code changes, for example:

```powershell
mvn -pl jeecg-boot-module/jeecg-module-rehealth -am test
mvn -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
```

Adjust exact commands after the new module exists.
