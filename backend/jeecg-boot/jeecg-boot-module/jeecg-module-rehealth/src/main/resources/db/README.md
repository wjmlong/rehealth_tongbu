# ReHealth 数据库脚本目录

本目录是 `jeecg-module-rehealth` 所属数据库结构和模块测试数据的统一入口。

## 目录约定

| 路径 | 内容 | 是否自动执行 |
| --- | --- | --- |
| `software/mysql/V*.sql` | `software_db` 的正式、按版本递增的 Flyway 迁移，是表结构的唯一事实来源 | 是 |
| `hardware/mysql/V*.sql` | 硬件遥测库的建表迁移；当前文件名保留历史兼容，实际部署以 Device Service 的 TimescaleDB 迁移为准 | 按部署配置 |
| `testdata/software/mysql/*.sql` | 仅限本地验收的可重复测试数据 | 否 |

测试数据必须放在 `db/testdata` 下，不能放入 `db/software/mysql` 的 Flyway
扫描目录，避免应用启动时向数据库自动写入测试记录。测试数据只能通过带环境检查、
冲突保护和结果校验的执行器运行。

## software_db 表结构索引

以下分组用于快速定位表结构。已有版本迁移不得改名、移动或合并，否则会破坏
Flyway 校验和及既有环境的升级链。

### ReHealth 基础业务

- `V1__create_rehealth_software_tables.sql`：设备绑定、健康档案、访谈、CVD 特征与风险、干预计划、反馈、模型请求审计和迁移记录。
- `V20260723_2__upgrade_legacy_software_schema.sql`：历史结构兼容升级。
- `V20260723_3__add_telemetry_kafka_projection.sql`：遥测事件投影和质量工单。
- `V20260724_1__harden_model_request_audit.sql`：模型请求审计加固。
- `V20260729_1__normalize_business_records.sql`：官网业务记录规范化。
- `V20260730_1__add_health_agent_conversations.sql`：健康问答会话与消息。
- `V20260731_1__add_behavior_records.sql`：结构化行为记录。
- `V20260731_2__add_factor16_contributions.sql`：Factor16 贡献结果。
- `V20260805_1__add_rhi_manual_health_input.sql`：RHI 手工健康输入。
- `V20260812_1__add_website_records.sql`：官网业务记录扩展。
- `V20260814_3__create_rhi_daily_snapshot.sql`：RHI 每日聚合快照。
- `V20260814_4__create_rdi_daily_snapshot.sql`：RDI 每日聚合快照与结构化贡献。

### 保险业务与机构计划

- `V20260811_1__seed_insurance_risk_permission.sql`：保险风险查看权限。
- `V20260812_2__create_insurance_business_schema.sql`：保险主体、保单、保障、授权、干预、理赔、研究、报告、结算与审计核心表。
- `V20260812_3__seed_insurer_roles.sql`：保险角色模板。
- `V20260813_1__extend_insurance_workflow.sql`：导入批次、研究任务、计划绑定与 APP 反馈。
- `V20260813_2__seed_insurer_workflow_permissions.sql`：保险工作流权限。
- `V20260813_3__grant_insurance_workflow_to_admin.sql`：本地管理员验收授权。
- `V20260813_4__add_insurance_subject_manager_scope.sql`：保险对象负责人范围。
- `V20260813_5__rename_insurer_roles_cn.sql`：保险角色中文名称。
- `V20260813_6__create_insurance_settings.sql`：保险机构设置。
- `V20260813_7__grant_insurance_settings_to_admin.sql`：本地机构设置验收授权。
- `V20260813_8__isolate_department_codes_by_tenant.sql`：租户内部门编码隔离。
- `V20260814_1__grant_insurance_settings_view.sql`：保险机构只读权限。
- `V20260814_2__create_insurance_intervention_actions.sql`：人工干预行动。
- `V20260817_1__add_insurance_adherence_events.sql`：保险计划执行事件与依从性统计基础。
- `V20260819_1__create_versioned_care_plans.sql`：整合后的机构计划主表、不可变版本、版本项目、任务实例和审计表；5 个表和 71 个字段均带中文注释。

## 本地测试数据

- `testdata/software/mysql/seed-versioned-care-plan-test-data.sql`：复用
  `LOCAL_MULTI_INSURER_APP_QA` 的 36 个保险服务对象，插入 36 个计划、36 个发布版本、
  108 个计划项目、108 个任务实例和 72 个审计事件。

执行入口：

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-versioned-care-plan-test-data.ps1 `
  -AnchorDate 2026-08-19
```

展示字段使用正常业务名称，不带“测试”“合成”或 `QA` 字样；测试归属只保留在
确定性 ID 和 `LOCAL_VERSIONED_CARE_PLAN_QA` 内部标识中。
