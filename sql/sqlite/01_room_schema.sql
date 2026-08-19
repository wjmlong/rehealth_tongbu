-- Android Room 当前结构快照，数据库版本 19，文件名 rehealth-local.db
-- SQLite 不支持 COMMENT ON；每张表和字段的中文注释使用相邻 -- 注释表达。
PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;

-- ============================================================================
-- 表：health_records
-- 中文名称：通用健康记录表
-- 业务用途：早期通用健康记录骨架；当前未在 AppDatabase 暴露 DAO，实际用途待确认。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 type：类型；当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。
-- 字段 value：记录值；保存通用健康记录的值；具体类型由同表 type 和 unit 解释。
-- 字段 unit：计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。
-- 字段 recordedAt：TODO：字段中文业务含义待确认
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
CREATE TABLE IF NOT EXISTS `health_records` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `unit` TEXT NOT NULL, `recordedAt` INTEGER NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：attribution_logs
-- 中文名称：本地归因审计骨架表
-- 业务用途：保存本地归因完整度、证据等级和审计哈希；当前未在 AppDatabase 暴露 DAO。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 date：日期；当前本地记录所属的自然日。
-- 字段 completeness：数据完整度；归因审计中记录的输入完整度。
-- 字段 evidenceGrade：证据等级；本地归因审计使用的证据等级。
-- 字段 auditHash：审计哈希；本地归因证据的完整性摘要。
CREATE TABLE IF NOT EXISTS `attribution_logs` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `completeness` REAL NOT NULL, `evidenceGrade` TEXT NOT NULL, `auditHash` TEXT NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：ring_measurements
-- 中文名称：可穿戴测量表
-- 业务用途：保存按用户和设备隔离的心率、血氧、血压等规范化标量测量。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 metric_type：指标类型；标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。
-- 字段 measured_at：测量时间；健康指标实际测量时间。
-- 字段 primary_value：主测量值；规范化测量的主要数值，例如单值指标或血压收缩压分量。
-- 字段 secondary_value：次测量值；规范化测量的可选第二数值，例如成对测量的第二分量。
-- 字段 unit：计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。
-- 字段 quality：质量值；Provider 提供或规范化后的测量质量；具体量纲按指标实现确认。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 raw_payload：原始扩展载荷；本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。
-- 字段 owner_user_id：所属用户 ID；Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。
-- 字段 device_id：稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。
CREATE TABLE IF NOT EXISTS `ring_measurements` (`id` TEXT NOT NULL, `metric_type` TEXT NOT NULL, `measured_at` INTEGER NOT NULL, `primary_value` REAL NOT NULL, `secondary_value` REAL, `unit` TEXT NOT NULL, `quality` INTEGER, `source` TEXT NOT NULL, `raw_payload` TEXT, `owner_user_id` TEXT, `device_id` TEXT, PRIMARY KEY(`id`));
-- 索引 index_ring_measurements_metric_type_measured_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_measurements_metric_type_measured_at` ON `ring_measurements` (`metric_type`, `measured_at`);
-- 索引 index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at` ON `ring_measurements` (`owner_user_id`, `device_id`, `source`, `metric_type`, `measured_at`);

