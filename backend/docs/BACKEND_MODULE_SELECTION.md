# Backend Module Selection

Last reviewed: 2026-07-27

JeecgBoot provides account, tenant, permission, administration, and ReHealth business
orchestration. Python `model-service` remains the authority for CatBoost, SHAP, health
assistant generation, and attribution.

## Current modules

| Module | Decision | Responsibility |
| --- | --- | --- |
| `jeecg-boot-base-core` | Keep | Shared auth, web, validation, MyBatis, datasource, and framework support. |
| `jeecg-module-system/jeecg-system-api` | Keep | Local and cloud system contracts. |
| `jeecg-module-system/jeecg-system-biz` | Keep | Users, tenants, permissions, menus, dictionaries, and platform administration. |
| `jeecg-module-system/jeecg-system-start` | Keep | Monolith launcher for local development and MVP validation. |
| `jeecg-boot-module/jeecg-module-rehealth` | Keep | ReHealth mobile APIs, persistence, model clients, intervention, feedback, and operations. |
| `jeecg-boot-module/jeecg-boot-module-airag` | Keep | Jeecg AI/RAG platform capability required by `jeecg-system-biz`; it is not the ReHealth model authority. |
| `jeecg-server-cloud/jeecg-cloud-gateway` | Keep | Optional cloud gateway and route aggregation. |
| `jeecg-server-cloud/jeecg-cloud-nacos` | Keep | Optional discovery and configuration service. |
| `jeecg-server-cloud/jeecg-system-cloud-start` | Keep | Optional cloud-mode system/ReHealth launcher. |
| `jeecg-server-cloud/jeecg-visual` | Keep | Optional monitoring, Sentinel, XXLJob, and upstream cloud examples. |
| `backend/jeecgboot-vue3` | Keep | JeecgBoot management frontend. |

## Removed modules

- `jeecg-module-demo`: upstream sample controllers, mock endpoints, sample entities,
  static big-screen assets, and Demo test data. Inspection found no ReHealth code or
  product dependency.
- `jeecg-demo-cloud-start`: launcher that existed only to expose `jeecg-module-demo`.

The system launchers no longer depend on or exclude the removed artifact. Their code
generator defaults now target `jeecg-module-rehealth` instead of a machine-local Demo
path. Existing databases may still contain upstream Demo tables or menu rows; this source
cleanup does not perform a destructive production database migration.

## Service and data boundaries

```text
Android
  -> Gateway (optional)
  -> JeecgBoot ReHealth APIs
       -> software_db for account and business records
       -> model-service for risk/intervention/assistant

Android telemetry
  -> Device Service
       -> TimescaleDB durable write + Outbox
       -> Kafka persistence/quality events
```

- JeecgBoot must not write Device Service-owned TimescaleDB tables directly.
- Model inference and attribution must stay behind Java client abstractions.
- User and tenant identity comes from the authenticated server context, never a request
  body ownership field.
- Telemetry success is returned only after durable persistence and idempotency checks.

## Build validation

From the repository root:

```powershell
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am test
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
mvn -f backend/jeecg-boot/jeecg-server-cloud/pom.xml -pl jeecg-system-cloud-start -am package -DskipTests
```

Any future module addition or removal must update the root `README.md`, this document,
deployment topology, and `STATUS.md` when release scope changes.
