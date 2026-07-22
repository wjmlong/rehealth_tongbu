# ReHealth 后端低代码与手写代码边界

状态：架构契约与实施基线（2026-07-23）

## 总体架构

```text
Android / iOS
  -> API Gateway / Jeecg JWT
  -> ReHealth Mobile API
       -> software_db：用户与业务记录
       -> hardware_db：戒指遥测与采集批次
       -> model-service：风险、干预、PIAS 归因
  -> Jeecg Online / JimuReport / Flowable：管理端
```

Android 负责 BLE、Room、离线队列和用户交互；JeecgBoot 负责账户、权限、业务编排与后台管理；Python FastAPI 负责模型推理和归因。禁止把模型推理放进 Android 或 Jeecg Java。

## 适合 Jeecg 低代码

| 领域 | 推荐能力 | 边界 |
| --- | --- | --- |
| 用户档案 | Online 表单、列表、导入导出 | `software_db` 档案表或脱敏视图 |
| 医生工作台 | 仪表盘、查询、待办、随访入口 | 只读聚合视图 + 手写服务命令 |
| 机构管理 | 组织、部门、人员、数据权限 | 复用 Jeecg RBAC |
| 设备管理 | 登记、绑定状态、售后状态 | 不直接写 `hardware_db` |
| 随访表单 | Online 表单 + Flowable | 定义、实例和结果表 |
| 健康任务 | 模板、派发、完成状态 | 规则计算由手写服务完成 |
| 异常工单 | 工单流转、通知、复核 | 原始遥测仅通过 ID 引用 |
| 风险人群 | 筛选、分层、只读列表 | 已持久化风险结果的投影 |
| 运营报表 | JimuReport、图表、大屏 | 脱敏汇总，不读取原始 PPG/RRI |

低代码表统一使用 `rehealth_` 命名空间，并配置角色、字段权限、保留期限和审计策略。

## 必须手写

| 能力 | 位置 |
| --- | --- |
| MRD BLE、SDK 命令、协议解析 | Android `ring/mrd` |
| Room、前台采集、断点续传、上传队列 | Android repository/service/worker |
| JWT 鉴权、移动 API、用户隔离 | Java `mobile` / `service` |
| 遥测校验、幂等、批次事务 | Java `ingest` / `hardware` |
| 风险、干预、归因编排 | Java `service` / `model` / `pias` |
| CatBoost、SHAP、PIAS、LLM | Python `model-service` |
| 临床告警、隐私、同意、审计 | 独立手写领域服务 |

## 强制规则

1. Online 表单不得直接写 `hardware_db`、模型输出或审计日志。
2. 管理端重新评估、生成干预、设备解绑必须调用手写应用服务。
3. `software_db` 与 `hardware_db` 不做跨库事务，以批次 ID、事件和重试衔接。
4. 风险结果必须保留模型版本、Mock/降级标识、时间和数据质量。
5. 低代码生成物只能进入独立 `org.jeecg.modules.rehealth.admin` 包，不得进入 `mobile`、`ingest`、`model` 或 `pias`。

## 当前实现

- Android 已有 Retrofit/OkHttp、JWT 会话、Room 上传队列和真实后端调用。
- `JdbcHardwareTelemetryWriter` 已实现 `hardware_db` 幂等批量写入，需配置真实独立数据源后启用。
- `JdbcSoftwareDbReHealthBusinessRepository` 已实现按 JWT 用户隔离的设备绑定、风险、干预、反馈和归因持久化。
- MySQL 脚本位于 `db/software/mysql/V1__create_rehealth_software_tables.sql`；执行后设置 `rehealth.software-db.enabled=true`。
- Jeecg Online/JimuReport 页面尚未生成；下一步应先完成 MySQL 8 验收和字段级 RBAC，再生成管理页面。
