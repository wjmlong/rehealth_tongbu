# 保险业务、PSM、RWE 与结算契约

状态：本地 MVP 实现，2026-08-14。权威业务库为 JeecgBoot `software_db`（MySQL）；官网 FastAPI 是受控 BFF 和 PSM 执行器，不直接连接数据库。

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
- 同一 Jeecg 账号可以属于多个保险租户；机构设置修改成员部门或角色时，只允许修改当前租户拥有的关系，不得删除或覆盖该账号在其他租户的部门与角色。
- 投保人不要求匿名；只有同时命中当前租户保险角色和有效负责关系的员工才能查看其负责用户的身份与业务数据。`subject_ref` 继续作为租户内技术关联键，原始健康遥测仍不直接提供给保险官网。风险列表和详情返回 `scope_mode=assigned_app_users`，`display_name` 为授权范围内的真实姓名，`product_name` 与 `channel_name` 来自当前有效保单；官网 BFF 不再对姓名做二次脱敏。
- FastAPI 不持有 MySQL 凭据；文件解析后的类型化批次仍通过 JeecgBoot API 写入。

## 2. 角色与权限

| 角色 | 权限范围 |
| --- | --- |
| 保险查看员（`insurer_viewer`） | 风险、研究和报告只读 |
| 保险分析员（`insurer_analyst`） | 风险只读；创建研究、冻结快照、运行和审核 PSM；报告只读 |
| 保险运营员（`insurance_operator`） | 风险只读；业务数据导入；研究只读；报告与结算操作 |
| 保险审计员（`insurer_auditor`） | 风险、研究、报告和审计证据只读 |
| 保险机构管理员（`insurance_org_admin`） | 机构、员工、角色和负责人管理；读取本人负责用户 |
| 保险部门经理（`insurance_department_manager`） | 读取本人负责用户及本部门负责人关系 |

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
rehealth:insurance:intervention:manage
```

迁移仅创建角色模板和权限关系，不自动给业务用户授权。`V20260813_3` 为本地既有 `admin` 角色补齐保险工作流权限，`V20260813_7` 补齐机构设置验收所需权限，仍不创建用户或租户成员；正式环境应通过 `insurance_org_admin` 等最小权限角色授权。

## 3. 业务数据导入

### 2.1 机构设置与负责人范围

JeecgBoot `rehealth:insurance:organization:*`、`member:*`、`role:assign` 和
`assignment:manage` 权限控制机构设置。官网 FastAPI 仅转发当前登录会话，MySQL
只由 JeecgBoot 访问。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET/PUT` | `/rehealth/insurance/v1/settings/organization` | 查询/保存当前租户机构信息 |
| `GET` | `/rehealth/insurance/v1/settings/departments` | 查询当前租户部门及成员数 |
| `GET` | `/rehealth/insurance/v1/settings/members` | 查询成员、角色、部门和负责人数量 |
| `POST` | `/rehealth/insurance/v1/settings/members/invitations` | 按已注册手机号邀请账号加入当前租户，可预选当前租户部门 |
| `PUT` | `/rehealth/insurance/v1/settings/members/{userId}/status` | 启用或停用当前租户成员关系，不停用全局账号 |
| `PUT` | `/rehealth/insurance/v1/settings/members/{userId}/department` | 调整成员在当前租户内的部门，不影响其他租户 |
| `PUT` | `/rehealth/insurance/v1/settings/members/{userId}/role` | 分配白名单内的保险业务角色，不允许通过接口授予机构管理员 |
| `GET` | `/rehealth/insurance/v1/settings/assignments` | 查询投保人与部门经理负责人关系 |
| `PUT` | `/rehealth/insurance/v1/settings/assignments/{subjectRef}` | 由机构管理员维护负责人关系 |

`insurance_org_admin`（保险机构管理员）可维护机构、成员、角色和负责人；
`insurer_viewer`、`insurer_analyst`、`insurance_operator` 和 `insurer_auditor` 可只读查看当前租户的机构信息与员工目录；员工姓名、账号、邮箱和手机号不脱敏，成员、角色及负责人关系的修改权限仍只授予机构管理员等管理角色。

