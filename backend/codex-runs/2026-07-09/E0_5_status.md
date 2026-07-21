# E0.5 Backend Module Selection And Database Split Status

Date: 2026-07-09  
Status: complete  
Scope: documentation-only architecture review. No Java/source implementation changed.

## Inputs Inspected

- `D:/rehealthAI/AGENTS.md`
- `D:/rehealthAI/ENGINEERING.md`
- `D:/rehealthAI/CODEX_ORCHESTRATION.md`
- `D:/rehealthAI/.agents/skills/rehealth-android-mvp/SKILL.md`
- `D:/rehealthAI/backend/README.md`
- `D:/rehealthAI/backend/jeecg-boot/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-boot-base-core/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-boot-module/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-boot-module/jeecg-module-demo/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-module-system/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-module-system/jeecg-system-api/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-module-system/jeecg-system-start/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-gateway/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-gateway/src/main/resources/application.yml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-nacos/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-nacos/docs/config/jeecg-gateway-router.json`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-demo-cloud-start/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-server-cloud/jeecg-visual/pom.xml`
- `D:/rehealthAI/backend/jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/mobile/ReHealthMobileController.java`
- `D:/rehealthAI/model-service/docs/API_CONTRACT.md`
- `D:/rehealthAI/Android-apk/docs/FEATURE_EXTRACTOR.md`

## Existing Implementation Summary

- Backend is a JeecgBoot 3.9.2 repository with monolith and cloud deployment structures.
- MVP-relevant modules are base core, system API/business/start, and a future dedicated ReHealth module.
- Cloud modules present locally include gateway, Nacos, system cloud start, demo cloud start, monitor, Sentinel, XXLJob, and test/example modules for RabbitMQ, RocketMQ, Seata, and ShardingSphere.
- The only current ReHealth Java code is an untracked prototype controller under `jeecg-module-demo`.
- The prototype uses `@IgnoreAuth`, static in-memory maps/lists, Java mock scoring, obsolete `/api/pias/predict` model call shape, and no persistence.
- JeecgBoot base core already includes dynamic datasource support, which fits a `software_db` plus `hardware_db` split.
- Accepted model-service endpoints are `/health`, `/v1/cvd/risk/evaluate`, `/v1/cvd/intervention/generate`, and `/v1/cvd/attribution/individual`.

## Documents Written

- `D:/rehealthAI/backend/docs/BACKEND_MODULE_SELECTION.md`
- `D:/rehealthAI/backend/docs/DATABASE_SPLIT_ARCHITECTURE.md`
- `D:/rehealthAI/backend/codex-runs/2026-07-09/E0_5_status.md`

## Recommendation Captured

- Create/use `jeecg-boot-module/jeecg-module-rehealth` for production ReHealth code.
- Keep `jeecg-module-demo` and the current untracked controller as prototype/reference only.
- Use `jeecg-system-start` plus the ReHealth module for E1 monolith validation.
- Add ReHealth cloud start module(s), gateway route `/rehealth/**`, Sentinel rules, and MQ ingestion in E2/high-concurrency deployment.
- Split data ownership into `software_db` and `hardware_db`.
- Use synchronous `hardware_db` writes for E1 only if datasource access exists; add MQ later for burst/high-concurrency ingestion.
- Use a Java `ModelServiceClient` abstraction to call Python `model-service`; do not implement clinical/model scoring in Java.

## Revised E1 Gate

E1 should implement only after this review is accepted:

- Dedicated ReHealth module/package boundaries.
- Authenticated mobile API contract under `/rehealth/mobile/**`.
- Batch/idempotent telemetry upload.
- MVP persistence across software and hardware ownership domains, or explicit documented limitation if local datasource setup blocks it.
- Model-service client abstraction and persistence of model response metadata.
- Docs for endpoint contracts and schema/migration strategy.

E1 should not implement:

- Production code in `jeecg-module-demo`.
- Java CatBoost, SHAP, LLM, causal attribution, or mock scoring as production behavior.
- Gateway/Nacos route edits unless separately approved.
- MQ/Seata/ShardingSphere production wiring.
- Android, model-service, or rehealth-android changes.

## Validation

Commands run:

```powershell
Get-Content -Raw <required guidance and target files>
rg --files backend/jeecg-boot/jeecg-server-cloud
rg --files backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-gateway
rg --files backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-nacos
rg --files backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start
rg --files backend/jeecg-boot/jeecg-server-cloud/jeecg-demo-cloud-start
rg --files backend/jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth
rg -n "IgnoreAuth|mock|Mock|in-memory|memory|ConcurrentHashMap|Map<|new ArrayList|static final" backend/jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth -S
rg -n "Module Classification|Proposed Service Architecture|Revised E1 Implementation Plan|Exact MVP endpoint|Existing ReHealth" docs/BACKEND_MODULE_SELECTION.md
rg -n "software_db|hardware_db|Android Measurement Upload Flow|Hardware Data Ingestion Flow|Latest Data Cache Flow|Model-Service Call Flow|Revised E1 Database Scope" docs/DATABASE_SPLIT_ARCHITECTURE.md
git status --short
git status --short  # in Android-apk, model-service, rehealth-android
```

No Maven/Gradle/pytest build was run because this task changed documentation only and did not modify Java, Android, model-service, or rehealth-android source.

## Git Status Before Commit

Backend status after documentation update:

```text
?? codex-runs/
?? docs/
?? jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/
```

The untracked Java prototype existed before this documentation update and remains unchanged.

Other repos checked:

```text
Android-apk: clean
model-service: clean
rehealth-android: clean
```

## Risks

- Physical database names, credentials, and deployment topology are not configured yet.
- E1 must decide whether local MySQL can provide both datasources or whether persistence is partially blocked.
- The untracked demo controller may confuse future E1 work unless it is explicitly ignored, removed, or migrated after approval.
- Gateway route and cloud-start changes require a separate approval/deployment step.
- MQ choice is still open; do not introduce RabbitMQ and RocketMQ together.