-- ============================================================================
-- 表：ring_sleep_sessions
-- 中文名称：可穿戴睡眠会话表
-- 业务用途：保存设备睡眠阶段、厂商总睡眠时长及用户/设备归属。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 started_at：开始时间；会话、活动或信号时间窗开始时间。
-- 字段 ended_at：结束时间；会话、活动或信号时间窗结束时间。
-- 字段 deep_minutes：深睡时长；深睡阶段分钟数。
-- 字段 light_minutes：浅睡时长；浅睡阶段分钟数。
-- 字段 awake_minutes：清醒时长；睡眠会话内清醒分钟数。
-- 字段 rem_minutes：REM 时长；快速眼动睡眠阶段分钟数。
-- 字段 interruption_minutes：中断时长；睡眠中断分钟数。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 raw_payload：原始扩展载荷；本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。
-- 字段 total_sleep_minutes：厂商总睡眠时长；Provider 明确返回的权威睡眠总分钟数；无值时不以起止跨度替代。
-- 字段 owner_user_id：所属用户 ID；Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。
-- 字段 device_id：稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。
CREATE TABLE IF NOT EXISTS `ring_sleep_sessions` (`id` TEXT NOT NULL, `started_at` INTEGER NOT NULL, `ended_at` INTEGER NOT NULL, `deep_minutes` INTEGER NOT NULL, `light_minutes` INTEGER NOT NULL, `awake_minutes` INTEGER NOT NULL, `rem_minutes` INTEGER NOT NULL, `interruption_minutes` INTEGER NOT NULL, `source` TEXT NOT NULL, `raw_payload` TEXT, `total_sleep_minutes` INTEGER, `owner_user_id` TEXT, `device_id` TEXT, PRIMARY KEY(`id`));
-- 索引 index_ring_sleep_sessions_owner_user_id_ended_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_sleep_sessions_owner_user_id_ended_at` ON `ring_sleep_sessions` (`owner_user_id`, `ended_at`);

-- ============================================================================
-- 表：ring_activities
-- 中文名称：可穿戴活动表
-- 业务用途：保存步数、距离、热量、活动时长和平均心率等活动事实。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 started_at：开始时间；会话、活动或信号时间窗开始时间。
-- 字段 ended_at：结束时间；会话、活动或信号时间窗结束时间。
-- 字段 activity_type：活动类型；标识活动记录的类型；具体允许值由设备 Provider 映射定义。
-- 字段 steps：步数；活动时间窗或自然日内的设备步数。
-- 字段 distance_meters：距离；活动距离，单位米。
-- 字段 calories_kcal：热量；餐食或活动能量，单位千卡。
-- 字段 duration_minutes：持续时长；活动持续分钟数。
-- 字段 average_heart_rate：平均心率；活动或 ECG 测量期间的平均心率。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 raw_payload：原始扩展载荷；本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。
-- 字段 owner_user_id：所属用户 ID；Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。
-- 字段 device_id：稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。
CREATE TABLE IF NOT EXISTS `ring_activities` (`id` TEXT NOT NULL, `started_at` INTEGER NOT NULL, `ended_at` INTEGER, `activity_type` TEXT NOT NULL, `steps` INTEGER NOT NULL, `distance_meters` REAL NOT NULL, `calories_kcal` REAL NOT NULL, `duration_minutes` INTEGER NOT NULL, `average_heart_rate` REAL, `source` TEXT NOT NULL, `raw_payload` TEXT, `owner_user_id` TEXT, `device_id` TEXT, PRIMARY KEY(`id`));
-- 索引 index_ring_activities_owner_user_id_started_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_activities_owner_user_id_started_at` ON `ring_activities` (`owner_user_id`, `started_at`);

-- ============================================================================
-- 表：ring_signal_chunks
-- 中文名称：本地信号与 ECG 分块表
-- 业务用途：保存仅限本机使用的信号/ECG 波形和导联、采样、校准元数据；波形不上传云端。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 signal_type：信号类型；标识信号/ECG 分块或元数据的信号类别。
-- 字段 started_at：开始时间；会话、活动或信号时间窗开始时间。
-- 字段 sample_rate_hz：采样率；信号采样频率，单位 Hz。
-- 字段 sample_count：采样点数；当前信号块包含的样本数量。
-- 字段 encoding：信号编码；本地信号 payload 的编码格式。
-- 字段 payload：信号载荷；Android 本地保存的信号/ECG 二进制波形；不进入云端遥测上传。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 draw_frequency_hz：绘制频率；ECG 界面绘制或重采样频率，单位 Hz。
-- 字段 duration_seconds：信号时长；信号/ECG 记录持续秒数。
-- 字段 lead_type：导联类型；ECG 记录使用的导联类型。
-- 字段 ecg_type：ECG 类型；厂商 SDK 返回的 ECG 类型代码；具体枚举待 SDK 证据确认。
-- 字段 calibration_type：校准方式；ADC 换算到 mV 时使用的校准方式。
-- 字段 average_heart_rate：平均心率；活动或 ECG 测量期间的平均心率。
-- 字段 contact_quality：接触质量；ECG 电极接触质量状态；具体枚举待 SDK 证据确认。
-- 字段 owner_user_id：所属用户 ID；Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。
-- 字段 device_id：稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。
CREATE TABLE IF NOT EXISTS `ring_signal_chunks` (`id` TEXT NOT NULL, `signal_type` TEXT NOT NULL, `started_at` INTEGER NOT NULL, `sample_rate_hz` INTEGER, `sample_count` INTEGER NOT NULL, `encoding` TEXT NOT NULL, `payload` BLOB NOT NULL, `source` TEXT NOT NULL, `draw_frequency_hz` INTEGER, `duration_seconds` INTEGER, `lead_type` TEXT, `ecg_type` INTEGER, `calibration_type` TEXT, `average_heart_rate` INTEGER, `contact_quality` TEXT, `owner_user_id` TEXT, `device_id` TEXT, PRIMARY KEY(`id`));
-- 索引 index_ring_signal_chunks_signal_type_started_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_signal_chunks_signal_type_started_at` ON `ring_signal_chunks` (`signal_type`, `started_at`);
-- 索引 index_ring_signal_chunks_owner_user_id_signal_type_started_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_ring_signal_chunks_owner_user_id_signal_type_started_at` ON `ring_signal_chunks` (`owner_user_id`, `signal_type`, `started_at`);