平台级 `admin`、`super_admin` 账号即使因本地验收加入了保险租户，也不会出现在机构成员目录、部门成员数或负责人候选中，且不能通过邀请、成员修改或负责人接口由机构管理员操作。平台管理员仍可使用其平台权限进行验收，但不属于保险机构可管理的业务成员。
`insurance_department_manager`（保险部门经理）只能读取自己负责的投保人、所属部门及对应负责人关系，不能读取同租户其他经理或未分配投保人的信息。风险看板、列表和详情对所有保险后台角色统一应用 `rehealth_insurance_subject_manager`：角色只区分允许执行的后台操作，不裁剪负责用户的可读业务字段。
邀请接口只匹配已经注册的 Jeecg 手机号，写入状态为 `5` 的待接受租户关系；被邀请人同意后才能登录该保险机构工作台，管理员不能通过状态接口跳过成员确认直接启用。当前操作人不能停用自己的租户成员关系，避免机构管理会话自锁。
风险列表、详情和看板从 `rehealth_insurance_subject` 取得当前租户 APP 服务用户，并按当前员工 ID 关联 `rehealth_insurance_subject_manager`；APP 用户不需要加入 `sys_user_tenant`。有保险角色但没有分配时返回空范围，没有当前租户保险角色时拒绝访问。

### 2.1 投保人风险分层

风险分层查询、筛选选项和详情均在 JeecgBoot 内按“当前租户 + 当前员工有效负责关系”限定范围，浏览器不能提交租户或负责人标识。渠道取当前有效保单 `metadata_json.channel`，年龄取 APP 用户档案；渠道、年龄和风险条件在数据库分页及总数查询中同时生效，禁止仅在浏览器当前页过滤。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/rehealth/insurance/v1/dashboard/risk` | 汇总当前负责范围的真实高、中、低风险与未评估人数 |
| `GET` | `/rehealth/insurance/v1/insureds` | 分页查询风险名单；支持 `keyword`、`riskLevel`、`channel`、`minAge`、`maxAge` |
| `GET` | `/rehealth/insurance/v1/insureds/filter-options` | 返回当前负责范围内可用渠道及年龄上下界 |
| `GET` | `/rehealth/insurance/v1/insureds/{subjectId}` | 查询负责范围内单个投保人的档案、保单和风险摘要 |

官网 BFF 的 `GET /api/insurer/insureds/export` 使用相同服务端条件导出 UTF-8 CSV，最多 10000 条，并对电子表格公式前缀做转义。导出不新增数据权限，也不返回租户 ID、原始健康遥测或未列入风险列表白名单的字段。

### 2.2 干预改善工作台

干预改善工作台复用上述负责人范围，不接受浏览器提供租户或用户范围，并只返回 `consent_status=granted` 的服务对象。人工行动继续使用 `rehealth:insurance:risk:view` / `rehealth:insurance:intervention:manage`；机构计划另拆分为 `rehealth:insurance:care-plan:view`、`rehealth:insurance:care-plan:manage` 和 `rehealth:insurance:care-plan:publish`，避免能够编辑草稿的账号自动获得发布或撤回权限。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/rehealth/insurance/v1/interventions/dashboard` | 按当前员工负责范围汇总待行动、进行中、待复核和已改善数量 |
| `GET` | `/rehealth/insurance/v1/interventions` | 分页查询负责用户档案中的年龄、性别、BMI，以及 CVD 风险、主要 Factor16、RHI、RDI、当前干预、依从性、负责人和流程状态；队列首屏无需再读取详情 |
| `GET` | `/rehealth/insurance/v1/interventions/{subjectId}` | 返回 CVD 风险趋势、Factor16、RHI/RDI 日快照、RDI 结构化贡献项、计划、反馈、人工行动和归因证据 |
| `POST` | `/rehealth/insurance/v1/interventions/{subjectId}/actions` | 创建随访、任务或人工复核行动 |
| `POST` | `/rehealth/insurance/v1/interventions/actions/batch` | 为 1–100 名负责范围内用户原子创建同一批激励行动；全部范围校验通过后才写入 |
| `PUT` | `/rehealth/insurance/v1/intervention-actions/{actionId}` | 更新行动状态、负责人、期限和有界结果 |
| `GET` | `/rehealth/insurance/v1/interventions/{subjectId}/care-plans` | 查询负责对象的机构计划、已发布版本和当前草稿 |
| `POST` | `/rehealth/insurance/v1/interventions/{subjectId}/care-plans` | 创建版本 1 草稿；草稿内容允许修改 |
| `GET` | `/rehealth/insurance/v1/care-plans/{planId}` | 查询单个计划及完整版本历史 |
| `PUT` | `/rehealth/insurance/v1/care-plans/{planId}/draft` | 使用 `expected_lock_version` 原子替换草稿内容 |
| `POST` | `/rehealth/insurance/v1/care-plans/{planId}/revisions` | 从最新已发布版本克隆一个新草稿，并保留稳定 `logical_item_id` |
| `POST` | `/rehealth/insurance/v1/care-plans/{planId}/draft/discard` | 放弃草稿但保留版本与审计快照 |
| `POST` | `/rehealth/insurance/v1/care-plans/{planId}/publish` | 冻结并发布草稿；可指定不早于当前时间的 `effective_at` |
| `POST` | `/rehealth/insurance/v1/care-plans/{planId}/withdraw` | 撤回最新版本并排除其未来任务实例 |

