# 数据库结构审计与待办

## 审计结论

- MySQL 软件库：194 张当前基础表；快照生成前 catalog 中 137 张表、916 个字段缺少中文数据库注释。统一快照已补齐中文说明或明确 TODO，但未回写线上库、未篡改历史迁移。
- TimescaleDB 硬件库：10 张 `hardware_*` 表；V1–V4 是权威迁移，统一快照合并了 V4 的 `diet_record_count` ALTER，并补齐表、字段、索引中文注释。
- Android Room：版本 19、23 张实际注册表；统一快照直接来自 19.json，Entity/索引/列名以导出结果为准。
- MyBatis-Plus `@TableName` 映射在当前软件库中未找到的表：无。
- 跨 software_db、hardware_db、Room 的 `tenant_id/user_id/device_id` 关系均为逻辑关联；不同数据库之间不创建物理外键。

## 问题分级

### P0

- 无已确认的阻断级结构问题。统一脚本仍必须先在目标版本的空库和备份副本验证，禁止直接覆盖生产库。

### P1

- ReHealth MySQL 迁移位于模块 `db/software/mysql`，Jeecg 平台 Flyway 默认位置位于 cloud-start 的 `flyway/sql/mysql`；部署时必须明确两组迁移的执行者和顺序，避免只执行平台 Flyway。
- `backend/jeecg-boot/db/jeecgboot-mysql-5.7.sql` 是 MySQL 5.7 来源的 Jeecg 平台基线，并创建 `jeecg-boot`；当前部署是 MySQL 8.4 的 `rehealth_software`。首次部署需由运维显式改库名/选择库并完成兼容性验证。
- 当前 Room 19.json 与 `V20260819_2__create_care_plan_execution_facts.sql` 在本次扫描时仍是工作区未提交来源；在其业务分支提交前，不应将本快照视为正式发布基线。

### P2

- Timescale V1–V4 尚无原生 `COMMENT ON`；统一快照已补齐，但正式迁移链若要求线上 catalog 也具备注释，应新增只追加 COMMENT 的后续 Flyway，不能修改已发布版本。
- MySQL 历史迁移大量字段无注释。统一快照使用中文通用含义补齐；标有“TODO：待确认”的平台/厂商字段仍需模块负责人确认。
- `cvd_risk_cache` 存在 Room Entity/DAO，但未注册到 AppDatabase，不属于 Room v19 实际表；保留为代码清理/接入待办。
- 旧 JeecgBoot Java 中仍有 MySQL `hardware_*` 兼容写入代码；当前权威硬件事实库是 TimescaleDB，需继续避免双写形成两个权威源。
- 医疗工作台专用测试数据保留“测试”提示以防误认真实患者；保险演示数据使用自然业务名称，不再以“（测试）”污染真实展示样式。
- 测试脚本统一固定 UTC 和锚点日期 2026-08-19；跨日期回归时应显式修改锚点，不应使用隐式 `CURRENT_DATE` 导致结果漂移。

## 索引与关系复核

- 当前快照直接保留 catalog/Flyway/Room 导出中的主键、唯一键、普通索引、部分索引和物理外键，不另建重复的 `02_index.sql`。
- 高频作用域查询遵循 `(tenant_id, user_id, 时间)` 或 `(tenant_id, device_id, 时间)`；保险域跨聚合关系多数由服务层校验，已在表头逻辑关联注释中标注。
- 外键只用于同库内明确的父子生命周期；跨服务和历史 Jeecg 表不擅自补物理外键，以免破坏现有删除、迁移与多租户规则。