-- ============================================================================
-- 表：sync_upload_queue
-- 中文名称：离线上传队列表
-- 业务用途：保存先落库后上传的持久化任务，支持认证暂停、退避重试和死信。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 kind：任务种类；标识离线队列载荷的业务种类，由上传调度器选择对应处理客户端。
-- 字段 payload_json：载荷 JSON；保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 attempts：已尝试次数；离线队列执行当前任务的累计尝试次数，用于退避和死信判断。
-- 字段 last_error：最近错误；最近一次队列处理失败的脱敏错误信息。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 next_retry_at：下次重试时间；队列项允许再次处理的最早时间，支持指数退避。
CREATE TABLE IF NOT EXISTS `sync_upload_queue` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `payload_json` TEXT NOT NULL, `status` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `last_error` TEXT, `created_at` INTEGER NOT NULL, `next_retry_at` INTEGER NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：intervention_feedback_queue
-- 中文名称：干预反馈上传队列表
-- 业务用途：保存用户对具体干预项的反馈及上传重试状态。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 owner_user_id：所属用户 ID；Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。
-- 字段 intervention_id：干预行动 ID；标识用户反馈所针对的具体干预行动。
-- 字段 binding_id：TODO：字段中文业务含义待确认
-- 字段 tenant_id：租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。
-- 字段 plan_item_id：TODO：字段中文业务含义待确认
-- 字段 occurrence_id：TODO：字段中文业务含义待确认
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 note：备注；保存用户或业务操作的可选补充说明。
-- 字段 expected_count：TODO：字段中文业务含义待确认
-- 字段 completed_count：TODO：字段中文业务含义待确认
-- 字段 verification_type：TODO：字段中文业务含义待确认
-- 字段 checked_at：反馈打卡时间；用户对干预行动提交反馈的时间。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 upload_status：上传状态；当前本地记录的离线上传生命周期状态。
-- 字段 upload_attempts：上传尝试次数；反馈队列上传当前记录的累计尝试次数。
-- 字段 last_error：最近错误；最近一次队列处理失败的脱敏错误信息。
-- 字段 next_retry_at：下次重试时间；队列项允许再次处理的最早时间，支持指数退避。
CREATE TABLE IF NOT EXISTS `intervention_feedback_queue` (`id` TEXT NOT NULL, `owner_user_id` TEXT, `intervention_id` TEXT NOT NULL, `binding_id` TEXT, `tenant_id` INTEGER, `plan_item_id` TEXT, `occurrence_id` TEXT, `status` TEXT NOT NULL, `note` TEXT, `expected_count` REAL, `completed_count` REAL, `verification_type` TEXT NOT NULL, `checked_at` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `upload_status` TEXT NOT NULL, `upload_attempts` INTEGER NOT NULL, `last_error` TEXT, `next_retry_at` INTEGER NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：cvd_risk_history
-- 中文名称：本地 CVD 风险历史表
-- 业务用途：按用户和自然日保存已确认、非 Mock 的云端 CVD/RDI-16 风险结果。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 evaluated_on：评估日期；已确认风险结果所属的用户本地自然日。
-- 字段 risk_score：风险分数；模型返回的风险数值；解释范围和概率语义必须以模型契约为准。
-- 字段 risk_level：风险等级；模型基于风险分数返回的离散等级；完整枚举待模型契约确认。
-- 字段 evaluated_at：评估时间；模型或规则完成评估的时间。
CREATE TABLE IF NOT EXISTS `cvd_risk_history` (`user_id` TEXT NOT NULL, `evaluated_on` TEXT NOT NULL, `risk_score` REAL NOT NULL, `risk_level` TEXT, `evaluated_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`, `evaluated_on`));
-- 索引 index_cvd_risk_history_user_day：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_cvd_risk_history_user_day` ON `cvd_risk_history` (`user_id`, `evaluated_on`);

-- ============================================================================
-- 表：health_chat_conversations
-- 中文名称：本地健康问答会话表
-- 业务用途：按用户保存健康问答会话列表、激活和逻辑删除状态。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 conversation_id：会话 ID；标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。
-- 字段 title：标题；当前会话、研究、报告或业务对象的展示标题。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
-- 字段 is_active：是否活动会话；标识该用户当前正在使用的健康问答会话。
-- 字段 is_deleted：是否逻辑删除；标识本地会话是否已被用户逻辑删除。
CREATE TABLE IF NOT EXISTS `health_chat_conversations` (`user_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `title` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `is_active` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, PRIMARY KEY(`user_id`, `conversation_id`));
-- 索引 index_health_chat_conversations_user_id_updated_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_health_chat_conversations_user_id_updated_at` ON `health_chat_conversations` (`user_id`, `updated_at`);
-- 索引 index_health_chat_conversations_user_id_is_active：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_health_chat_conversations_user_id_is_active` ON `health_chat_conversations` (`user_id`, `is_active`);

