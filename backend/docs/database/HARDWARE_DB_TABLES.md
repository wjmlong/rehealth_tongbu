# TimescaleDB hardware_db 数据库逐表结构

> 本文件由 `tools/generate_database_schema_docs.py` 根据只读结构元数据生成。
> 不包含数据库账号、密码、业务行内容或原始健康数据。

结构来自运行中的 `rehealth_hardware`（PostgreSQL 17.5 / TimescaleDB 2.21.1），共 11 张基础表。

## 表清单

| 序号 | 表名 | 中文名称 | 模块 | 主要用途 | 核心表 |
| ---: | --- | --- | --- | --- | --- |
| 1 | [`flyway_schema_history`](#flyway-schema-history) | Flyway 迁移历史表 | 迁移元数据 | 记录 Flyway 数据库迁移执行历史；不是业务数据。 | 否（迁移元数据） |
| 2 | [`hardware_activity`](#hardware-activity) | 硬件活动表 | 硬件时序事实 | 保存规范化活动、步数、距离、热量、时长和心率。 | 是 |
| 3 | [`hardware_data_quality_event`](#hardware-data-quality-event) | 硬件数据质量事件表 | 硬件信号与质量 | 保存遥测质量事件、严重程度和详情码。 | 是 |
| 4 | [`hardware_diet_record`](#hardware-diet-record) | 硬件域饮食行为表 | 硬件时序事实 | 保存随 telemetry-v2 批次提交的规范化饮食行为。 | 是 |
| 5 | [`hardware_measurement`](#hardware-measurement) | 硬件标量测量表 | 硬件时序事实 | 保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。 | 是 |
| 6 | [`hardware_migration_checkpoint`](#hardware-migration-checkpoint) | 硬件迁移检查点表 | 硬件迁移对账 | 保存旧 MySQL 硬件数据迁移位置、行数、哈希和校验状态。 | 否（迁移支持） |
| 7 | [`hardware_outbox`](#hardware-outbox) | 遥测事务 Outbox 表 | 硬件可靠性 | 与遥测事实同事务写入，随后可靠发布隐私安全 Kafka 事件。 | 是 |
| 8 | [`hardware_reconciliation`](#hardware-reconciliation) | 硬件批次对账表 | 硬件可靠性 | 保存每个上传批次唯一的对账状态、重试和人工处理元数据。 | 是 |
| 9 | [`hardware_signal_chunk_metadata`](#hardware-signal-chunk-metadata) | 硬件信号元数据表 | 硬件信号与质量 | 只保存信号时间窗、采样率和质量元数据，不保存原始波形。 | 是 |
| 10 | [`hardware_sleep_session`](#hardware-sleep-session) | 硬件睡眠会话表 | 硬件时序事实 | 保存规范化睡眠会话和阶段分钟数。 | 是 |
| 11 | [`hardware_upload_batch`](#hardware-upload-batch) | 硬件上传批次表 | 硬件上传批次 | 保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。 | 是 |

## 模块统计

| 模块 | 表数 |
| --- | ---: |
| 硬件上传批次 | 1 |
| 硬件信号与质量 | 2 |
| 硬件可靠性 | 2 |
| 硬件时序事实 | 4 |
| 硬件迁移对账 | 1 |
| 迁移元数据 | 1 |

## 1. 表：`flyway_schema_history` Flyway 迁移历史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `flyway_schema_history` |
| 中文名称 | Flyway 迁移历史表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 迁移元数据 |
| 业务作用 | 记录 Flyway 数据库迁移执行历史；不是业务数据。 |
| 主键 | `installed_rank` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 4 行（精确计数，非生产容量） |
| 是否核心表 | 否（迁移元数据） |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `installed_rank` | 待确认 | `integer` | 32,0 | 否 | `无/NULL` | 是 | 否 | flyway_schema_history_pk | flyway_schema_history_pk | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `version` | 版本 | `character varying(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录或配置版本；是否为乐观锁需结合实体 @Version 判断。 |
| 3 | `description` | 描述 | `character varying(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前记录的业务内容描述。 |
| 4 | `type` | 类型 | `character varying(20)` | 20 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。 |
| 5 | `script` | 待确认 | `character varying(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `checksum` | 待确认 | `integer` | 32,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `installed_by` | 待确认 | `character varying(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `installed_on` | 日期 | `timestamp without time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务日期待确认。 |
| 9 | `execution_time` | 待确认 | `integer` | 32,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `success` | 待确认 | `boolean` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | flyway_schema_history_s_idx | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `flyway_schema_history_pk` | `installed_rank` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `flyway_schema_history_s_idx` | `success` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

记录 Flyway 数据库迁移执行历史；不是业务数据。

## 2. 表：`hardware_activity` 硬件活动表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_activity` |
| 中文名称 | 硬件活动表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件时序事实 |
| 业务作用 | 保存规范化活动、步数、距离、热量、时长和心率。 |
| 主键 | `id, started_at` |
| 存储引擎 | TimescaleDB Hypertable |
| 数据量级 | 当前本地实例 15 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_activity_pkey | hardware_activity_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_activity_source | idx_hardware_activity_scoped_recent、uq_hardware_activity_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_activity_source | idx_hardware_activity_scoped_recent、uq_hardware_activity_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_activity_source | idx_hardware_activity_scoped_recent、uq_hardware_activity_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_activity_source | uq_hardware_activity_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `started_at` | 开始时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_activity_pkey、联合唯一:uq_hardware_activity_source | hardware_activity_pkey、hardware_activity_started_at_idx、idx_hardware_activity_scoped_recent、uq_hardware_activity_source | 否 | 否 | CHECK (((ended_at IS NULL) OR (ended_at >= started_at))) | 会话、活动或信号时间窗开始时间。 |
| 8 | `ended_at` | 结束时间 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK (((ended_at IS NULL) OR (ended_at >= started_at))) | 会话、活动或信号时间窗结束时间。 |
| 9 | `activity_type` | 活动类型 | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_activity_source | uq_hardware_activity_source | 否 | 否 | — | 标识活动记录的类型；具体允许值由设备 Provider 映射定义。 |
| 10 | `steps` | 步数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((steps >= 0)) | 活动时间窗或自然日内的设备步数。 |
| 11 | `distance_meters` | 距离 | `numeric(20,3)` | 20,3 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((distance_meters >= (0)::numeric)) | 活动距离，单位米。 |
| 12 | `calories_kcal` | 热量 | `numeric(20,3)` | 20,3 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((calories_kcal >= (0)::numeric)) | 餐食或活动能量，单位千卡。 |
| 13 | `duration_minutes` | 持续时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((duration_minutes >= 0)) | 活动持续分钟数。 |
| 14 | `average_heart_rate` | 平均心率 | `numeric(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动或 ECG 测量期间的平均心率。 |
| 15 | `source` | 数据来源 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 16 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_activity_pkey` | `id, started_at` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `hardware_activity_started_at_idx` | `started_at` | 普通索引 | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_activity_scoped_recent` | `tenant_id, user_id, device_id, started_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_activity_source` | `tenant_id, user_id, device_id, started_at, activity_type, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_activity.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `started_at`：CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))。
- `ended_at`：CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))。
- `steps`：CHECK ((steps >= 0))。
- `distance_meters`：CHECK ((distance_meters >= (0)::numeric))。
- `calories_kcal`：CHECK ((calories_kcal >= (0)::numeric))。
- `duration_minutes`：CHECK ((duration_minutes >= 0))。
- `source`：状态/类型类字段，完整枚举值待确认。
- `ck_hardware_activity_window`（CHECK）：`CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))`。
- `hardware_activity_calories_kcal_check`（CHECK）：`CHECK ((calories_kcal >= (0)::numeric))`。
- `hardware_activity_distance_meters_check`（CHECK）：`CHECK ((distance_meters >= (0)::numeric))`。
- `hardware_activity_duration_minutes_check`（CHECK）：`CHECK ((duration_minutes >= 0))`。
- `hardware_activity_steps_check`（CHECK）：`CHECK ((steps >= 0))`。

### 业务说明

保存规范化活动、步数、距离、热量、时长和心率。

## 3. 表：`hardware_data_quality_event` 硬件数据质量事件表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_data_quality_event` |
| 中文名称 | 硬件数据质量事件表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件信号与质量 |
| 业务作用 | 保存遥测质量事件、严重程度和详情码。 |
| 主键 | `id, event_at` |
| 存储引擎 | TimescaleDB Hypertable |
| 数据量级 | 当前本地实例 0 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_data_quality_event_pkey | hardware_data_quality_event_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_quality_source | idx_hardware_quality_scoped_recent、uq_hardware_quality_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_quality_source | idx_hardware_quality_scoped_recent、uq_hardware_quality_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_quality_source | idx_hardware_quality_scoped_recent、uq_hardware_quality_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_quality_source | uq_hardware_quality_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `event_type` | 事件类型 | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_quality_source | uq_hardware_quality_source | 否 | 否 | — | 标识质量、Outbox、归因或审计事件的业务类型。 |
| 8 | `severity` | 严重程度 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARN'::character varying, 'ERROR'::character varying])::text[]))) | 质量事件严重程度，受数据库 CHECK 约束。 |
| 9 | `detail_code` | 质量详情码 | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 质量事件的稳定细分代码。 |
| 10 | `event_at` | 事件发生时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_data_quality_event_pkey、联合唯一:uq_hardware_quality_source | hardware_data_quality_event_event_at_idx、hardware_data_quality_event_pkey、idx_hardware_quality_scoped_recent、uq_hardware_quality_source | 否 | 否 | — | 硬件质量事件实际发生时间。 |
| 11 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_data_quality_event_event_at_idx` | `event_at` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `hardware_data_quality_event_pkey` | `id, event_at` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `idx_hardware_quality_scoped_recent` | `tenant_id, user_id, device_id, event_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_quality_source` | `tenant_id, user_id, device_id, event_at, event_type, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_data_quality_event.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `severity`：CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARN'::character varying, 'ERROR'::character varying])::text[])))。
- `ck_hardware_quality_severity`（CHECK）：`CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARN'::character varying, 'ERROR'::character varying])::text[])))`。

### 业务说明

保存遥测质量事件、严重程度和详情码。

## 4. 表：`hardware_diet_record` 硬件域饮食行为表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_diet_record` |
| 中文名称 | 硬件域饮食行为表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件时序事实 |
| 业务作用 | 保存随 telemetry-v2 批次提交的规范化饮食行为。 |
| 主键 | `id, consumed_at` |
| 存储引擎 | TimescaleDB Hypertable |
| 数据量级 | 当前本地实例 3 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_diet_record_pkey | hardware_diet_record_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_diet_source | idx_hardware_diet_scoped_recent、uq_hardware_diet_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_diet_source | idx_hardware_diet_scoped_recent、uq_hardware_diet_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_diet_source | uq_hardware_diet_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_diet_source | uq_hardware_diet_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `consumed_at` | 进餐时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_diet_record_pkey、联合唯一:uq_hardware_diet_source | hardware_diet_record_consumed_at_idx、hardware_diet_record_pkey、idx_hardware_diet_scoped_recent、uq_hardware_diet_source | 否 | 否 | — | 用户实际进餐或记录餐食的时间。 |
| 8 | `meal_type` | 餐次类型 | `character varying(16)` | 16 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK (((meal_type)::text = ANY ((ARRAY['breakfast'::character varying, 'lunch'::character varying, 'dinner'::character varying, 'snack'::character varying])::text[]))) | 餐食所属的早餐、午餐、晚餐或加餐类别。 |
| 9 | `description` | 描述 | `character varying(256)` | 256 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前记录的业务内容描述。 |
| 10 | `calories_kcal` | 热量 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食或活动能量，单位千卡。 |
| 11 | `protein_grams` | 蛋白质 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食蛋白质估计值，单位克。 |
| 12 | `carbohydrate_grams` | 碳水化合物 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食碳水化合物估计值，单位克。 |
| 13 | `fat_grams` | 脂肪 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食脂肪估计值，单位克。 |
| 14 | `fiber_grams` | 膳食纤维 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食膳食纤维估计值，单位克。 |
| 15 | `sodium_milligrams` | 钠 | `numeric(12,3)` | 12,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric)))) | 餐食钠估计值，单位毫克。 |
| 16 | `source` | 数据来源 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 17 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_diet_record_consumed_at_idx` | `consumed_at` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `hardware_diet_record_pkey` | `id, consumed_at` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `idx_hardware_diet_scoped_recent` | `tenant_id, user_id, consumed_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_diet_source` | `tenant_id, user_id, device_id, consumed_at, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_diet_record.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `meal_type`：CHECK (((meal_type)::text = ANY ((ARRAY['breakfast'::character varying, 'lunch'::character varying, 'dinner'::character varying, 'snack'::character varying])::text[])))。
- `calories_kcal`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `protein_grams`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `carbohydrate_grams`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `fat_grams`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `fiber_grams`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `sodium_milligrams`：CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))。
- `source`：状态/类型类字段，完整枚举值待确认。
- `ck_hardware_diet_meal_type`（CHECK）：`CHECK (((meal_type)::text = ANY ((ARRAY['breakfast'::character varying, 'lunch'::character varying, 'dinner'::character varying, 'snack'::character varying])::text[])))`。
- `ck_hardware_diet_non_negative`（CHECK）：`CHECK ((((calories_kcal IS NULL) OR (calories_kcal >= (0)::numeric)) AND ((protein_grams IS NULL) OR (protein_grams >= (0)::numeric)) AND ((carbohydrate_grams IS NULL) OR (carbohydrate_grams >= (0)::numeric)) AND ((fat_grams IS NULL) OR (fat_grams >= (0)::numeric)) AND ((fiber_grams IS NULL) OR (fiber_grams >= (0)::numeric)) AND ((sodium_milligrams IS NULL) OR (sodium_milligrams >= (0)::numeric))))`。

### 业务说明

保存随 telemetry-v2 批次提交的规范化饮食行为。

## 5. 表：`hardware_measurement` 硬件标量测量表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_measurement` |
| 中文名称 | 硬件标量测量表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件时序事实 |
| 业务作用 | 保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。 |
| 主键 | `id, observed_at` |
| 存储引擎 | TimescaleDB Hypertable |
| 数据量级 | 当前本地实例 67 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_measurement_pkey | hardware_measurement_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_measurement_source | idx_hardware_measurement_metric_time、idx_hardware_measurement_scoped_recent、uq_hardware_measurement_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_measurement_source | idx_hardware_measurement_scoped_recent、uq_hardware_measurement_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_measurement_source | idx_hardware_measurement_scoped_recent、uq_hardware_measurement_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_measurement_source | uq_hardware_measurement_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `metric_type` | 指标类型 | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_measurement_source | idx_hardware_measurement_metric_time、uq_hardware_measurement_source | 否 | 否 | — | 标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。 |
| 8 | `observed_at` | 观测时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_measurement_pkey、联合唯一:uq_hardware_measurement_source | hardware_measurement_observed_at_idx、hardware_measurement_pkey、idx_hardware_measurement_metric_time、idx_hardware_measurement_scoped_recent、uq_hardware_measurement_source | 否 | 否 | — | 硬件标量测量发生时间，使用 TIMESTAMPTZ。 |
| 9 | `primary_value` | 主测量值 | `numeric(20,6)` | 20,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的主要数值，例如单值指标或血压收缩压分量。 |
| 10 | `secondary_value` | 次测量值 | `numeric(20,6)` | 20,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的可选第二数值，例如成对测量的第二分量。 |
| 11 | `unit` | 计量单位 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 12 | `quality_code` | 质量代码 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化的设备或遥测质量代码。 |
| 13 | `source` | 数据来源 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 14 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_measurement_observed_at_idx` | `observed_at` | 普通索引 | 支持按业务作用域和时间范围查询或排序。 |
| `hardware_measurement_pkey` | `id, observed_at` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `idx_hardware_measurement_metric_time` | `tenant_id, metric_type, observed_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_measurement_scoped_recent` | `tenant_id, user_id, device_id, observed_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_measurement_source` | `tenant_id, user_id, device_id, observed_at, metric_type, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_measurement.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。

## 6. 表：`hardware_migration_checkpoint` 硬件迁移检查点表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_migration_checkpoint` |
| 中文名称 | 硬件迁移检查点表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件迁移对账 |
| 业务作用 | 保存旧 MySQL 硬件数据迁移位置、行数、哈希和校验状态。 |
| 主键 | `id` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 0 行（精确计数，非生产容量） |
| 是否核心表 | 否（迁移支持） |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | hardware_migration_checkpoint_pkey | hardware_migration_checkpoint_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `source_name` | 迁移来源名称 | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_migration_checkpoint | uq_hardware_migration_checkpoint | 否 | 否 | — | 旧数据迁移来源系统或表的稳定名称。 |
| 3 | `checkpoint_key` | 迁移检查点键 | `character varying(256)` | 256 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_migration_checkpoint | uq_hardware_migration_checkpoint | 否 | 否 | — | 标识某个迁移分片或范围的稳定检查点。 |
| 4 | `source_position` | 来源位置 | `character varying(512)` | 512 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 已处理到的来源偏移、水位或主键位置。 |
| 5 | `row_count` | 迁移行数 | `bigint` | 64,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((row_count >= 0)) | 当前迁移检查点覆盖的记录行数。 |
| 6 | `source_hash` | 来源哈希 | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 迁移来源范围的完整性摘要。 |
| 7 | `target_hash` | 目标哈希 | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 迁移目标范围的完整性摘要，用于与来源对账。 |
| 8 | `status` | 状态 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'DRIFTED'::character varying, 'BLOCKED'::character varying])::text[]))) | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 9 | `checked_at` | 反馈打卡时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户对干预行动提交反馈的时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_migration_checkpoint_pkey` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uq_hardware_migration_checkpoint` | `source_name, checkpoint_key` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `row_count`：CHECK ((row_count >= 0))。
- `status`：CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'DRIFTED'::character varying, 'BLOCKED'::character varying])::text[])))。
- `ck_hardware_migration_status`（CHECK）：`CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'DRIFTED'::character varying, 'BLOCKED'::character varying])::text[])))`。
- `hardware_migration_checkpoint_row_count_check`（CHECK）：`CHECK ((row_count >= 0))`。

