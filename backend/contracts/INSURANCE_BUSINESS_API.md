# 保险业务、PSM、RWE 与结算契约

状态：本地 MVP 实现，2026-08-12。权威业务库为 JeecgBoot `software_db`（MySQL）；官网 FastAPI 是受控 BFF 和 PSM 执行器，不直接连接数据库。

## 1. 边界与租户安全

```text
保险官网浏览器
  -> HttpOnly FastAPI 会话
  -> FastAPI 从服务端会话取得 Jeecg Token 与 tenantId
  -> JeecgBoot Shiro 权限 + 当前租户成员校验
  -> MyBatis-Plus / MyBatis
  -> MySQL software_db
```

- 浏览器提交的 `X-Tenant-Id` 不作为租户依据；FastAPI 只转发服务端会话中的租户。
- JeecgBoot 对每次保险查询、导入、研究、报告和结算操作重新校验当前账号的有效租户成员关系。
- 所有业务表查询必须包含 `tenant_id`；路径 ID 不能绕过租户条件。
- 官网只展示匿名化 `subject_ref`、聚合风险和业务结果，不展示原始健康遥测、手机号或身份证号。
- FastAPI 不持有 MySQL 凭据；文件解析后的类型化批次仍通过 JeecgBoot API 写入。

## 2. 角色与权限

| 角色 | 权限范围 |
| --- | --- |
| `insurer_viewer` | 风险、研究和报告只读 |
| `insurer_analyst` | 风险只读；创建研究、冻结快照、运行和审核 PSM；报告只读 |
| `insurance_operator` | 风险只读；业务数据导入；研究只读；报告与结算操作 |
| `insurer_auditor` | 风险、研究、报告和审计证据只读 |

对应权限码为：

```text
rehealth:insurance:risk:view
rehealth:insurance:business:import
rehealth:insurance:study:view
rehealth:insurance:study:manage
rehealth:insurance:report:view
rehealth:insurance:report:manage
rehealth:insurance:settlement:operate
rehealth:insurance:audit:view
```

迁移仅创建角色模板和权限关系，不自动给业务用户授权。`V20260813_3` 只为本地既有 `admin` 角色补齐保险工作流权限，仍不创建用户或租户成员。

## 3. 业务数据导入

JeecgBoot 基础路径：`/jeecg-boot/rehealth/insurance/v1`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/imports/subjects` | 关联当前租户成员，生成不可逆 `subject_ref` 并记录保险授权状态 |
| `POST` | `/imports/policies` | 按 `insuredSubjectRef` 关联投保人 |
| `POST` | `/imports/claims` | 按 `policyNo`、`subjectRef` 关联保单和理赔事件 |

官网 BFF 另提供 `/api/insurer/workflow/imports/{subjects|policies|claims}` 的 JSON 与 `/file` 文件入口。文件支持 CSV/XLSX，单文件不超过 10 MB、单批 1–2000 行。日期格式为 `yyyy-MM-dd`，日期时间格式为 `yyyy-MM-dd HH:mm:ss`。

每个批次必须提供 `sourceSystem` 和 `idempotencyKey`。服务端保存内容 SHA-256；同租户、同类型、同幂等键且内容一致时返回既有结果，内容不同则拒绝。投保人、保单和理赔记录还分别以外部来源记录 ID、保单号和理赔号建立租户内唯一约束。

## 4. PSM 研究状态机

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST/GET` | `/studies` | 创建或查询租户研究项目 |
| `POST` | `/studies/{studyId}/snapshots` | 固化研究成员、协变量和行哈希；快照创建后不可修改 |
| `GET` | `/study-snapshots/{snapshotId}` | 读取快照及成员 |
| `POST` | `/studies/{studyId}/jobs` | 以 `requestId` 幂等创建持久化任务 |
| `GET` | `/study-jobs/{jobId}` | 查询异步任务状态 |
| `PUT` | `/study-jobs/{jobId}/result` | FastAPI 写回成功或失败结果 |
| `POST` | `/study-results/{resultId}/review` | `approve` 或 `return` |

官网 FastAPI 的 PSM 执行器仅使用冻结快照：对可用协变量做确定性逻辑回归倾向评分，按 logit 标准差的 0.2 caliper 做 1:1 不放回匹配，并用 bootstrap 生成 ATT 置信区间。缺失核心字段的成员会被明确排除；没有足够匹配对时任务标记失败，不回退为演示数据。

```text
DRAFT -> SNAPSHOT_FROZEN -> JOB_QUEUED -> RUNNING
                                  -> SUCCEEDED -> PENDING_REVIEW -> APPROVED/RETURNED
                                  -> FAILED
```

## 5. RWE 报告与结算

- 只有已审核通过的 PSM 结果才能生成 RWE 报告。
- RWE 报告支持 `submit`、`approve`、`return`，并可导出 Word。
- 默认 Word 模板为仓库 `docs/ReHealth_PSM_RWE_Report_Draft_V0.1.docx`；运行环境可用 `REHEALTH_RWE_TEMPLATE_PATH` 指向经过审核的只读模板。
- 报告保留“相关性不等于因果”“不替代临床判断”等安全声明，不生成诊断或治疗结论。
- 只有已审核报告才能生成结算包。结算包版本不可变；`recalculate` 创建新版本并保留旧版本，不能原地覆盖证据。
- 结算支持 `submit`、`approve`、`return`、`recalculate`；审批及操作写入独立审计表。

## 6. App 授权与反馈

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/rehealth/mobile/insurance/plans/bind` | 当前登录用户按租户、有效保单和授权版本绑定保险计划 |
| `GET` | `/rehealth/mobile/insurance/plans/current` | 查询当前有效计划 |
| `POST` | `/rehealth/mobile/insurance/plans/{bindingId}/feedback` | 幂等回传完成率、依从性和有界结果摘要 |

绑定必须同时满足：当前用户存在保险投保人映射、保单有效、租户匹配、授权记录有效。App 不向保险官网发送原始健康测量；保险侧只消费按授权范围生成的聚合风险改善、干预执行和理赔结果。

当前 Android 已提供类型化网络 DTO 和认证客户端调用，但计划绑定 UI、用户撤回授权入口、离线反馈队列和产品级验收仍是下一阶段工作。

## 7. 数据库迁移

- `V20260813_1__extend_insurance_workflow.sql`：导入批次、研究任务、计划绑定、干预反馈，以及研究成员协变量与行哈希。
- `V20260813_2__seed_insurer_workflow_permissions.sql`：细分角色与最小权限。
- `V20260813_3__grant_insurance_workflow_to_admin.sql`：本地管理员验收授权。

迁移均为向前兼容的非破坏性变更，不删除既有保险数据。完整逐表结构见 `backend/docs/REHEALTH_DB_SCHEMA.md`。

## 8. 本地验收基线

本地 `software_db` 已使用显式测试来源 `local_acceptance` 验证 1 个投保人、1 张有效保单和 1 条已支付理赔：重复批次幂等命中，工作台返回真实保单和理赔汇总，风险池返回真实 BMI。历史风险记录为 `is_mock=1` 时风险评分和评估时间保持空值，不伪装为真实结果。

安全基线覆盖：正确登录、错误密码、缺少权限、浏览器伪造租户头，以及直接 Jeecg 跨租户访问返回 `403`。正式环境仍需用真实角色账号和两个独立真实租户复跑相同用例。