-- ============================================================================
-- 表：health_chat_messages
-- 中文名称：本地健康问答消息表
-- 业务用途：在请求服务端前先保存用户消息，并跟踪请求、模型和投递状态。
-- 字段 message_id：TODO：字段中文业务含义待确认
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 conversation_id：会话 ID；标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。
-- 字段 request_id：请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。
-- 字段 role：消息角色；标识健康问答消息发送方角色；服务端和本地会话代码据此组装上下文。
-- 字段 content：消息内容；保存当前健康问答消息正文。
-- 字段 delivery_status：消息投递状态；本地健康问答消息发送到服务端的状态。
-- 字段 provider：服务提供方；标识产生消息、模型结果或设备数据的 Provider。
-- 字段 model_version：模型版本；产生当前模型输出的版本标识。
-- 字段 created_at：创建时间；记录首次创建时间。
CREATE TABLE IF NOT EXISTS `health_chat_messages` (`message_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `request_id` TEXT, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `delivery_status` TEXT NOT NULL, `provider` TEXT, `model_version` TEXT, `created_at` INTEGER NOT NULL, PRIMARY KEY(`message_id`));
-- 索引 index_health_chat_messages_user_id_created_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_health_chat_messages_user_id_created_at` ON `health_chat_messages` (`user_id`, `created_at`);
-- 索引 index_health_chat_messages_user_id_conversation_id_created_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_health_chat_messages_user_id_conversation_id_created_at` ON `health_chat_messages` (`user_id`, `conversation_id`, `created_at`);
-- 索引 index_health_chat_messages_user_id_request_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_health_chat_messages_user_id_request_id` ON `health_chat_messages` (`user_id`, `request_id`);

-- ============================================================================
-- 表：rdi_daily_snapshots
-- 中文名称：RDI 每日快照表
-- 业务用途：保存本地 RDI 规则引擎每日快照；不替代云端 CVD 临床风险。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 raw_score：原始分数；平滑或展示转换前的当日算法分数。
-- 字段 display_score：展示分数；经过规定平滑后用于产品展示的分数。
-- 字段 data_confidence：数据可信度；算法对当前输入覆盖和质量的综合可信度。
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 is_mock：是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rdi_daily_snapshots` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `raw_score` REAL NOT NULL, `display_score` REAL NOT NULL, `data_confidence` REAL NOT NULL, `status` TEXT NOT NULL, `is_mock` INTEGER NOT NULL DEFAULT 0, `algorithm_version` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rdi_daily_snapshots_user_id_scored_on：用于当前表的业务查询或唯一性约束。
CREATE UNIQUE INDEX IF NOT EXISTS `index_rdi_daily_snapshots_user_id_scored_on` ON `rdi_daily_snapshots` (`user_id`, `scored_on`);
-- 索引 index_rdi_daily_snapshots_user_id_updated_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rdi_daily_snapshots_user_id_updated_at` ON `rdi_daily_snapshots` (`user_id`, `updated_at`);

-- ============================================================================
-- 表：rdi_contribution_records
-- 中文名称：RDI 因素贡献表
-- 业务用途：保存每日 RDI 快照的逐因素证据、置信度和贡献分。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 snapshot_id：快照记录 ID；逻辑关联本业务域的快照主记录。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 factor_code：因素编码；RDI 因素的稳定代码。
-- 字段 domain：健康领域；RHI/RDI 因素所属健康领域；RHI 五领域枚举见约束说明。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 current_value：当前值；当前因素或指标参与计算时使用的实际值。
-- 字段 baseline_value：基线值；用于与当前值比较的个人或研究基线值。
-- 字段 unit：计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。
-- 字段 raw_points：原始贡献分；乘入置信度等修正前的因素贡献分。
-- 字段 confidence：置信度；当前特征、因素、识别结果或计划的可信程度。
-- 字段 final_points：最终贡献分；考虑置信度和规则修正后实际使用的贡献分。
-- 字段 evidence_text：证据说明；解释当前因素贡献所依据的用户数据。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 source_factor_id：来源因素 ID；关联产生当前贡献的稳定来源因素。
-- 字段 created_at：创建时间；记录首次创建时间。
CREATE TABLE IF NOT EXISTS `rdi_contribution_records` (`id` TEXT NOT NULL, `snapshot_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `factor_code` TEXT NOT NULL, `domain` TEXT NOT NULL, `source` TEXT NOT NULL, `current_value` REAL NOT NULL, `baseline_value` REAL, `unit` TEXT NOT NULL, `raw_points` REAL NOT NULL, `confidence` REAL NOT NULL, `final_points` REAL NOT NULL, `evidence_text` TEXT NOT NULL, `algorithm_version` TEXT NOT NULL, `source_factor_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rdi_contribution_records_snapshot_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rdi_contribution_records_snapshot_id` ON `rdi_contribution_records` (`snapshot_id`);
-- 索引 index_rdi_contribution_records_user_id_scored_on：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rdi_contribution_records_user_id_scored_on` ON `rdi_contribution_records` (`user_id`, `scored_on`);
-- 索引 index_rdi_contribution_records_source_factor_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rdi_contribution_records_source_factor_id` ON `rdi_contribution_records` (`source_factor_id`);

-- ============================================================================
-- 表：rdi_baselines
-- 中文名称：RDI 个人基线表
-- 业务用途：保存按用户和因素版本化、冻结期内不覆盖的个人稳健基线。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 factor_code：因素编码；RDI 因素的稳定代码。
-- 字段 baseline_value：基线值；用于与当前值比较的个人或研究基线值。
-- 字段 mad：中位数绝对偏差；个人基线的稳健离散程度指标。
-- 字段 established_on：基线建立日期；个人基线首次达到建立条件的本地日期。
-- 字段 frozen_until：基线冻结截止日期；在此日期前保持基线不变，以维持历史可比性。
-- 字段 version：版本；记录或配置版本；是否为乐观锁需结合实体 @Version 判断。
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rdi_baselines` (`user_id` TEXT NOT NULL, `factor_code` TEXT NOT NULL, `baseline_value` REAL NOT NULL, `mad` REAL NOT NULL, `established_on` TEXT NOT NULL, `frozen_until` TEXT NOT NULL, `version` INTEGER NOT NULL, `status` TEXT NOT NULL, `algorithm_version` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`, `factor_code`, `version`));
-- 索引 index_rdi_baselines_user_id_factor_code：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rdi_baselines_user_id_factor_code` ON `rdi_baselines` (`user_id`, `factor_code`);

-- ============================================================================
-- 表：rdi_confirmed_labs
-- 中文名称：已确认化验锚点表
-- 业务用途：保存用户确认后的化验指标锚点；未确认 OCR 不计入评分。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 marker_code：化验指标代码；用户确认化验指标的稳定代码，例如 LDL_C。
-- 字段 measured_value：实测值；用户确认的化验或临床测量值，必须结合 unit 解释。
-- 字段 unit：计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。
-- 字段 measured_at：测量时间；健康指标实际测量时间。
-- 字段 control_trend：控制支持趋势；近期控制行为的支持趋势分，不替代临床实测值。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 confidence：置信度；当前特征、因素、识别结果或计划的可信程度。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rdi_confirmed_labs` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `marker_code` TEXT NOT NULL, `measured_value` REAL NOT NULL, `unit` TEXT NOT NULL, `measured_at` TEXT NOT NULL, `control_trend` REAL NOT NULL, `source` TEXT NOT NULL, `confidence` REAL NOT NULL, `algorithm_version` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：rdi_confirmed_meals
-- 中文名称：已确认餐食锚点表
-- 业务用途：保存用户确认后的餐食营养区间与餐食影响证据。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 meal_type：餐次类型；餐食所属的早餐、午餐、晚餐或加餐类别。
-- 字段 kcal_low：TODO：字段中文业务含义待确认
-- 字段 kcal_high：TODO：字段中文业务含义待确认
-- 字段 protein_low：TODO：字段中文业务含义待确认
-- 字段 protein_high：TODO：字段中文业务含义待确认
-- 字段 fat_low：TODO：字段中文业务含义待确认
-- 字段 fat_high：TODO：字段中文业务含义待确认
-- 字段 sodium_low：TODO：字段中文业务含义待确认
-- 字段 sodium_high：TODO：字段中文业务含义待确认
-- 字段 meal_impact：餐食影响分；RDI 规则使用的已确认单餐影响分。
-- 字段 reason_text：原因说明；解释餐食影响或业务决策的文本。
-- 字段 recorded_at：记录时间；业务事件发生或数据记录时间；具体时区/单位见存储域说明。
-- 字段 confidence：置信度；当前特征、因素、识别结果或计划的可信程度。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rdi_confirmed_meals` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `meal_type` TEXT NOT NULL, `kcal_low` REAL NOT NULL, `kcal_high` REAL NOT NULL, `protein_low` REAL NOT NULL, `protein_high` REAL NOT NULL, `fat_low` REAL NOT NULL, `fat_high` REAL NOT NULL, `sodium_low` REAL NOT NULL, `sodium_high` REAL NOT NULL, `meal_impact` REAL NOT NULL, `reason_text` TEXT NOT NULL, `recorded_at` TEXT NOT NULL, `confidence` REAL NOT NULL, `algorithm_version` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`));

-- ============================================================================
-- 表：rhi_manual_health_inputs
-- 中文名称：RHI 手工健康输入表
-- 业务用途：保存久坐、腰围、VO2max、化验和经确认袖带血压等用户手填输入。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 sedentary_hours_per_day：日均久坐时长；用户确认的日均久坐小时数。
-- 字段 waist_circumference_cm：腰围；用户确认的腰围，单位厘米。
-- 字段 vo2_max_ml_kg_min：最大摄氧量；正式 VO2max，单位 ml/kg/min。
-- 字段 hba1c_percent：糖化血红蛋白；用户确认的 HbA1c 百分比。
-- 字段 egfr_ml_min_1_73m2：估算肾小球滤过率；用户确认的 eGFR，单位 ml/min/1.73m²。
-- 字段 cuff_sbp_7d_mean：7 日袖带收缩压均值；经确认上臂袖带测量的 3–7 日收缩压均值。
-- 字段 cuff_dbp_7d_mean：7 日袖带舒张压均值；经确认上臂袖带测量的 3–7 日舒张压均值。
-- 字段 cuff_valid_days：袖带有效天数；计算袖带血压均值时包含的有效自然日数。
-- 字段 cuff_confirmed：袖带血压是否确认；只有用户确认且满足规则的上臂袖带血压才进入正式特征。
-- 字段 fasting_glucose_mmol_l：空腹血糖；用户确认的空腹血糖，单位 mmol/L。
-- 字段 total_cholesterol_mmol_l：总胆固醇；用户确认的总胆固醇，单位 mmol/L。
-- 字段 ldl_mmol_l：低密度脂蛋白胆固醇；用户确认的 LDL-C，单位 mmol/L。
-- 字段 hdl_mmol_l：高密度脂蛋白胆固醇；用户确认的 HDL-C，单位 mmol/L。
-- 字段 triglycerides_mmol_l：甘油三酯；用户确认的甘油三酯，单位 mmol/L。
-- 字段 lab_confirmed：化验是否确认；只有用户确认且带日期的医院化验值才进入正式特征。
-- 字段 lab_recorded_at：化验日期时间；经确认医院化验报告的记录时间。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rhi_manual_health_inputs` (`user_id` TEXT NOT NULL, `sedentary_hours_per_day` REAL, `waist_circumference_cm` REAL, `vo2_max_ml_kg_min` REAL, `hba1c_percent` REAL, `egfr_ml_min_1_73m2` REAL, `cuff_sbp_7d_mean` REAL, `cuff_dbp_7d_mean` REAL, `cuff_valid_days` INTEGER, `cuff_confirmed` INTEGER NOT NULL, `fasting_glucose_mmol_l` REAL, `total_cholesterol_mmol_l` REAL, `ldl_mmol_l` REAL, `hdl_mmol_l` REAL, `triglycerides_mmol_l` REAL, `lab_confirmed` INTEGER NOT NULL, `lab_recorded_at` INTEGER, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`));

-- ============================================================================
-- 表：rhi_daily_health_index
-- 中文名称：RHI 每日健康指数表
-- 业务用途：保存每个用户每日唯一的 RHI 总分、可信度、冷启动状态和算法版本。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 raw_score：原始分数；平滑或展示转换前的当日算法分数。
-- 字段 display_score：展示分数；经过规定平滑后用于产品展示的分数。
-- 字段 data_confidence：数据可信度；算法对当前输入覆盖和质量的综合可信度。
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 product_tier：产品数据层级；RHI 根据当前可用证据确定的 LITE/STANDARD/CLINICAL 数据层级。
-- 字段 available_days：有效天数；评分回看窗口内具有可用证据的天数。
-- 字段 available_feature_count：可用特征数；本次评分实际提取到的有效特征数量。
-- 字段 smoothing_alpha：平滑系数；原始分与历史展示分合并时使用的平滑参数。
-- 字段 algorithm_version：算法版本；产生当前规则或算法结果的版本标识。
-- 字段 calculation_source：计算来源；标识当前 RHI 快照由哪个受控计算路径产生。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `rhi_daily_health_index` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `raw_score` REAL NOT NULL, `display_score` REAL NOT NULL, `data_confidence` REAL NOT NULL, `status` TEXT NOT NULL, `product_tier` TEXT NOT NULL, `available_days` INTEGER NOT NULL, `available_feature_count` INTEGER NOT NULL, `smoothing_alpha` REAL NOT NULL, `algorithm_version` TEXT NOT NULL, `calculation_source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rhi_daily_health_index_user_id_scored_on：用于当前表的业务查询或唯一性约束。
CREATE UNIQUE INDEX IF NOT EXISTS `index_rhi_daily_health_index_user_id_scored_on` ON `rhi_daily_health_index` (`user_id`, `scored_on`);
-- 索引 index_rhi_daily_health_index_user_id_updated_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rhi_daily_health_index_user_id_updated_at` ON `rhi_daily_health_index` (`user_id`, `updated_at`);

-- ============================================================================
-- 表：rhi_daily_domain_score
-- 中文名称：RHI 每日领域分表
-- 业务用途：保存 RHI 日快照的五领域分解；无有效指标的领域分数保持 NULL。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 index_id：RHI 指数记录 ID；逻辑关联 Room rhi_daily_health_index.id。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 domain：健康领域；RHI/RDI 因素所属健康领域；RHI 五领域枚举见约束说明。
-- 字段 score：领域/规则分数；当前领域或规则的计算分数；空值表示该领域未参与评分。
-- 字段 weight：权重；领域、因素或规则参与汇总计算时使用的权重。
-- 字段 created_at：创建时间；记录首次创建时间。
CREATE TABLE IF NOT EXISTS `rhi_daily_domain_score` (`id` TEXT NOT NULL, `index_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `domain` TEXT NOT NULL, `score` REAL, `weight` REAL NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rhi_daily_domain_score_index_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rhi_daily_domain_score_index_id` ON `rhi_daily_domain_score` (`index_id`);
-- 索引 index_rhi_daily_domain_score_user_id_scored_on_domain：用于当前表的业务查询或唯一性约束。
CREATE UNIQUE INDEX IF NOT EXISTS `index_rhi_daily_domain_score_user_id_scored_on_domain` ON `rhi_daily_domain_score` (`user_id`, `scored_on`, `domain`);

-- ============================================================================
-- 表：rhi_daily_feature_snapshot
-- 中文名称：RHI 每日特征快照表
-- 业务用途：保存产生每日 RHI 的特征值、置信度和个人基线统计。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 index_id：RHI 指数记录 ID；逻辑关联 Room rhi_daily_health_index.id。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 feature：特征名称；RHI 32 维协议中的稳定特征字段名。
-- 字段 value：记录值；保存通用健康记录的值；具体类型由同表 type 和 unit 解释。
-- 字段 confidence：置信度；当前特征、因素、识别结果或计划的可信程度。
-- 字段 baseline_median：基线中位数；个人历史基线的稳健中位数。
-- 字段 baseline_mad：基线 MAD；个人历史基线的中位数绝对偏差。
-- 字段 baseline_sample_count：基线样本数；建立当前个人基线时使用的有效样本数量。
-- 字段 created_at：创建时间；记录首次创建时间。
CREATE TABLE IF NOT EXISTS `rhi_daily_feature_snapshot` (`id` TEXT NOT NULL, `index_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `feature` TEXT NOT NULL, `value` REAL NOT NULL, `confidence` REAL NOT NULL, `baseline_median` REAL, `baseline_mad` REAL, `baseline_sample_count` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rhi_daily_feature_snapshot_index_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rhi_daily_feature_snapshot_index_id` ON `rhi_daily_feature_snapshot` (`index_id`);
-- 索引 index_rhi_daily_feature_snapshot_user_id_scored_on_feature：用于当前表的业务查询或唯一性约束。
CREATE UNIQUE INDEX IF NOT EXISTS `index_rhi_daily_feature_snapshot_user_id_scored_on_feature` ON `rhi_daily_feature_snapshot` (`user_id`, `scored_on`, `feature`);

-- ============================================================================
-- 表：rhi_data_quality_snapshot
-- 中文名称：RHI 数据质量快照表
-- 业务用途：保存每日 RHI 的缺失字段、低置信字段、质量警告和设备变化标志。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 index_id：RHI 指数记录 ID；逻辑关联 Room rhi_daily_health_index.id。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 scored_on：评分日期；评分所属本地自然日，使用 ISO-8601 日期。
-- 字段 confidence_score：置信度分数；RHI 数据质量的数值化可信度。
-- 字段 confidence_grade：置信度等级；由 confidence_score 映射得到的 A–D 等级。
-- 字段 missing_fields：缺失字段；逗号分隔的缺失特征名；空字符串表示无缺失。
-- 字段 low_confidence_fields：低置信字段；逗号分隔的低置信特征名。
-- 字段 warning_codes：质量警告码；逗号分隔的稳定质量警告代码。
-- 字段 warning_messages：质量警告说明；与 warning_codes 对应的人类可读质量说明。
-- 字段 device_change_detected：是否检测到设备变化；标识评分窗口内是否发现可能影响可比性的设备变更。
-- 字段 created_at：创建时间；记录首次创建时间。
CREATE TABLE IF NOT EXISTS `rhi_data_quality_snapshot` (`id` TEXT NOT NULL, `index_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `scored_on` TEXT NOT NULL, `confidence_score` REAL NOT NULL, `confidence_grade` TEXT NOT NULL, `missing_fields` TEXT NOT NULL, `low_confidence_fields` TEXT NOT NULL, `warning_codes` TEXT NOT NULL, `warning_messages` TEXT NOT NULL, `device_change_detected` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`));
-- 索引 index_rhi_data_quality_snapshot_index_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_rhi_data_quality_snapshot_index_id` ON `rhi_data_quality_snapshot` (`index_id`);
-- 索引 index_rhi_data_quality_snapshot_user_id_scored_on：用于当前表的业务查询或唯一性约束。
CREATE UNIQUE INDEX IF NOT EXISTS `index_rhi_data_quality_snapshot_user_id_scored_on` ON `rhi_data_quality_snapshot` (`user_id`, `scored_on`);

-- ============================================================================
-- 表：diet_records
-- 中文名称：本地饮食记录表
-- 业务用途：保存手工或经确认拍照产生的餐食，随后通过遥测离线队列上传。
-- 字段 id：主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 consumed_at：进餐时间；用户实际进餐或记录餐食的时间。
-- 字段 meal_type：餐次类型；餐食所属的早餐、午餐、晚餐或加餐类别。
-- 字段 description：描述；当前记录的业务内容描述。
-- 字段 calories_kcal：热量；餐食或活动能量，单位千卡。
-- 字段 protein_grams：蛋白质；餐食蛋白质估计值，单位克。
-- 字段 carbohydrate_grams：碳水化合物；餐食碳水化合物估计值，单位克。
-- 字段 fat_grams：脂肪；餐食脂肪估计值，单位克。
-- 字段 fiber_grams：膳食纤维；餐食膳食纤维估计值，单位克。
-- 字段 sodium_milligrams：钠；餐食钠估计值，单位毫克。
-- 字段 source：数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。
-- 字段 created_at：创建时间；记录首次创建时间。
-- 字段 upload_batch_id：上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。
CREATE TABLE IF NOT EXISTS `diet_records` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `consumed_at` INTEGER NOT NULL, `meal_type` TEXT NOT NULL, `description` TEXT NOT NULL, `calories_kcal` REAL NOT NULL, `protein_grams` REAL, `carbohydrate_grams` REAL, `fat_grams` REAL, `fiber_grams` REAL, `sodium_milligrams` REAL, `source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `upload_batch_id` TEXT, PRIMARY KEY(`id`));
-- 索引 index_diet_records_user_id_consumed_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_diet_records_user_id_consumed_at` ON `diet_records` (`user_id`, `consumed_at`);
-- 索引 index_diet_records_upload_batch_id：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_diet_records_upload_batch_id` ON `diet_records` (`upload_batch_id`);

-- ============================================================================
-- 表：pias_attribution_cache
-- 中文名称：pias_attribution_cache 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- 字段 user_id：用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。
-- 字段 status：状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。
-- 字段 history_days：TODO：字段中文业务含义待确认
-- 字段 payload_json：载荷 JSON；保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。
-- 字段 is_mock：是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。
-- 字段 model_version：模型版本；产生当前模型输出的版本标识。
-- 字段 updated_at：更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。
CREATE TABLE IF NOT EXISTS `pias_attribution_cache` (`user_id` TEXT NOT NULL, `status` TEXT, `history_days` INTEGER, `payload_json` TEXT NOT NULL, `is_mock` INTEGER NOT NULL, `model_version` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`));
-- 索引 index_pias_attribution_cache_updated_at：用于当前表的业务查询或唯一性约束。
CREATE INDEX IF NOT EXISTS `index_pias_attribution_cache_updated_at` ON `pias_attribution_cache` (`updated_at`);

COMMIT;
PRAGMA foreign_keys = ON;