### 业务说明

保存旧 MySQL 硬件数据迁移位置、行数、哈希和校验状态。

## 7. 表：`hardware_outbox` 遥测事务 Outbox 表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_outbox` |
| 中文名称 | 遥测事务 Outbox 表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件可靠性 |
| 业务作用 | 与遥测事实同事务写入，随后可靠发布隐私安全 Kafka 事件。 |
| 主键 | `id` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 11 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | hardware_outbox_pkey | hardware_outbox_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `aggregate_type` | 聚合类型 | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_outbox_event | uq_hardware_outbox_event | 否 | 否 | — | Outbox 事件所属业务聚合类型。 |
| 5 | `aggregate_id` | 聚合 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_outbox_event | uq_hardware_outbox_event | 否 | 否/待确认 | — | Outbox 事件所属业务聚合标识。 |
| 6 | `event_type` | 事件类型 | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_outbox_event | uq_hardware_outbox_event | 否 | 否 | — | 标识质量、Outbox、归因或审计事件的业务类型。 |
| 7 | `event_version` | 事件版本 | `integer` | 32,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_outbox_event | uq_hardware_outbox_event | 否 | 否 | CHECK ((event_version > 0)) | 同一聚合事件类型的版本号，必须大于零。 |
| 8 | `status` | 状态 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_outbox_dispatch | 是 | 否 | CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying, 'DLQ_REVIEW'::character varying])::text[]))) | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 9 | `event_metadata` | 事件元数据 | `jsonb` | 不适用 | 否 | `'{}'::jsonb` | 否 | 否 | 否 | 否 | 否 | 否 | — | Kafka 发布所需的最小隐私安全元数据，不含原始健康值。 |
| 10 | `attempt_count` | 尝试次数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((attempt_count >= 0)) | 对账、Outbox 或迁移任务的累计处理次数。 |
| 11 | `available_at` | 可处理时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | idx_hardware_outbox_dispatch | 否 | 否 | — | Outbox 事件允许发布器领取的最早时间。 |
| 12 | `published_at` | 发布时间 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_hardware_outbox_published_retention | 否 | 否 | — | Outbox 事件成功发布到 Kafka 的时间。 |
| 13 | `last_error_code` | 最近错误码 | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最近一次失败的脱敏稳定错误码。 |
| 14 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 15 | `updated_at` | 更新时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_outbox_pkey` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `idx_hardware_outbox_dispatch` | `status, available_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_hardware_outbox_published_retention` | `published_at` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `uq_hardware_outbox_event` | `aggregate_type, aggregate_id, event_type, event_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_outbox.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `event_version`：CHECK ((event_version > 0))。
- `status`：CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying, 'DLQ_REVIEW'::character varying])::text[])))。
- `attempt_count`：CHECK ((attempt_count >= 0))。
- `ck_hardware_outbox_status`（CHECK）：`CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying, 'DLQ_REVIEW'::character varying])::text[])))`。
- `hardware_outbox_attempt_count_check`（CHECK）：`CHECK ((attempt_count >= 0))`。
- `hardware_outbox_event_version_check`（CHECK）：`CHECK ((event_version > 0))`。