机构计划采用 `draft -> published -> withdrawn` 版本状态。已发布版本的标题、说明、项目、时间规则和评分权重不可原地覆盖；修改时必须先克隆新版本。所有写操作使用计划级 `lock_version`，过期的 `expected_lock_version` 返回 `409`。发布新版本会为旧版本写入 `effective_to`，并把该时间之后尚未执行的旧任务实例标记为 `cancelled/superseded_by_revision`，使其不进入后续依从性分母。保险侧只允许生活方式、提醒、教育、监测和跟进类项目，不允许借此修改诊断、用药或治疗。

批量激励要求 `rehealth:insurance:intervention:manage`，使用批次请求 ID 派生逐行动幂等键；任一主体越权、无效或写入失败时整个事务回滚。APP 通用反馈只保留在个人计划链路；保险反馈必须带具体 `bindingId + planItemId`，不会复制到其他机构。工作台只消费聚合后的 CVD 风险、RHI、RDI、计划和反馈，不返回原始遥测；CVD 风险、RHI 与 RDI 是三个独立指标，前端不得用风险分数推导 RDI。RDI Mock、过期或数据不足状态必须显式展示，且不参与现有 PIAS/风险工作流状态计算。只有真实、数据充分且方向一致的归因证据才会自动标记“已改善”；Mock 或证据不足时必须显示说明，不能把合成风险评分当作业务判断。

版本化机构计划 API 与旧 `rehealth_insurance_plan_binding` 并行存在。App 已通过独立聚合接口读取当前生效版本，并在请求时将 `daily`、`weekly`、`once` 规则展开为稳定任务实例；旧绑定仅保留兼容，不会因机构发布动作被自动改写。

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
| `GET` | `/rehealth/mobile/insurance/care-plans/current` | 查询当前登录用户各机构的生效发布版本、今日任务及权威 28 日依从性 |
| `POST` | `/rehealth/mobile/insurance/care-plan-occurrences/{occurrenceId}/feedback` | 按稳定任务实例幂等提交完成、部分完成、跳过或不适用执行事实 |

绑定必须同时满足：当前用户存在保险投保人映射、保单有效、租户匹配、授权记录有效。App 不向保险官网发送原始健康测量；保险侧只消费按授权范围生成的聚合风险改善、干预执行和理赔结果。

计划绑定、当前计划查询和干预反馈还会实时检查 `sys_user_tenant`、用户账号及保险租户均处于启用状态。撤销租户成员关系或停用租户后，即使旧的投保人映射、计划绑定或 App 登录令牌仍存在，也必须拒绝继续访问；读取计划关联的保单和授权记录必须同时包含 `tenant_id`，不能使用裸主键跨租户读取。

版本化计划依从性采用滚动 28 个自然日：窗口内已经到期的有效任务进入分母；当天已反馈任务即使尚未到期也进入统计。项目 `scoring_weight` 同时作用于分子、分母，完成计 1、部分完成计 0.5、跳过计 0，不适用排除。无有效分母时返回空分数，不显示 0%。Android 已接入类型化 DTO、Room v19 离线队列和归因页展示；计划绑定 UI、用户撤回授权入口和真机产品验收仍待完成。

## 7. 数据库迁移

- `V20260813_1__extend_insurance_workflow.sql`：导入批次、研究任务、计划绑定、干预反馈，以及研究成员协变量与行哈希。
- `V20260813_2__seed_insurer_workflow_permissions.sql`：细分角色与最小权限。
- `V20260813_3__grant_insurance_workflow_to_admin.sql`：本地管理员验收授权。
- `V20260813_4__add_insurance_subject_manager_scope.sql`：经理与投保人负责关系表，为后续经理级数据权限提供租户隔离基础。
- `V20260813_6__create_insurance_settings.sql`：机构设置、成员管理权限和保险机构管理角色模板。
- `V20260813_7__grant_insurance_settings_to_admin.sql`：本地 `admin` 验收账号的机构设置读取/维护权限，不用于正式账号授权。
- `V20260814_2__create_insurance_intervention_actions.sql`：保险人工行动表、干预写权限、反馈计划标识扩容及最小角色授权。
- `V20260814_3__create_rhi_daily_snapshot.sql`：认证 APP 用户的 RHI 每日聚合快照；不存原始遥测，并按用户/日期幂等更新。
- `V20260814_4__create_rdi_daily_snapshot.sql`：认证 APP 用户的 RDI 每日聚合快照与结构化贡献项；不存原始遥测或自由文本证据，并按用户/日期幂等更新。
- `V20260819_1__create_versioned_care_plans.sql`：通用机构计划、不可变发布版本、版本化项目、任务实例和审计表；增加保险计划查看、草稿编辑和发布权限。
- `V20260819_2__create_care_plan_execution_facts.sql`：按任务实例保存不可变执行事实、计分值、验证类型和幂等来源；所有表和字段均带注释。

