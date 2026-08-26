# ReHealth 后端 API E1

状态：E1 实现契约。
模块：`jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`。

## 模块边界

ReHealth 生产后端代码位于 `jeecg-module-rehealth`。

在确认所有生产 ReHealth API 均位于专用模块后，已移除过时的 Demo 模块及其早期原型路由位置。

## 端点列表

单体模式下的基础路径：

```text
/jeecg-boot/rehealth/mobile
```

移动端点（以 `ReHealthMobileController` 及各专项控制器为准；逐字段契约见
[`MOBILE_API.md`](./MOBILE_API.md)）：

```text
GET  /rehealth/mobile/health                       （唯一 @IgnoreAuth 健康检查）
GET  /rehealth/mobile/config
GET  /rehealth/mobile/profile
PUT  /rehealth/mobile/profile
POST /rehealth/mobile/interviews
GET  /rehealth/mobile/interviews/latest
POST /rehealth/mobile/devices/bind
POST /rehealth/mobile/measurements/batch
GET  /rehealth/mobile/measurements/recent
POST /rehealth/mobile/viomi/bind
POST /rehealth/mobile/viomi/sync
POST /rehealth/mobile/features/evaluate
POST /rehealth/mobile/rhi/evaluate-series
GET  /rehealth/mobile/rhi/manual-inputs
PUT  /rehealth/mobile/rhi/manual-inputs
POST /rehealth/mobile/rhi/daily-snapshot
POST /rehealth/mobile/rdi/daily-snapshot
GET  /rehealth/mobile/risk/latest
GET  /rehealth/mobile/risk/history
POST /rehealth/mobile/interventions/generate
GET  /rehealth/mobile/interventions/today
POST /rehealth/mobile/interventions/{id}/feedback
POST /rehealth/mobile/attribution/events
POST /rehealth/mobile/agent/messages
GET  /rehealth/mobile/agent/conversations/latest
POST /rehealth/mobile/behavior-records/analyze-photo
GET  /rehealth/mobile/behavior-records/today
POST /rehealth/mobile/insurance/plans/bind
GET  /rehealth/mobile/insurance/plans/bindable-policies
GET  /rehealth/mobile/insurance/plans/current
GET  /rehealth/mobile/insurance/plans/active
POST /rehealth/mobile/insurance/plans/{bindingId}/feedback
GET  /rehealth/mobile/insurance/care-plans/current
POST /rehealth/mobile/insurance/care-plan-occurrences/{occurrenceId}/feedback
GET  /rehealth/mobile/insurance/assignments/current
```

其他 ReHealth 模块端点：

```text
POST /rehealth/viomi/report                        （云米主动上报回调，JWT HS256 验签）
POST /rehealth/website/v1/{resource} 及 GET/{id}、DELETE/{id}   （官网 BFF 业务记录）
GET/POST/PUT /rehealth/insurance/v1/**             （保险风险、干预工作台、机构计划、导入、研究、报告、结算、机构设置、服务关系与计划目录）
POST /rehealth/internal/v1/identity/authorize-device （Device Service 内部设备授权）
GET  /rehealth/account/password/status             （员工密码状态）
PUT  /rehealth/account/password                    （当前账号自助修改密码）
GET  /rehealth/admin/v1/patients、/patients/{patientId} （官网患者只读聚合，位于 jeecg-system-biz）
```

其中保险侧 `GET/POST /rehealth/insurance/v1/plans` 为计划目录（`InsurancePlanCatalogController`，权限
`rehealth:insurance:care-plan:view/manage`）；服务关系端点
（`/rehealth/insurance/v1/assignments/*`）与移动端 `plans/bindable-policies`、`assignments/current`
的逐字段契约见 [`INSURANCE_BUSINESS_API.md`](../contracts/INSURANCE_BUSINESS_API.md)。

## Website BFF business records

The corporate website FastAPI BFF does not connect to PostgreSQL or MySQL. It
forwards the authenticated Jeecg token and tenant header to these JeecgBoot
endpoints; the records are stored in the MySQL `software_db` table
`rehealth_website_record` by the `jeecg-module-rehealth` module.

```text
POST   /rehealth/website/v1/{patients|attributions|settlements|screening}
GET    /rehealth/website/v1/{resource}
GET    /rehealth/website/v1/{resource}/{id}
DELETE /rehealth/website/v1/{resource}/{id}
```

All calls require normal Jeecg authentication and tenant scope (`X-Tenant-Id`);
the resource allow-list is enforced server-side.

## Insurer read API

The insurer website BFF forwards the authenticated token and tenant header to
the Jeecg insurance risk bridge:

```text
GET /rehealth/insurance/v1/dashboard/risk
GET /rehealth/insurance/v1/insureds
GET /rehealth/insurance/v1/insureds/{subjectId}
```

The dashboard response keeps the existing risk metrics and adds
`business_summary` (`active_policies`, `active_coverages`, `claim_count`,
`billed_amount`, `paid_amount`, `active_interventions` and
`latest_updated_at`). Subject detail adds the same tenant-scoped business
summary plus `consent_status` and `intervention_status`. The first-phase
business summary is read through MyBatis-Plus mappers in the Java service; the
FastAPI BFF only normalizes and forwards the response.

## Insurer workflow API