### 业务说明

与遥测事实同事务写入，随后可靠发布隐私安全 Kafka 事件。

## 8. 表：`hardware_reconciliation` 硬件批次对账表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_reconciliation` |
| 中文名称 | 硬件批次对账表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件可靠性 |
| 业务作用 | 保存每个上传批次唯一的对账状态、重试和人工处理元数据。 |
| 主键 | `id` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 11 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | hardware_reconciliation_pkey | hardware_reconciliation_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | hardware_reconciliation_upload_batch_id_key | hardware_reconciliation_upload_batch_id_key | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_reconciliation_state_time | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `state` | 待确认 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_reconciliation_state_time | 否 | 否 | CHECK (((state)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[]))) | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `attempt_count` | 尝试次数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((attempt_count >= 0)) | 对账、Outbox 或迁移任务的累计处理次数。 |
| 6 | `last_error_code` | 最近错误码 | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最近一次失败的脱敏稳定错误码。 |
| 7 | `operator_actor_id` | 处理人 ID | `character varying(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 对账进入人工处理时的操作者标识。 |
| 8 | `operator_reason` | 处理原因 | `character varying(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 人工对账或处置的原因说明。 |
| 9 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 10 | `updated_at` | 更新时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | idx_hardware_reconciliation_state_time | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_reconciliation_pkey` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `hardware_reconciliation_upload_batch_id_key` | `upload_batch_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `idx_hardware_reconciliation_state_time` | `tenant_id, state, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |

### 关联关系

- `hardware_reconciliation.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `state`：CHECK (((state)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[])))。
- `attempt_count`：CHECK ((attempt_count >= 0))。
- `ck_hardware_reconciliation_state`（CHECK）：`CHECK (((state)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[])))`。
- `hardware_reconciliation_attempt_count_check`（CHECK）：`CHECK ((attempt_count >= 0))`。

