# ReHealth 数据库 Schema

状态：`software_db` 和 `hardware_db` 的 MVP Schema 脚本已实现；生产环境的资源置备仍由部署流程负责。

## 决策

ReHealth 使用两个逻辑数据库：

- `software_db`：用户、业务和应用记录。
- `hardware_db`：高容量可穿戴遥测和接入记录。

ReHealth 模块为两个数据库域提供按条件启用的 JDBC 写入器。在对应 Schema 和数据源配置完成前，写入器保持禁用。

## `software_db` 边界

所有者：ReHealth 移动端、风险和管理业务代码，以及现有 Jeecg 系统账号/认证表。

E1 Java 边界：

```text
org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.impl.E1PendingSoftwareDbReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.impl.JdbcSoftwareDbReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.BehaviorRecordRepository
org.jeecg.modules.rehealth.repository.impl.JdbcBehaviorRecordRepository
```

规划的数据表：

| 表 | 用途 | E1 状态 |
| --- | --- | --- |
| `rehealth_device_binding` | 用户与设备绑定。 | 已实现。 |
| `rehealth_patient_profile` 及诊断/用药/过敏表 | 类型化 ReHealth 档案字段和有序病史条目。 | 已通过认证的 `GET/PUT /profile` 实现；BMI 由服务端计算，`profile_version` 提供乐观锁。 |
| `rehealth_health_interview` 及回答/基线/关注项表 | 类型化访谈主记录和有序的回答/业务档案行。 | 已通过认证的 `POST /interviews` 和 `GET /interviews/latest` 实现。 |
| `rehealth_cvd_feature_vector` | CVD 16 维向量和特征质量元数据。 | 已通过 `/features/evaluate` 实现。 |
| `rehealth_cvd_risk_result` | 风险分数、等级、模型贡献、Factor16 规则贡献/分量、模型版本、缺失字段、警告和摘要。 | 已实现按用户读取最新记录。 |
| `rehealth_intervention_plan` 及禁忌行 | 可查询的保守干预字段和原始模型证据快照。 | 已实现按用户读取最新记录。 |
| `rehealth_intervention_feedback` | 用户反馈、依从性和打卡。 | 已通过 `/interventions/{id}/feedback` 实现。 |
| `rehealth_attribution_result` | PIAS 请求和结果快照。 | 已通过 `/attribution/events` 实现。 |
| `rehealth_behavior_record` | 认证的拍照食物/OCR 记录；只保存已验证的结构化输出，绝不保存原始图片字节。 | 已通过 `/behavior-records/analyze-photo` 和 `/behavior-records/today` 实现；按所有者限定的 `request_id` 保证幂等。 |
| `rehealth_model_request_log` | 不含原始 PII 或原始遥测载荷的最小请求元数据。 | 已用于风险、干预和归因模型调用。 |
| `rehealth_upload_batch` | 软件侧上传状态和物化摘要。 | 已延期。 |

事务策略：

- 只在单个软件业务聚合内部使用强一致性。
- 不与 `hardware_db` 建立跨库事务。
- 不记录原始健康数据、令牌、手机号或标识符。

结构化运营字段使用类型化列或有序子表。仅当模型重放、审计证据、厂商扩展元数据或持久化队列重试需要完整版本化载荷时才保留 JSON。应用读取档案、访谈、风险摘要和干预字段时不依赖整对象 JSON 文档。模型特征向量及原始请求/响应证据则有意保留为 JSON 快照。

## `hardware_db` 边界

所有者：`backend/device-service`；未来仍可选择拆分专用接入服务以扩展容量。

PostgreSQL 17 / TimescaleDB Schema 由 `backend/device-service/src/main/resources/db/migration/timescale` 下的 Flyway 迁移管理版本。任务 8 负责遥测端口适配器；仅启用迁移并不会让当前尚不可用的适配器自动就绪。

| 表 | 用途 |
| --- | --- |
| `hardware_upload_batch` | 幂等上传收据和批次状态。 |
| `hardware_measurement` | 规范化标量测量；按 `observed_at` 形成一天分块。 |
| `hardware_sleep_session` | 规范化睡眠会话；按 `started_at` 形成七天分块。 |
| `hardware_activity` | 规范化活动会话；按 `started_at` 形成七天分块。 |
| `hardware_signal_chunk_metadata` | 仅保存信号元数据，不保存原始波形载荷。 |
| `hardware_data_quality_event` | 质量事件；按 `event_at` 形成七天分块。 |
| `hardware_reconciliation` | 对账状态和重试元数据。 |
| `hardware_outbox` | 持久化发布状态。 |
| `hardware_migration_checkpoint` | 旧数据迁移检查点。 |

所有领域时间均使用 `TIMESTAMPTZ`。来源唯一性由租户、用户、设备、事件时间、记录类型和来源记录 ID 共同确定。遥测、会话和质量 Hypertable 在七天后压缩分块。

| 数据类别 | 默认保留期 | 配置 |
| --- | --- | --- |
| 测量、睡眠、活动 | 730 天 | `REHEALTH_MEASUREMENT_RETENTION_DAYS` |
| 信号元数据 | 90 天 | `REHEALTH_SIGNAL_METADATA_RETENTION_DAYS` |
| 质量和运营历史 | 1,095 天 | `REHEALTH_OPERATIONAL_RETENTION_DAYS` |
| 已发布的 Outbox 行 | 30 天 | `REHEALTH_PUBLISHED_OUTBOX_RETENTION_DAYS` |

普通表生命周期任务只删除已进入终态的数据。失败或未解决的 Outbox 记录绝不自动删除；存在未完成对账或 Outbox 工作的上传批次也会保留。

## 资源置备

新建数据库时，将 `db/software/mysql/V1__create_rehealth_software_tables.sql` 应用于 Jeecg 主软件数据源。对于使用仅 JSON 档案/访谈 Schema 创建的现有数据库，应先备份数据库，再于部署匹配的应用前应用 `V20260729_1__normalize_business_records.sql`。

该升级会新增类型化列和子表、回填有效旧 JSON、保留可空的旧 JSON 列以便回滚，并在 `rehealth_schema_migration` 中记录 `software-V20260729.1`。应用以下迁移：

- `V20260730_1__add_health_agent_conversations.sql`：认证的 AI 历史；
- `V20260731_1__add_behavior_records.sql`：拍照生成的行为记录；
- `V20260731_2__add_factor16_contributions.sql`：独立版本化的 Factor16 规则贡献字段。

停用旧列前，应验证行数和无效 JSON。随后设置 `rehealth.software-db.enabled=true`。每次移动业务读写都从已认证的 Jeecg 用户派生数据归属，不接受客户端为这些记录提供用户 ID。

对于 `hardware_db`，设置 `REHEALTH_HARDWARE_DB_ENABLED=true`、`REHEALTH_HARDWARE_DB_URL`、`REHEALTH_HARDWARE_DB_USERNAME`，以及 `REHEALTH_HARDWARE_DB_PASSWORD` 或 `REHEALTH_HARDWARE_DB_PASSWORD_FILE`。

启动过程会在创建任何硬件写入适配器前验证并应用 Timescale Flyway 迁移。未安装 Timescale 扩展的 PostgreSQL 会在前置迁移阶段失败，不会写入应用数据表。

旧 MySQL `DATETIME(3)` 值在转换为 `TIMESTAMPTZ` 前，由 `rehealth_legacy_mysql_datetime_utc(timestamp)` 按 UTC 解释；迁移调用方不得再应用服务器会话时区。
