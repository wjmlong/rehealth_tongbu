# Android Room v16 数据库逐表结构

> 本文件由 `tools/generate_database_schema_docs.py` 根据只读结构元数据生成。
> 不包含数据库账号、密码、业务行内容或原始健康数据。

结构来自 Room 导出的 `16.json`，共 22 张实际注册表；当前无已连接 Android 设备，因此数据量记为未知。

## 表清单

| 序号 | 表名 | 中文名称 | 模块 | 主要用途 | 核心表 |
| ---: | --- | --- | --- | --- | --- |
| 1 | [`health_records`](#health-records) | 通用健康记录表 | Android 早期骨架 | 早期通用健康记录骨架；当前未在 AppDatabase 暴露 DAO，实际用途待确认。 | 否（遗留骨架） |
| 2 | [`attribution_logs`](#attribution-logs) | 本地归因审计骨架表 | Android 早期骨架 | 保存本地归因完整度、证据等级和审计哈希；当前未在 AppDatabase 暴露 DAO。 | 否（遗留骨架） |
| 3 | [`ring_measurements`](#ring-measurements) | 可穿戴测量表 | Android 可穿戴数据 | 保存按用户和设备隔离的心率、血氧、血压等规范化标量测量。 | 是 |
| 4 | [`ring_sleep_sessions`](#ring-sleep-sessions) | 可穿戴睡眠会话表 | Android 可穿戴数据 | 保存设备睡眠阶段、厂商总睡眠时长及用户/设备归属。 | 是 |
| 5 | [`ring_activities`](#ring-activities) | 可穿戴活动表 | Android 可穿戴数据 | 保存步数、距离、热量、活动时长和平均心率等活动事实。 | 是 |
| 6 | [`ring_signal_chunks`](#ring-signal-chunks) | 本地信号与 ECG 分块表 | Android 可穿戴数据 | 保存仅限本机使用的信号/ECG 波形和导联、采样、校准元数据；波形不上传云端。 | 是 |
| 7 | [`sync_upload_queue`](#sync-upload-queue) | 离线上传队列表 | Android 离线同步 | 保存先落库后上传的持久化任务，支持认证暂停、退避重试和死信。 | 是 |
| 8 | [`intervention_feedback_queue`](#intervention-feedback-queue) | 干预反馈上传队列表 | Android 离线同步 | 保存用户对具体干预项的反馈及上传重试状态。 | 是 |
| 9 | [`cvd_risk_history`](#cvd-risk-history) | 本地 CVD 风险历史表 | Android CVD 风险 | 按用户和自然日保存已确认、非 Mock 的云端 CVD/RDI-16 风险结果。 | 是 |
| 10 | [`health_chat_conversations`](#health-chat-conversations) | 本地健康问答会话表 | Android 健康问答 | 按用户保存健康问答会话列表、激活和逻辑删除状态。 | 是 |
| 11 | [`health_chat_messages`](#health-chat-messages) | 本地健康问答消息表 | Android 健康问答 | 在请求服务端前先保存用户消息，并跟踪请求、模型和投递状态。 | 是 |
| 12 | [`rdi_daily_snapshots`](#rdi-daily-snapshots) | RDI 每日快照表 | Android RDI | 保存本地 RDI 规则引擎每日快照；不替代云端 CVD 临床风险。 | 是 |
| 13 | [`rdi_contribution_records`](#rdi-contribution-records) | RDI 因素贡献表 | Android RDI | 保存每日 RDI 快照的逐因素证据、置信度和贡献分。 | 是 |
| 14 | [`rdi_baselines`](#rdi-baselines) | RDI 个人基线表 | Android RDI | 保存按用户和因素版本化、冻结期内不覆盖的个人稳健基线。 | 是 |
| 15 | [`rdi_confirmed_labs`](#rdi-confirmed-labs) | 已确认化验锚点表 | Android RDI | 保存用户确认后的化验指标锚点；未确认 OCR 不计入评分。 | 是 |
| 16 | [`rdi_confirmed_meals`](#rdi-confirmed-meals) | 已确认餐食锚点表 | Android RDI | 保存用户确认后的餐食营养区间与餐食影响证据。 | 是 |
| 17 | [`rhi_manual_health_inputs`](#rhi-manual-health-inputs) | RHI 手工健康输入表 | Android RHI | 保存久坐、腰围、VO2max、化验和经确认袖带血压等用户手填输入。 | 是 |
| 18 | [`rhi_daily_health_index`](#rhi-daily-health-index) | RHI 每日健康指数表 | Android RHI | 保存每个用户每日唯一的 RHI 总分、可信度、冷启动状态和算法版本。 | 是 |
| 19 | [`rhi_daily_domain_score`](#rhi-daily-domain-score) | RHI 每日领域分表 | Android RHI | 保存 RHI 日快照的五领域分解；无有效指标的领域分数保持 NULL。 | 是 |
| 20 | [`rhi_daily_feature_snapshot`](#rhi-daily-feature-snapshot) | RHI 每日特征快照表 | Android RHI | 保存产生每日 RHI 的特征值、置信度和个人基线统计。 | 是 |
| 21 | [`rhi_data_quality_snapshot`](#rhi-data-quality-snapshot) | RHI 数据质量快照表 | Android RHI | 保存每日 RHI 的缺失字段、低置信字段、质量警告和设备变化标志。 | 是 |
| 22 | [`diet_records`](#diet-records) | 本地饮食记录表 | Android 饮食 | 保存手工或经确认拍照产生的餐食，随后通过遥测离线队列上传。 | 是 |

## 模块统计

| 模块 | 表数 |
| --- | ---: |
| Android CVD 风险 | 1 |
| Android RDI | 5 |
| Android RHI | 5 |
| Android 健康问答 | 2 |
| Android 可穿戴数据 | 4 |
| Android 早期骨架 | 2 |
| Android 离线同步 | 2 |
| Android 饮食 | 1 |

## 1. 表：`health_records` 通用健康记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `health_records` |
| 中文名称 | 通用健康记录表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 早期骨架 |
| 业务作用 | 早期通用健康记录骨架；当前未在 AppDatabase 暴露 DAO，实际用途待确认。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 否（遗留骨架） |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `type` | 类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。 |
| 3 | `value` | 记录值 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存通用健康记录的值；具体类型由同表 type 和 unit 解释。 |
| 4 | `unit` | 计量单位 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 5 | `recordedAt` | 待确认 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type, source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

早期通用健康记录骨架；当前未在 AppDatabase 暴露 DAO，实际用途待确认。

## 2. 表：`attribution_logs` 本地归因审计骨架表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `attribution_logs` |
| 中文名称 | 本地归因审计骨架表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 早期骨架 |
| 业务作用 | 保存本地归因完整度、证据等级和审计哈希；当前未在 AppDatabase 暴露 DAO。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 否（遗留骨架） |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `date` | 日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前本地记录所属的自然日。 |
| 3 | `completeness` | 数据完整度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 归因审计中记录的输入完整度。 |
| 4 | `evidenceGrade` | 证据等级 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地归因审计使用的证据等级。 |
| 5 | `auditHash` | 审计哈希 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地归因证据的完整性摘要。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存本地归因完整度、证据等级和审计哈希；当前未在 AppDatabase 暴露 DAO。

## 3. 表：`ring_measurements` 可穿戴测量表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `ring_measurements` |
| 中文名称 | 可穿戴测量表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 可穿戴数据 |
| 业务作用 | 保存按用户和设备隔离的心率、血氧、血压等规范化标量测量。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `metric_type` | 指标类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_measurements_metric_type_measured_at、index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at | 否 | 否 | — | 标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。 |
| 3 | `measured_at` | 测量时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_measurements_metric_type_measured_at、index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at | 否 | 否 | — | 健康指标实际测量时间。 |
| 4 | `primary_value` | 主测量值 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的主要数值，例如单值指标或血压收缩压分量。 |
| 5 | `secondary_value` | 次测量值 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的可选第二数值，例如成对测量的第二分量。 |
| 6 | `unit` | 计量单位 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 7 | `quality` | 质量值 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Provider 提供或规范化后的测量质量；具体量纲按指标实现确认。 |
| 8 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 9 | `raw_payload` | 原始扩展载荷 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。 |
| 10 | `owner_user_id` | 所属用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at | 否 | 逻辑→认证用户.sys_user.id | — | Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。 |
| 11 | `device_id` | 稳定设备 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at | 否 | 逻辑→rehealth_device_binding.稳定设备标识 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_ring_measurements_metric_type_measured_at` | `metric_type, measured_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at` | `owner_user_id, device_id, source, metric_type, measured_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `ring_measurements.(owner_user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。
- `ring_measurements.(device_id)` → `rehealth_device_binding.(稳定设备标识)`：逻辑外键；跨库设备逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存按用户和设备隔离的心率、血氧、血压等规范化标量测量。

## 4. 表：`ring_sleep_sessions` 可穿戴睡眠会话表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `ring_sleep_sessions` |
| 中文名称 | 可穿戴睡眠会话表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 可穿戴数据 |
| 业务作用 | 保存设备睡眠阶段、厂商总睡眠时长及用户/设备归属。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `started_at` | 开始时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 3 | `ended_at` | 结束时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_sleep_sessions_owner_user_id_ended_at | 否 | 否 | — | 会话、活动或信号时间窗结束时间。 |
| 4 | `deep_minutes` | 深睡时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 深睡阶段分钟数。 |
| 5 | `light_minutes` | 浅睡时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 浅睡阶段分钟数。 |
| 6 | `awake_minutes` | 清醒时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 睡眠会话内清醒分钟数。 |
| 7 | `rem_minutes` | REM 时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 快速眼动睡眠阶段分钟数。 |
| 8 | `interruption_minutes` | 中断时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 睡眠中断分钟数。 |
| 9 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 10 | `raw_payload` | 原始扩展载荷 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。 |
| 11 | `total_sleep_minutes` | 厂商总睡眠时长 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Provider 明确返回的权威睡眠总分钟数；无值时不以起止跨度替代。 |
| 12 | `owner_user_id` | 所属用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_ring_sleep_sessions_owner_user_id_ended_at | 否 | 逻辑→认证用户.sys_user.id | — | Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。 |
| 13 | `device_id` | 稳定设备 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_device_binding.稳定设备标识 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_ring_sleep_sessions_owner_user_id_ended_at` | `owner_user_id, ended_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `ring_sleep_sessions.(owner_user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。
- `ring_sleep_sessions.(device_id)` → `rehealth_device_binding.(稳定设备标识)`：逻辑外键；跨库设备逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存设备睡眠阶段、厂商总睡眠时长及用户/设备归属。

## 5. 表：`ring_activities` 可穿戴活动表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `ring_activities` |
| 中文名称 | 可穿戴活动表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 可穿戴数据 |
| 业务作用 | 保存步数、距离、热量、活动时长和平均心率等活动事实。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `started_at` | 开始时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_activities_owner_user_id_started_at | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 3 | `ended_at` | 结束时间 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗结束时间。 |
| 4 | `activity_type` | 活动类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识活动记录的类型；具体允许值由设备 Provider 映射定义。 |
| 5 | `steps` | 步数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动时间窗或自然日内的设备步数。 |
| 6 | `distance_meters` | 距离 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动距离，单位米。 |
| 7 | `calories_kcal` | 热量 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食或活动能量，单位千卡。 |
| 8 | `duration_minutes` | 持续时长 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动持续分钟数。 |
| 9 | `average_heart_rate` | 平均心率 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动或 ECG 测量期间的平均心率。 |
| 10 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 11 | `raw_payload` | 原始扩展载荷 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。 |
| 12 | `owner_user_id` | 所属用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_ring_activities_owner_user_id_started_at | 否 | 逻辑→认证用户.sys_user.id | — | Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。 |
| 13 | `device_id` | 稳定设备 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_device_binding.稳定设备标识 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_ring_activities_owner_user_id_started_at` | `owner_user_id, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |

### 关联关系

- `ring_activities.(owner_user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。
- `ring_activities.(device_id)` → `rehealth_device_binding.(稳定设备标识)`：逻辑外键；跨库设备逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存步数、距离、热量、活动时长和平均心率等活动事实。

## 6. 表：`ring_signal_chunks` 本地信号与 ECG 分块表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `ring_signal_chunks` |
| 中文名称 | 本地信号与 ECG 分块表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 可穿戴数据 |
| 业务作用 | 保存仅限本机使用的信号/ECG 波形和导联、采样、校准元数据；波形不上传云端。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `signal_type` | 信号类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_signal_chunks_signal_type_started_at、index_ring_signal_chunks_owner_user_id_signal_type_started_at | 否 | 否 | — | 标识信号/ECG 分块或元数据的信号类别。 |
| 3 | `started_at` | 开始时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_ring_signal_chunks_signal_type_started_at、index_ring_signal_chunks_owner_user_id_signal_type_started_at | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 4 | `sample_rate_hz` | 采样率 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 信号采样频率，单位 Hz。 |
| 5 | `sample_count` | 采样点数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前信号块包含的样本数量。 |
| 6 | `encoding` | 信号编码 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地信号 payload 的编码格式。 |
| 7 | `payload` | 信号载荷 | `BLOB` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Android 本地保存的信号/ECG 二进制波形；不进入云端遥测上传。 |
| 8 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 9 | `draw_frequency_hz` | 绘制频率 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | ECG 界面绘制或重采样频率，单位 Hz。 |
| 10 | `duration_seconds` | 信号时长 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 信号/ECG 记录持续秒数。 |
| 11 | `lead_type` | 导联类型 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | ECG 记录使用的导联类型。 |
| 12 | `ecg_type` | ECG 类型 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 厂商 SDK 返回的 ECG 类型代码；具体枚举待 SDK 证据确认。 |
| 13 | `calibration_type` | 校准方式 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | ADC 换算到 mV 时使用的校准方式。 |
| 14 | `average_heart_rate` | 平均心率 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动或 ECG 测量期间的平均心率。 |
| 15 | `contact_quality` | 接触质量 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | ECG 电极接触质量状态；具体枚举待 SDK 证据确认。 |
| 16 | `owner_user_id` | 所属用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_ring_signal_chunks_owner_user_id_signal_type_started_at | 否 | 逻辑→认证用户.sys_user.id | — | Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。 |
| 17 | `device_id` | 稳定设备 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_device_binding.稳定设备标识 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_ring_signal_chunks_signal_type_started_at` | `signal_type, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `index_ring_signal_chunks_owner_user_id_signal_type_started_at` | `owner_user_id, signal_type, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |

### 关联关系

- `ring_signal_chunks.(owner_user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。
- `ring_signal_chunks.(device_id)` → `rehealth_device_binding.(稳定设备标识)`：逻辑外键；跨库设备逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存仅限本机使用的信号/ECG 波形和导联、采样、校准元数据；波形不上传云端。

## 7. 表：`sync_upload_queue` 离线上传队列表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sync_upload_queue` |
| 中文名称 | 离线上传队列表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 离线同步 |
| 业务作用 | 保存先落库后上传的持久化任务，支持认证暂停、退避重试和死信。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `kind` | 任务种类 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | telemetry_batch=遥测批次；health_interview=健康访谈；rhi_daily_snapshot=RHI 日快照；rhi_manual_health_input=RHI 手工健康输入 | 标识离线队列载荷的业务种类，由上传调度器选择对应处理客户端。 |
| 3 | `payload_json` | 载荷 JSON | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。 |
| 4 | `status` | 状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | pending=待上传；uploading=上传中；done=已完成；failed=可重试失败；dead_letter=死信 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 5 | `attempts` | 已尝试次数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 离线队列执行当前任务的累计尝试次数，用于退避和死信判断。 |
| 6 | `last_error` | 最近错误 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最近一次队列处理失败的脱敏错误信息。 |
| 7 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 8 | `next_retry_at` | 下次重试时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 队列项允许再次处理的最早时间，支持指数退避。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `kind`：telemetry_batch=遥测批次；health_interview=健康访谈；rhi_daily_snapshot=RHI 日快照；rhi_manual_health_input=RHI 手工健康输入。
- `status`：pending=待上传；uploading=上传中；done=已完成；failed=可重试失败；dead_letter=死信。

### 业务说明

保存先落库后上传的持久化任务，支持认证暂停、退避重试和死信。

## 8. 表：`intervention_feedback_queue` 干预反馈上传队列表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `intervention_feedback_queue` |
| 中文名称 | 干预反馈上传队列表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 离线同步 |
| 业务作用 | 保存用户对具体干预项的反馈及上传重试状态。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `intervention_id` | 干预行动 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 标识用户反馈所针对的具体干预行动。 |
| 3 | `status` | 状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | completed=已完成；partially_completed=部分完成；skipped=已跳过；not_applicable=不适用 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 4 | `note` | 备注 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存用户或业务操作的可选补充说明。 |
| 5 | `checked_at` | 反馈打卡时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户对干预行动提交反馈的时间。 |
| 6 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 7 | `upload_status` | 上传状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | pending=待上传；uploading=上传中；done=已完成；failed=失败 | 当前本地记录的离线上传生命周期状态。 |
| 8 | `upload_attempts` | 上传尝试次数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 反馈队列上传当前记录的累计尝试次数。 |
| 9 | `last_error` | 最近错误 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最近一次队列处理失败的脱敏错误信息。 |
| 10 | `next_retry_at` | 下次重试时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 队列项允许再次处理的最早时间，支持指数退避。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：completed=已完成；partially_completed=部分完成；skipped=已跳过；not_applicable=不适用。
- `upload_status`：pending=待上传；uploading=上传中；done=已完成；failed=失败。

### 业务说明

保存用户对具体干预项的反馈及上传重试状态。

## 9. 表：`cvd_risk_history` 本地 CVD 风险历史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `cvd_risk_history` |
| 中文名称 | 本地 CVD 风险历史表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android CVD 风险 |
| 业务作用 | 按用户和自然日保存已确认、非 Mock 的云端 CVD/RDI-16 风险结果。 |
| 主键 | `user_id, evaluated_on` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、index_cvd_risk_history_user_day | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 2 | `evaluated_on` | 评估日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、index_cvd_risk_history_user_day | 否 | 否 | — | 已确认风险结果所属的用户本地自然日。 |
| 3 | `risk_score` | 风险分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型返回的风险数值；解释范围和概率语义必须以模型契约为准。 |
| 4 | `risk_level` | 风险等级 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型基于风险分数返回的离散等级；完整枚举待模型契约确认。 |
| 5 | `evaluated_at` | 评估时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型或规则完成评估的时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `user_id, evaluated_on` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `index_cvd_risk_history_user_day` | `user_id, evaluated_on` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `cvd_risk_history.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

按用户和自然日保存已确认、非 Mock 的云端 CVD/RDI-16 风险结果。

## 10. 表：`health_chat_conversations` 本地健康问答会话表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `health_chat_conversations` |
| 中文名称 | 本地健康问答会话表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 健康问答 |
| 业务作用 | 按用户保存健康问答会话列表、激活和逻辑删除状态。 |
| 主键 | `user_id, conversation_id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、index_health_chat_conversations_user_id_updated_at、index_health_chat_conversations_user_id_is_active | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 2 | `conversation_id` | 会话 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否/待确认 | — | 标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。 |
| 3 | `title` | 标题 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 4 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 5 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_health_chat_conversations_user_id_updated_at | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |
| 6 | `is_active` | 是否活动会话 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_health_chat_conversations_user_id_is_active | 否 | 否 | — | 标识该用户当前正在使用的健康问答会话。 |
| 7 | `is_deleted` | 是否逻辑删除 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识本地会话是否已被用户逻辑删除。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `user_id, conversation_id` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `index_health_chat_conversations_user_id_updated_at` | `user_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `index_health_chat_conversations_user_id_is_active` | `user_id, is_active` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `health_chat_conversations.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

按用户保存健康问答会话列表、激活和逻辑删除状态。

## 11. 表：`health_chat_messages` 本地健康问答消息表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `health_chat_messages` |
| 中文名称 | 本地健康问答消息表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 健康问答 |
| 业务作用 | 在请求服务端前先保存用户消息，并跟踪请求、模型和投递状态。 |
| 主键 | `message_id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `message_id` | 待确认 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_health_chat_messages_user_id_created_at、index_health_chat_messages_user_id_conversation_id_created_at、index_health_chat_messages_user_id_request_id | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `conversation_id` | 会话 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_health_chat_messages_user_id_conversation_id_created_at | 否 | 逻辑→health_chat_conversations.conversation_id | — | 标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。 |
| 4 | `request_id` | 请求幂等 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_health_chat_messages_user_id_request_id | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 5 | `role` | 消息角色 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识健康问答消息发送方角色；服务端和本地会话代码据此组装上下文。 |
| 6 | `content` | 消息内容 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前健康问答消息正文。 |
| 7 | `delivery_status` | 消息投递状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本地健康问答消息发送到服务端的状态。 |
| 8 | `provider` | 服务提供方 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生消息、模型结果或设备数据的 Provider。 |
| 9 | `model_version` | 模型版本 | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 10 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_health_chat_messages_user_id_created_at、index_health_chat_messages_user_id_conversation_id_created_at | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `message_id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_health_chat_messages_user_id_created_at` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `index_health_chat_messages_user_id_conversation_id_created_at` | `user_id, conversation_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `index_health_chat_messages_user_id_request_id` | `user_id, request_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `health_chat_messages.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。
- `health_chat_messages.(conversation_id)` → `health_chat_conversations.(conversation_id)`：逻辑外键；同一用户作用域下的本地会话。

### 枚举与约束

- `role`：状态/类型类字段，完整枚举值待确认。

### 业务说明

在请求服务端前先保存用户消息，并跟踪请求、模型和投递状态。

## 12. 表：`rdi_daily_snapshots` RDI 每日快照表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rdi_daily_snapshots` |
| 中文名称 | RDI 每日快照表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RDI |
| 业务作用 | 保存本地 RDI 规则引擎每日快照；不替代云端 CVD 临床风险。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rdi_daily_snapshots_user_id_scored_on | index_rdi_daily_snapshots_user_id_scored_on、index_rdi_daily_snapshots_user_id_updated_at | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rdi_daily_snapshots_user_id_scored_on | index_rdi_daily_snapshots_user_id_scored_on | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 4 | `raw_score` | 原始分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 平滑或展示转换前的当日算法分数。 |
| 5 | `display_score` | 展示分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经过规定平滑后用于产品展示的分数。 |
| 6 | `data_confidence` | 数据可信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 算法对当前输入覆盖和质量的综合可信度。 |
| 7 | `status` | 状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `is_mock` | 是否模拟数据 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 明确标识结果是否来自 Mock/合成路径；生产结果不得为真。 |
| 9 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 10 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 11 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rdi_daily_snapshots_user_id_updated_at | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rdi_daily_snapshots_user_id_scored_on` | `user_id, scored_on` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `index_rdi_daily_snapshots_user_id_updated_at` | `user_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |

### 关联关系

- `rdi_daily_snapshots.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存本地 RDI 规则引擎每日快照；不替代云端 CVD 临床风险。

## 13. 表：`rdi_contribution_records` RDI 因素贡献表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rdi_contribution_records` |
| 中文名称 | RDI 因素贡献表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RDI |
| 业务作用 | 保存每日 RDI 快照的逐因素证据、置信度和贡献分。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `snapshot_id` | 快照记录 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rdi_contribution_records_snapshot_id | 否 | 逻辑→rdi_daily_snapshots.id | — | 逻辑关联本业务域的快照主记录。 |
| 3 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rdi_contribution_records_user_id_scored_on | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rdi_contribution_records_user_id_scored_on | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 5 | `factor_code` | 因素编码 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RDI 因素的稳定代码。 |
| 6 | `domain` | 健康领域 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RHI/RDI 因素所属健康领域；RHI 五领域枚举见约束说明。 |
| 7 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 8 | `current_value` | 当前值 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前因素或指标参与计算时使用的实际值。 |
| 9 | `baseline_value` | 基线值 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用于与当前值比较的个人或研究基线值。 |
| 10 | `unit` | 计量单位 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 11 | `raw_points` | 原始贡献分 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 乘入置信度等修正前的因素贡献分。 |
| 12 | `confidence` | 置信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 13 | `final_points` | 最终贡献分 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 考虑置信度和规则修正后实际使用的贡献分。 |
| 14 | `evidence_text` | 证据说明 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 解释当前因素贡献所依据的用户数据。 |
| 15 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 16 | `source_factor_id` | 来源因素 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rdi_contribution_records_source_factor_id | 否 | 否/待确认 | — | 关联产生当前贡献的稳定来源因素。 |
| 17 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rdi_contribution_records_snapshot_id` | `snapshot_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_rdi_contribution_records_user_id_scored_on` | `user_id, scored_on` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_rdi_contribution_records_source_factor_id` | `source_factor_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `rdi_contribution_records.(snapshot_id)` → `rdi_daily_snapshots.(id)`：逻辑外键；RDI 快照的逐因素明细。
- `rdi_contribution_records.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存每日 RDI 快照的逐因素证据、置信度和贡献分。

## 14. 表：`rdi_baselines` RDI 个人基线表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rdi_baselines` |
| 中文名称 | RDI 个人基线表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RDI |
| 业务作用 | 保存按用户和因素版本化、冻结期内不覆盖的个人稳健基线。 |
| 主键 | `user_id, factor_code, version` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、index_rdi_baselines_user_id_factor_code | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 2 | `factor_code` | 因素编码 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、index_rdi_baselines_user_id_factor_code | 否 | 否 | — | RDI 因素的稳定代码。 |
| 3 | `baseline_value` | 基线值 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用于与当前值比较的个人或研究基线值。 |
| 4 | `mad` | 中位数绝对偏差 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个人基线的稳健离散程度指标。 |
| 5 | `established_on` | 基线建立日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个人基线首次达到建立条件的本地日期。 |
| 6 | `frozen_until` | 基线冻结截止日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 在此日期前保持基线不变，以维持历史可比性。 |
| 7 | `version` | 版本 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 是 | 否 | — | 记录或配置版本；是否为乐观锁需结合实体 @Version 判断。 |
| 8 | `status` | 状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 9 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 10 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `user_id, factor_code, version` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `index_rdi_baselines_user_id_factor_code` | `user_id, factor_code` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `rdi_baselines.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存按用户和因素版本化、冻结期内不覆盖的个人稳健基线。

## 15. 表：`rdi_confirmed_labs` 已确认化验锚点表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rdi_confirmed_labs` |
| 中文名称 | 已确认化验锚点表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RDI |
| 业务作用 | 保存用户确认后的化验指标锚点；未确认 OCR 不计入评分。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `marker_code` | 化验指标代码 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认化验指标的稳定代码，例如 LDL_C。 |
| 4 | `measured_value` | 实测值 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的化验或临床测量值，必须结合 unit 解释。 |
| 5 | `unit` | 计量单位 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 6 | `measured_at` | 测量时间 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 健康指标实际测量时间。 |
| 7 | `control_trend` | 控制支持趋势 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 近期控制行为的支持趋势分，不替代临床实测值。 |
| 8 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 9 | `confidence` | 置信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 10 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 11 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rdi_confirmed_labs.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存用户确认后的化验指标锚点；未确认 OCR 不计入评分。

## 16. 表：`rdi_confirmed_meals` 已确认餐食锚点表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rdi_confirmed_meals` |
| 中文名称 | 已确认餐食锚点表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RDI |
| 业务作用 | 保存用户确认后的餐食营养区间与餐食影响证据。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `meal_type` | 餐次类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食所属的早餐、午餐、晚餐或加餐类别。 |
| 4 | `kcal_low` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `kcal_high` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `protein_low` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `protein_high` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `fat_low` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `fat_high` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `sodium_low` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `sodium_high` | 待确认 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `meal_impact` | 餐食影响分 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RDI 规则使用的已确认单餐影响分。 |
| 13 | `reason_text` | 原因说明 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 解释餐食影响或业务决策的文本。 |
| 14 | `recorded_at` | 记录时间 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 业务事件发生或数据记录时间；具体时区/单位见存储域说明。 |
| 15 | `confidence` | 置信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 16 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 17 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rdi_confirmed_meals.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存用户确认后的餐食营养区间与餐食影响证据。

## 17. 表：`rhi_manual_health_inputs` RHI 手工健康输入表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rhi_manual_health_inputs` |
| 中文名称 | RHI 手工健康输入表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RHI |
| 业务作用 | 保存久坐、腰围、VO2max、化验和经确认袖带血压等用户手填输入。 |
| 主键 | `user_id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 2 | `sedentary_hours_per_day` | 日均久坐时长 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的日均久坐小时数。 |
| 3 | `waist_circumference_cm` | 腰围 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的腰围，单位厘米。 |
| 4 | `vo2_max_ml_kg_min` | 最大摄氧量 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 正式 VO2max，单位 ml/kg/min。 |
| 5 | `hba1c_percent` | 糖化血红蛋白 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 HbA1c 百分比。 |
| 6 | `egfr_ml_min_1_73m2` | 估算肾小球滤过率 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 eGFR，单位 ml/min/1.73m²。 |
| 7 | `cuff_sbp_7d_mean` | 7 日袖带收缩压均值 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认上臂袖带测量的 3–7 日收缩压均值。 |
| 8 | `cuff_dbp_7d_mean` | 7 日袖带舒张压均值 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认上臂袖带测量的 3–7 日舒张压均值。 |
| 9 | `cuff_valid_days` | 袖带有效天数 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 计算袖带血压均值时包含的有效自然日数。 |
| 10 | `cuff_confirmed` | 袖带血压是否确认 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 只有用户确认且满足规则的上臂袖带血压才进入正式特征。 |
| 11 | `fasting_glucose_mmol_l` | 空腹血糖 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的空腹血糖，单位 mmol/L。 |
| 12 | `total_cholesterol_mmol_l` | 总胆固醇 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的总胆固醇，单位 mmol/L。 |
| 13 | `ldl_mmol_l` | 低密度脂蛋白胆固醇 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 LDL-C，单位 mmol/L。 |
| 14 | `hdl_mmol_l` | 高密度脂蛋白胆固醇 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 HDL-C，单位 mmol/L。 |
| 15 | `triglycerides_mmol_l` | 甘油三酯 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的甘油三酯，单位 mmol/L。 |
| 16 | `lab_confirmed` | 化验是否确认 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 只有用户确认且带日期的医院化验值才进入正式特征。 |
| 17 | `lab_recorded_at` | 化验日期时间 | `INTEGER` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认医院化验报告的记录时间。 |
| 18 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `user_id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rhi_manual_health_inputs.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存久坐、腰围、VO2max、化验和经确认袖带血压等用户手填输入。

## 18. 表：`rhi_daily_health_index` RHI 每日健康指数表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rhi_daily_health_index` |
| 中文名称 | RHI 每日健康指数表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RHI |
| 业务作用 | 保存每个用户每日唯一的 RHI 总分、可信度、冷启动状态和算法版本。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_health_index_user_id_scored_on | index_rhi_daily_health_index_user_id_scored_on、index_rhi_daily_health_index_user_id_updated_at | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_health_index_user_id_scored_on | index_rhi_daily_health_index_user_id_scored_on | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 4 | `raw_score` | 原始分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 平滑或展示转换前的当日算法分数。 |
| 5 | `display_score` | 展示分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经过规定平滑后用于产品展示的分数。 |
| 6 | `data_confidence` | 数据可信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 算法对当前输入覆盖和质量的综合可信度。 |
| 7 | `status` | 状态 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | provisional=少于 7 个有效日；initial=7–13 个有效日；baseline_confirmed=14–27 个有效日；confirmed=至少 28 个有效日 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `product_tier` | 产品数据层级 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RHI 根据当前可用证据确定的 LITE/STANDARD/CLINICAL 数据层级。 |
| 9 | `available_days` | 有效天数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 评分回看窗口内具有可用证据的天数。 |
| 10 | `available_feature_count` | 可用特征数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本次评分实际提取到的有效特征数量。 |
| 11 | `smoothing_alpha` | 平滑系数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 原始分与历史展示分合并时使用的平滑参数。 |
| 12 | `algorithm_version` | 算法版本 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 13 | `calculation_source` | 计算来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识当前 RHI 快照由哪个受控计算路径产生。 |
| 14 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 15 | `updated_at` | 更新时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rhi_daily_health_index_user_id_updated_at | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rhi_daily_health_index_user_id_scored_on` | `user_id, scored_on` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `index_rhi_daily_health_index_user_id_updated_at` | `user_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |

### 关联关系

- `rhi_daily_health_index.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `status`：provisional=少于 7 个有效日；initial=7–13 个有效日；baseline_confirmed=14–27 个有效日；confirmed=至少 28 个有效日。

### 业务说明

保存每个用户每日唯一的 RHI 总分、可信度、冷启动状态和算法版本。

## 19. 表：`rhi_daily_domain_score` RHI 每日领域分表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rhi_daily_domain_score` |
| 中文名称 | RHI 每日领域分表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RHI |
| 业务作用 | 保存 RHI 日快照的五领域分解；无有效指标的领域分数保持 NULL。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `index_id` | RHI 指数记录 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rhi_daily_domain_score_index_id | 否 | 逻辑→rhi_daily_health_index.id | — | 逻辑关联 Room rhi_daily_health_index.id。 |
| 3 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_domain_score_user_id_scored_on_domain | index_rhi_daily_domain_score_user_id_scored_on_domain | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_domain_score_user_id_scored_on_domain | index_rhi_daily_domain_score_user_id_scored_on_domain | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 5 | `domain` | 健康领域 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_domain_score_user_id_scored_on_domain | index_rhi_daily_domain_score_user_id_scored_on_domain | 否 | 否 | hemodynamic=血流动力学；activity_fitness=活动与体适能；sleep_recovery=睡眠恢复；metabolic_control=代谢控制；behavior_adherence=行为依从 | RHI/RDI 因素所属健康领域；RHI 五领域枚举见约束说明。 |
| 6 | `score` | 领域/规则分数 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前领域或规则的计算分数；空值表示该领域未参与评分。 |
| 7 | `weight` | 权重 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 领域、因素或规则参与汇总计算时使用的权重。 |
| 8 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rhi_daily_domain_score_index_id` | `index_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_rhi_daily_domain_score_user_id_scored_on_domain` | `user_id, scored_on, domain` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rhi_daily_domain_score.(index_id)` → `rhi_daily_health_index.(id)`：逻辑外键；RHI 日指数的领域分解。
- `rhi_daily_domain_score.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `domain`：hemodynamic=血流动力学；activity_fitness=活动与体适能；sleep_recovery=睡眠恢复；metabolic_control=代谢控制；behavior_adherence=行为依从。

### 业务说明

保存 RHI 日快照的五领域分解；无有效指标的领域分数保持 NULL。

## 20. 表：`rhi_daily_feature_snapshot` RHI 每日特征快照表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rhi_daily_feature_snapshot` |
| 中文名称 | RHI 每日特征快照表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RHI |
| 业务作用 | 保存产生每日 RHI 的特征值、置信度和个人基线统计。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `index_id` | RHI 指数记录 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rhi_daily_feature_snapshot_index_id | 否 | 逻辑→rhi_daily_health_index.id | — | 逻辑关联 Room rhi_daily_health_index.id。 |
| 3 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_feature_snapshot_user_id_scored_on_feature | index_rhi_daily_feature_snapshot_user_id_scored_on_feature | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_feature_snapshot_user_id_scored_on_feature | index_rhi_daily_feature_snapshot_user_id_scored_on_feature | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 5 | `feature` | 特征名称 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_daily_feature_snapshot_user_id_scored_on_feature | index_rhi_daily_feature_snapshot_user_id_scored_on_feature | 否 | 否 | — | RHI 32 维协议中的稳定特征字段名。 |
| 6 | `value` | 记录值 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存通用健康记录的值；具体类型由同表 type 和 unit 解释。 |
| 7 | `confidence` | 置信度 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 8 | `baseline_median` | 基线中位数 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个人历史基线的稳健中位数。 |
| 9 | `baseline_mad` | 基线 MAD | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个人历史基线的中位数绝对偏差。 |
| 10 | `baseline_sample_count` | 基线样本数 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 建立当前个人基线时使用的有效样本数量。 |
| 11 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rhi_daily_feature_snapshot_index_id` | `index_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_rhi_daily_feature_snapshot_user_id_scored_on_feature` | `user_id, scored_on, feature` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rhi_daily_feature_snapshot.(index_id)` → `rhi_daily_health_index.(id)`：逻辑外键；RHI 日指数的特征证据。
- `rhi_daily_feature_snapshot.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存产生每日 RHI 的特征值、置信度和个人基线统计。

## 21. 表：`rhi_data_quality_snapshot` RHI 数据质量快照表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rhi_data_quality_snapshot` |
| 中文名称 | RHI 数据质量快照表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android RHI |
| 业务作用 | 保存每日 RHI 的缺失字段、低置信字段、质量警告和设备变化标志。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `index_id` | RHI 指数记录 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_rhi_data_quality_snapshot_index_id | 否 | 逻辑→rhi_daily_health_index.id | — | 逻辑关联 Room rhi_daily_health_index.id。 |
| 3 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_data_quality_snapshot_user_id_scored_on | index_rhi_data_quality_snapshot_user_id_scored_on | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `scored_on` | 评分日期 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 联合唯一:index_rhi_data_quality_snapshot_user_id_scored_on | index_rhi_data_quality_snapshot_user_id_scored_on | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 5 | `confidence_score` | 置信度分数 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RHI 数据质量的数值化可信度。 |
| 6 | `confidence_grade` | 置信度等级 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | A=置信度 ≥ 0.85；B=0.70–0.8499；C=0.50–0.6999；D=置信度 < 0.50 | 由 confidence_score 映射得到的 A–D 等级。 |
| 7 | `missing_fields` | 缺失字段 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 逗号分隔的缺失特征名；空字符串表示无缺失。 |
| 8 | `low_confidence_fields` | 低置信字段 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 逗号分隔的低置信特征名。 |
| 9 | `warning_codes` | 质量警告码 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 逗号分隔的稳定质量警告代码。 |
| 10 | `warning_messages` | 质量警告说明 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 与 warning_codes 对应的人类可读质量说明。 |
| 11 | `device_change_detected` | 是否检测到设备变化 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识评分窗口内是否发现可能影响可比性的设备变更。 |
| 12 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_rhi_data_quality_snapshot_index_id` | `index_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_rhi_data_quality_snapshot_user_id_scored_on` | `user_id, scored_on` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rhi_data_quality_snapshot.(index_id)` → `rhi_daily_health_index.(id)`：逻辑外键；RHI 日指数的数据质量证据。
- `rhi_data_quality_snapshot.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `confidence_grade`：A=置信度 ≥ 0.85；B=0.70–0.8499；C=0.50–0.6999；D=置信度 < 0.50。

### 业务说明

保存每日 RHI 的缺失字段、低置信字段、质量警告和设备变化标志。

## 22. 表：`diet_records` 本地饮食记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `diet_records` |
| 中文名称 | 本地饮食记录表 |
| 所属数据库 | `rehealth-local.db` |
| 所属模块 | Android 饮食 |
| 业务作用 | 保存手工或经确认拍照产生的餐食，随后通过遥测离线队列上传。 |
| 主键 | `id` |
| 存储引擎 | SQLite / Room |
| 数据量级 | 未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema） |
| 是否核心表 | 是 |
| 结构依据 | Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16 |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_diet_records_user_id_consumed_at | 是 | 逻辑→认证用户.sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `consumed_at` | 进餐时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | index_diet_records_user_id_consumed_at | 否 | 否 | — | 用户实际进餐或记录餐食的时间。 |
| 4 | `meal_type` | 餐次类型 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | breakfast=早餐；lunch=午餐；dinner=晚餐；snack=加餐 | 餐食所属的早餐、午餐、晚餐或加餐类别。 |
| 5 | `description` | 描述 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前记录的业务内容描述。 |
| 6 | `calories_kcal` | 热量 | `REAL` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食或活动能量，单位千卡。 |
| 7 | `protein_grams` | 蛋白质 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食蛋白质估计值，单位克。 |
| 8 | `carbohydrate_grams` | 碳水化合物 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食碳水化合物估计值，单位克。 |
| 9 | `fat_grams` | 脂肪 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食脂肪估计值，单位克。 |
| 10 | `fiber_grams` | 膳食纤维 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食膳食纤维估计值，单位克。 |
| 11 | `sodium_milligrams` | 钠 | `REAL` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食钠估计值，单位毫克。 |
| 12 | `source` | 数据来源 | `TEXT` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 13 | `created_at` | 创建时间 | `INTEGER` | 不适用（SQLite 动态类型） | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 14 | `upload_batch_id` | 上传批次 ID | `TEXT` | 不适用（SQLite 动态类型） | 是 | `无/NULL` | 否 | 否 | 否 | index_diet_records_upload_batch_id | 否 | 否/待确认 | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `index_diet_records_user_id_consumed_at` | `user_id, consumed_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_diet_records_upload_batch_id` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `diet_records.(user_id)` → `认证用户.(sys_user.id)`：逻辑外键；跨端逻辑归属，不存在 SQLite 外键。

### 枚举与约束

- `meal_type`：breakfast=早餐；lunch=午餐；dinner=晚餐；snack=加餐。
- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存手工或经确认拍照产生的餐食，随后通过遥测离线队列上传。