### 业务说明

保存每个上传批次唯一的对账状态、重试和人工处理元数据。

## 9. 表：`hardware_signal_chunk_metadata` 硬件信号元数据表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_signal_chunk_metadata` |
| 中文名称 | 硬件信号元数据表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件信号与质量 |
| 业务作用 | 只保存信号时间窗、采样率和质量元数据，不保存原始波形。 |
| 主键 | `id` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 0 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | hardware_signal_chunk_metadata_pkey | hardware_signal_chunk_metadata_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `signal_type` | 信号类型 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 否 | 否 | — | 标识信号/ECG 分块或元数据的信号类别。 |
| 8 | `started_at` | 开始时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_signal_metadata_source | uq_hardware_signal_metadata_source | 否 | 否 | CHECK (((ended_at IS NULL) OR (ended_at >= started_at))) | 会话、活动或信号时间窗开始时间。 |
| 9 | `ended_at` | 结束时间 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK (((ended_at IS NULL) OR (ended_at >= started_at))) | 会话、活动或信号时间窗结束时间。 |
| 10 | `sample_rate_hz` | 采样率 | `numeric(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 信号采样频率，单位 Hz。 |
| 11 | `sample_count` | 采样点数 | `integer` | 32,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK (((sample_count IS NULL) OR (sample_count >= 0))) | 当前信号块包含的样本数量。 |
| 12 | `quality_code` | 质量代码 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化的设备或遥测质量代码。 |
| 13 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | idx_hardware_signal_metadata_retention | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_signal_chunk_metadata_pkey` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `idx_hardware_signal_metadata_retention` | `created_at` | 普通索引 | 支持按业务作用域和时间范围查询或排序。 |
| `uq_hardware_signal_metadata_source` | `tenant_id, user_id, device_id, started_at, signal_type, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_signal_chunk_metadata.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `started_at`：CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))。
- `ended_at`：CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))。
- `sample_count`：CHECK (((sample_count IS NULL) OR (sample_count >= 0)))。
- `ck_hardware_signal_window`（CHECK）：`CHECK (((ended_at IS NULL) OR (ended_at >= started_at)))`。
- `hardware_signal_chunk_metadata_sample_count_check`（CHECK）：`CHECK (((sample_count IS NULL) OR (sample_count >= 0)))`。

### 业务说明

只保存信号时间窗、采样率和质量元数据，不保存原始波形。

## 10. 表：`hardware_sleep_session` 硬件睡眠会话表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_sleep_session` |
| 中文名称 | 硬件睡眠会话表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件时序事实 |
| 业务作用 | 保存规范化睡眠会话和阶段分钟数。 |
| 主键 | `id, started_at` |
| 存储引擎 | TimescaleDB Hypertable |
| 数据量级 | 当前本地实例 15 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_sleep_session_pkey | hardware_sleep_session_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_sleep_source | idx_hardware_sleep_scoped_recent、uq_hardware_sleep_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_sleep_source | idx_hardware_sleep_scoped_recent、uq_hardware_sleep_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_sleep_source | idx_hardware_sleep_scoped_recent、uq_hardware_sleep_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source_record_id` | 来源记录 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_sleep_source | uq_hardware_sleep_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 7 | `started_at` | 开始时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 是 | 否 | 联合唯一:hardware_sleep_session_pkey、联合唯一:uq_hardware_sleep_source | hardware_sleep_session_pkey、hardware_sleep_session_started_at_idx、idx_hardware_sleep_scoped_recent、uq_hardware_sleep_source | 否 | 否 | CHECK ((ended_at >= started_at)) | 会话、活动或信号时间窗开始时间。 |
| 8 | `ended_at` | 结束时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((ended_at >= started_at)) | 会话、活动或信号时间窗结束时间。 |
| 9 | `deep_minutes` | 深睡时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((deep_minutes >= 0)) | 深睡阶段分钟数。 |
| 10 | `light_minutes` | 浅睡时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((light_minutes >= 0)) | 浅睡阶段分钟数。 |
| 11 | `awake_minutes` | 清醒时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((awake_minutes >= 0)) | 睡眠会话内清醒分钟数。 |
| 12 | `rem_minutes` | REM 时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((rem_minutes >= 0)) | 快速眼动睡眠阶段分钟数。 |
| 13 | `interruption_minutes` | 中断时长 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((interruption_minutes >= 0)) | 睡眠中断分钟数。 |
| 14 | `source` | 数据来源 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 15 | `created_at` | 创建时间 | `timestamp with time zone` | 不适用 | 否 | `now()` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_sleep_session_pkey` | `id, started_at` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `hardware_sleep_session_started_at_idx` | `started_at` | 普通索引 | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_sleep_scoped_recent` | `tenant_id, user_id, device_id, started_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_sleep_source` | `tenant_id, user_id, device_id, started_at, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_sleep_session.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- `started_at`：CHECK ((ended_at >= started_at))。
- `ended_at`：CHECK ((ended_at >= started_at))。
- `deep_minutes`：CHECK ((deep_minutes >= 0))。
- `light_minutes`：CHECK ((light_minutes >= 0))。
- `awake_minutes`：CHECK ((awake_minutes >= 0))。
- `rem_minutes`：CHECK ((rem_minutes >= 0))。
- `interruption_minutes`：CHECK ((interruption_minutes >= 0))。
- `source`：状态/类型类字段，完整枚举值待确认。
- `ck_hardware_sleep_window`（CHECK）：`CHECK ((ended_at >= started_at))`。
- `hardware_sleep_session_awake_minutes_check`（CHECK）：`CHECK ((awake_minutes >= 0))`。
- `hardware_sleep_session_deep_minutes_check`（CHECK）：`CHECK ((deep_minutes >= 0))`。
- `hardware_sleep_session_interruption_minutes_check`（CHECK）：`CHECK ((interruption_minutes >= 0))`。
- `hardware_sleep_session_light_minutes_check`（CHECK）：`CHECK ((light_minutes >= 0))`。
- `hardware_sleep_session_rem_minutes_check`（CHECK）：`CHECK ((rem_minutes >= 0))`。