迁移均为向前兼容的非破坏性变更，不删除既有保险数据。完整逐表结构见 `backend/docs/REHEALTH_DB_SCHEMA.md`。

## 8. 本地验收基线

本地 `software_db` 已使用显式测试来源 `local_acceptance` 验证 1 个投保人、1 张有效保单和 1 条已支付理赔：重复批次幂等命中，工作台返回真实保单和理赔汇总，风险池返回真实 BMI。历史风险记录为 `is_mock=1` 时风险评分和评估时间保持空值，不伪装为真实结果。

完整工作流可执行 `backend/deploy/rehealth/scripts/seed-insurance-workflow-test-data.ps1` 写入 `LOCAL_INSURANCE_QA` 验收基线：12 名合成租户成员、12 张有效保单、12 条已支付理赔，以及处理组/对照组各 6 人的 PSM 候选和 1 个草稿研究。脚本使用固定业务键重复更新，不重复插入；合成用户没有密码、手机号或邮箱，不能登录。

多租户成员与权限验收可执行 `backend/deploy/rehealth/scripts/seed-multi-insurer-tenant-test-data.ps1` 写入 `LOCAL_MULTI_INSURER_QA` 基线：租户 `9101`–`9103` 各包含机构节点、健康险运营部、精算与风控部，以及机构管理员、部门经理、分析员、运营员、查看员和待接受邀请账号；共享审计员使用同一个全局 Jeecg 账号加入三个租户。默认执行账号 `admin` 也会加入三个 QA 租户，以便在 Jeecg 租户选择器中切换后检查按当前租户过滤的部门树，但其默认登录租户保持不变。该基线只验证租户成员、部门与角色隔离，不生成投保人、保单、理赔或风险数据，并拒绝覆盖同编号的非 QA 租户。

完整多机构 APP 用户验收可执行 `backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-test-data.ps1 -AnchorDate 2026-08-14`。脚本复用上述机构和员工，创建 14 个全局 APP 账号、36 条保险服务关系和 120 条员工负责关系；每家机构保留原有 6 位服务用户，并增加 6 位已经接受其他保险机构服务的 APP 用户，因此每家机构的工作台均有 12 位负责对象。每个 APP 账号包含完整档案、RHI 手填、访谈、设备、30 天风险、7 条 RHI 日快照、7 条显式 Mock RDI 日快照（每条 3 个结构化贡献项）、4 个 Factor16 解释项及归因/干预数据；每条服务关系包含独立保单、保障、授权、计划绑定、3 条 APP 反馈、3 条人工行动和理赔。四种工作流状态 `pending_action`、`pending_review`、`in_progress`、`improved` 在每家机构内各 3 条；每个活跃测试员工最少负责 4 人。TimescaleDB 另写入按 Android Debug 全链路演练口径生成的 118 天测量、睡眠、活动和饮食记录。APP 账号不加入 `sys_user_tenant`，全部合成账号密码为 `123456`，来源标记为 `LOCAL_MULTI_INSURER_APP_QA`，仅限本地非临床验收。为了覆盖风险分布和“已改善”界面，风险行使用 `is_mock=0`、`scorer_mode=local_qa_fixture`、`artifact_name=LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL`、`[合成]` 姓名和 `clinicalUseAllowed=false` 的组合，且只有明确的 3 人改善组使用非 Mock 归因；该例外绝不能复制到预发布或生产环境。

该基线同时创建 2 个保险部门和 2 个经理账号（`local_insurance_manager_01`、`local_insurance_manager_02`，密码均为 `123456`），并在 `rehealth_insurance_subject_manager` 中按部门分别分配 6 名投保人。风险查询和机构设置的部门、成员、负责人只读接口均在 JeecgBoot 查询层按该映射表过滤，不能仅依赖前端隐藏菜单实现越权防护。

为覆盖“已评估风险”和 PSM 候选筛选，合成风险记录的 `is_mock=0`，但 `model_version=local-qa-seeded-nonclinical-v1`、`scorer_mode=local_qa_fixture`、`artifact_name=LOCAL_INSURANCE_QA_NOT_A_MODEL`，质量警告及响应体也明确禁止临床和业务决策。该脚本只允许在本地开发环境使用。

安全基线覆盖：正确登录、错误密码、缺少权限、浏览器伪造租户头，以及直接 Jeecg 跨租户访问返回 `403`。正式环境仍需用真实角色账号和两个独立真实租户复跑相同用例。
