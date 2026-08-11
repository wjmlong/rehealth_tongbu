# ReHealth 硬件数据库 Schema

日期：2026-07-31
状态：Device Service / TimescaleDB 是当前遥测权威

## 当前 TimescaleDB 权威边界

下文旧版 Jeecg/MySQL E2.1 Schema 仅作为历史回滚背景保留。遥测切换获批后，Device Service 拥有 `hardware_db`，并应用 `device-service/src/main/resources/db/migration/timescale/` 下的 Flyway 迁移。Jeecg 和模型代码通过已认证的 Device Service API 读取硬件事实，不直接查询或关联 TimescaleDB。

TimescaleDB V4 只新增内容：

- 向 `hardware_upload_batch` 增加 `diet_record_count`；
- 创建 `hardware_diet_record` Hypertable，以租户、用户、设备、来源记录和 `consumed_at` 为键；
- 保存餐次类型、有界描述、热量、蛋白质、碳水化合物、脂肪、膳食纤维、钠和来源，不保存姓名、手机号或原始图片；
- 七天后压缩分块，并使用已配置的普通遥测保留期；
- 饮食行与上传收据、其他遥测子记录、对账行和 Outbox 事件在同一事务中写入。

内部只读端点 `GET /rehealth/internal/v1/operations/users/{userId}/intervention-context` 要求提供 `X-ReHealth-Service-Credential`、`tenantId` 和有效的 `timeZone`。每次查询都按租户和用户限定范围。它返回有界的当前本地自然日活动、睡眠、规范化测量和饮食记录，以及描述性的近期/前一 7 日变化；绝不返回原始信号载荷，也不声称趋势代表临床改善。

## 旧版 MySQL E2.1 参考

## 所有权

ReHealth 硬件接入边界独占名为 `hardware` 的独立动态数据源。Jeecg 默认的 `master` 数据源仍是软件数据库。应用代码不得跨库关联两个数据库，也不得开启分布式事务。

迁移文件：
`jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/hardware/mysql/V1__create_hardware_telemetry_tables.sql`

JeecgBoot 排除了 Flyway 自动配置，因此 V1 由部署运行手册显式应用。该迁移只新增内容：六张新表、索引、唯一键和外键；不修改或删除任何现有表。

## 数据表

### `hardware_upload_batch`

为每个认证所有者、设备和 Android 批次保存一份持久化收据。

关键列：`id`、`receipt_id`、`batch_id`、`user_id`、`device_id`、来源和采集时间戳、提交状态、行数，以及不含原始数据的 `quality_json`。

约束：

- 主键 `id`。
- 唯一键 `(user_id, device_id, batch_id)`，用于保证重试幂等。
- `receipt_id` 唯一。
- 用户/设备采集时间索引。

### `hardware_measurement`

规范化的标量遥测，例如心率、SpO₂、血压、体温、步数和 HRV。保存客户端记录 ID、指标类型/时间、主数值、可选次数值、单位、质量码和来源，不保存原始载荷。

### `hardware_sleep_session`

规范化睡眠窗口，包含开始/结束时间以及深睡、浅睡、清醒、REM 和中断分钟数。

### `hardware_activity`

规范化活动窗口，包含类型、步数、距离、热量、时长和可选平均心率。

### `hardware_signal_chunk_metadata`

为未来经批准的原始信号流程预留的纯元数据表。E2.1 不写入该表。表中没有载荷或正文列；`payload_ref` 将指向经批准的外部加密对象存储，并要求设置到期时间戳。

### `hardware_data_quality_event`

为不含载荷的质量/审计事件预留。E2.1 将已接受批次的质量元数据保存在 `hardware_upload_batch.quality_json`；未来消费者可以在此物化运营事件，而不复制原始健康载荷。

## 事务边界

新批次行及其全部测量、睡眠和活动行在一个本地硬件事务中提交。任何子记录插入失败都会回滚整个批次。只有该事务成功返回后，API 才能报告 `persisted=true`。

重复重试读取已提交的收据，不修改子表。并发重试通过同一数据库唯一约束串行化。

## 身份边界

`user_id` 是当前已认证 Jeecg `LoginUser.id`，而不是客户端拥有的标识符。Android JSON 中的 `userId` 字段为兼容传输契约而保留，但在校验和持久化前会被覆盖。数据库只保存内部 ID，不保存手机号或姓名。

## 近期遥测查询

`GET /rehealth/mobile/measurements/recent?limit=50` 从 `hardware_measurement`、`hardware_sleep_session` 和 `hardware_activity` 读取规范化记录。每次查询都按当前认证 Jeecg 用户 ID 过滤，按时间倒序排列，并将每个类别限制为 1–200 行。每条规范化记录都将其已持久化的来源/客户端记录标识符作为稳定公共 `id` 返回；缺失时回退到硬件记录标识符，使 Android 登录恢复可以使用 Room 替换语义。响应不包含原始信号分块或载荷引用。硬件数据源禁用时返回受控的 `503` 响应。

## 保留策略

| 数据 | MVP 策略 | 实现状态 |
| --- | --- | --- |
| 普通遥测 | 热数据保留 30 天 | 策略已配置；清理/汇总任务待完成。 |
| 上传收据 | 180 天 | 运营任务待实现。 |
| 原始信号载荷 | 0 天/禁用 | 拒绝写入；V1 中没有载荷列。 |
| 信号元数据 | 未批准时保留 0 天 | 表已预留，写入器禁用。 |
| 质量事件 | 180 天 | 表已预留；清理任务待完成。 |

经过实际负载测试后，生产迁移可以将高容量数据行迁移到 ClickHouse/TimescaleDB 或分区 MySQL。Android 批次 ID 和后端幂等契约必须保持稳定。

## PIAS 使用边界

该 Schema 保存遥测事实，而不是模型结论。风险结果、干预、反馈、个体归因、群体任务和结算证据应在后续 E1.1/E1.2 工作中归属 `software_db`。model-service 不直接访问数据库，患者客户端不得提供权威风险历史或结算参数。