### 业务说明

保存规范化睡眠会话和阶段分钟数。

## 11. 表：`hardware_upload_batch` 硬件上传批次表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_upload_batch` |
| 中文名称 | 硬件上传批次表 |
| 所属数据库 | `rehealth_hardware` |
| 所属模块 | 硬件上传批次 |
| 业务作用 | 保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。 |
| 主键 | `id` |
| 存储引擎 | PostgreSQL |
| 数据量级 | 当前本地实例 12 行（精确计数，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `uuid` | 不适用 | 否 | `无/NULL` | 是 | 否 | hardware_upload_batch_pkey | hardware_upload_batch_pkey | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `receipt_id` | 持久化收据 ID | `uuid` | 不适用 | 否 | `无/NULL` | 否 | 否 | hardware_upload_batch_receipt_id_key | hardware_upload_batch_receipt_id_key | 否 | 否/待确认 | — | 服务端为已接收批次生成的唯一收据标识。 |
| 3 | `tenant_id` | 租户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_batch_source | idx_hardware_batch_device_time、idx_hardware_batch_user_time、uq_hardware_batch_source | 是 | 否/待确认 | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `character varying(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_batch_source | idx_hardware_batch_user_time、uq_hardware_batch_source | 是 | 否/待确认 | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_batch_source | idx_hardware_batch_device_time、uq_hardware_batch_source | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `batch_id` | 客户端批次 ID | `character varying(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uq_hardware_batch_source | uq_hardware_batch_source | 否 | 否/待确认 | — | 客户端生成的稳定遥测批次业务键，重试时保持不变。 |
| 7 | `source` | 数据来源 | `character varying(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 8 | `collected_from` | 采集窗口起点 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 上传批次覆盖的最早采集时间。 |
| 9 | `collected_to` | 采集窗口终点 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 上传批次覆盖的最晚采集时间。 |
| 10 | `received_at` | 接收时间 | `timestamp with time zone` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_batch_device_time、idx_hardware_batch_user_time | 否 | 否 | — | 服务端收到上传批次的时间。 |
| 11 | `committed_at` | 持久化完成时间 | `timestamp with time zone` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次完成约定 durable write 的时间。 |
| 12 | `status` | 状态 | `character varying(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[]))) | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 13 | `record_count` | 记录总数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((record_count >= 0)) | 批次中全部规范化记录数量。 |
| 14 | `measurement_count` | 测量记录数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((measurement_count >= 0)) | 批次中的标量测量条数。 |
| 15 | `sleep_session_count` | 睡眠会话数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((sleep_session_count >= 0)) | 批次中的睡眠会话条数。 |
| 16 | `activity_count` | 活动记录数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((activity_count >= 0)) | 批次中的活动记录条数。 |
| 17 | `signal_metadata_count` | 信号元数据数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((signal_metadata_count >= 0)) | 批次中的信号元数据条数，不含原始波形。 |
| 18 | `quality_summary` | 质量摘要 | `jsonb` | 不适用 | 否 | `'{}'::jsonb` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次级结构化质量汇总，不保存原始健康载荷。 |
| 19 | `diet_record_count` | 饮食记录数 | `integer` | 32,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | CHECK ((diet_record_count >= 0)) | 批次中的饮食行为条数。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `hardware_upload_batch_pkey` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `hardware_upload_batch_receipt_id_key` | `receipt_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `idx_hardware_batch_device_time` | `tenant_id, device_id, received_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_batch_user_time` | `tenant_id, user_id, received_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `uq_hardware_batch_source` | `tenant_id, user_id, device_id, batch_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[])))。
- `record_count`：CHECK ((record_count >= 0))。
- `measurement_count`：CHECK ((measurement_count >= 0))。
- `sleep_session_count`：CHECK ((sleep_session_count >= 0))。
- `activity_count`：CHECK ((activity_count >= 0))。
- `signal_metadata_count`：CHECK ((signal_metadata_count >= 0))。
- `diet_record_count`：CHECK ((diet_record_count >= 0))。
- `source`：状态/类型类字段，完整枚举值待确认。
- `ck_hardware_batch_status`（CHECK）：`CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PERSISTED'::character varying, 'EVENT_PENDING'::character varying, 'EVENT_PUBLISHED'::character varying, 'REJECTED'::character varying, 'RETRYABLE_FAILURE'::character varying, 'DLQ_REVIEW'::character varying, 'RESOLVED'::character varying])::text[])))`。
- `hardware_upload_batch_activity_count_check`（CHECK）：`CHECK ((activity_count >= 0))`。
- `hardware_upload_batch_diet_record_count_check`（CHECK）：`CHECK ((diet_record_count >= 0))`。
- `hardware_upload_batch_measurement_count_check`（CHECK）：`CHECK ((measurement_count >= 0))`。
- `hardware_upload_batch_record_count_check`（CHECK）：`CHECK ((record_count >= 0))`。
- `hardware_upload_batch_signal_metadata_count_check`（CHECK）：`CHECK ((signal_metadata_count >= 0))`。
- `hardware_upload_batch_sleep_session_count_check`（CHECK）：`CHECK ((sleep_session_count >= 0))`。

### 业务说明

保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。