The insurer business lifecycle is implemented under
`/rehealth/insurance/v1`. JeecgBoot/MySQL remains authoritative; the website
FastAPI BFF parses CSV/XLSX files, runs PSM against an immutable snapshot and
writes every state transition back through authenticated Java APIs.

```text
POST /imports/{subjects|policies|claims}
GET/POST /studies
POST /studies/{studyId}/snapshots
GET /study-snapshots/{snapshotId}
POST /studies/{studyId}/jobs
GET /study-jobs/{jobId}
PUT /study-jobs/{jobId}/result
POST /study-results/{resultId}/review
GET /reports
POST /studies/{studyId}/reports
POST /reports/{reportId}/review
GET /settlements
POST /studies/{studyId}/settlements
POST /settlements/{packageId}/actions
```

Every route uses the authenticated user, active tenant membership and a
least-privilege `rehealth:insurance:*` permission. IDs are always looked up
with `tenant_id`; a path ID from another tenant returns `403` or an absent
resource. Import batches are idempotent by tenant, import type and
`idempotencyKey`, while external claim numbers are tenant-unique. Detailed
request fields, role mappings and workflow states are maintained in
`backend/contracts/INSURANCE_BUSINESS_API.md`.

只有 `/health` 标记了 `@IgnoreAuth`。其他移动端点应使用 JeecgBoot 的常规认证与授权机制。

## 公司官网本地登录

公司官网不使用 Android 的 `/sys/mLogin`，也不复用管理后台 PC 登录的单点会话槽位：

```text
POST /sys/webLogin
```

请求体沿用 `SysLoginModel` 的 `username`/`password`；`username` 可为唯一登录账号或唯一邮箱。
端点复用 Jeecg 用户有效性检查、失败次数锁定、租户选择和密码校验，以固定 `WEB` 客户端类型
签发 JWT，并额外返回当前租户范围内的 `roles` 与 `permissions`。`WEB` 使用独立的 Redis
单点登录键，不会踢出同一用户的 PC 或 APP 会话。响应中的 Jeecg Token 只允许由官网 FastAPI
BFF 在服务端持有；不得保存到浏览器、日志或官网 PostgreSQL。

本地联调阶段保留官网现有滑块交互，但它不是服务端可验证的人机证明。正式发布前必须补充
服务端验证码/风控、HTTPS Secure Cookie、生产密钥管理和完整安全验收。邮箱验证码登录和机构
自主注册当前明确不支持。

## Model Service 契约

`ModelServiceClient` 是 Java 调用模型服务的唯一边界。

配置属性：

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

调用：

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /v1/cvd/attribution/individual`（仅非 PIAS 兼容回退）

生产归因默认 `attributionMode=pias`，经独立配置的
`rehealth.attribution-service.base-url` 调用 PIAS
`POST /api/pias/v2/attribute/individual`；`/v1/cvd/attribution/individual`
只是该模式下未启用时的兼容目标。

Java 后端不实现 CatBoost、SHAP、CVD 评分或归因逻辑。移动端干预端点是为 LangChain4j 明确保留的例外：Jeecg 组装持久化的权威上下文并生成结构化、保守的健康行动，但不进行诊断、调整用药或推断因果治疗效果。

每次调用 `POST /rehealth/mobile/interventions/generate` 前，Jeecg 都会从 `software_db` 重新加载当前认证用户的档案、最新访谈和风险，并携带租户、时区和内部凭据调用 Device Service：
`GET /rehealth/internal/v1/operations/users/{userId}/intervention-context`。
Device Service 从 TimescaleDB 读取今日活动、睡眠、测量、饮食及有界的近期变化。客户端提供的档案或风险上下文会被忽略。

## 数据库拆分状态

E1 定义的软件库/硬件库边界已实现：

`software_db` 边界：

- 由 `jeecg-module-rehealth` 通过 `ReHealthBusinessRepository` 系列实现读写；
- 档案、访谈、绑定、RHI/RDI 手工输入与日快照、风险、干预、反馈、行为记录、
  保险业务和官网业务记录均已持久化；
- 迁移见 `src/main/resources/db/software/mysql/`，当前最新为
  `V20260821_1__add_password_management.sql`；`software_db` 禁用时相关读写返回可重试的 `503`。

`hardware_db` 边界：

- 遥测权威已迁移到独立 Device Service/TimescaleDB（E2.1 完成），
  `POST /rehealth/mobile/measurements/batch` 经 Gateway 路由到 Device Service，
  由 `HardwareTelemetryIngestionService` 在单个 TimescaleDB 事务中写入批次、测量、
  睡眠、活动和饮食并提交 Outbox；
- `jeecg-module-rehealth` 内保留的 `HardwareIngestionPort` MySQL 实现仅为
  legacy/本地联调兼容路径，不再对外可达（路由优先于 Jeecg 通配路由）；
- TimescaleDB 迁移见 `device-service/src/main/resources/db/migration/timescale/`，
  当前最新为 `V4__create_diet_behavior_records.sql`。

遥测上传通过 `HardwareIngestionPort` 路由，不直接写入普通业务表。

## D1 集成说明

后端配置指向正在运行的 model-service 后，Android D1 即可使用 `/features/evaluate`。
`/measurements/batch` 只有返回 `accepted=true`、`persisted=true`、状态以 `ACCEPTED_` 开头时
才视为持久化同步完成；重复批次返回 `ACCEPTED_DUPLICATE` 和原始收据。
