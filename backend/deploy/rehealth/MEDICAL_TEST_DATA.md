# 医疗侧本地测试数据

本数据集依据仓库根目录 `docs/测试数据生成提示词.md` 生成，并以当前 MySQL/TimescaleDB 真实 DDL、唯一约束、外键和医疗侧读取链路为准。只允许用于本地开发或隔离测试环境，禁止写入生产库。

## 数据范围

固定标记为 `LOCAL_MEDICAL_TEST_SEED`。数据依赖关系如下：

```text
医疗租户
├─ 机构管理员/医生 ─ 角色 ─ rehealth:admin:patient:view
└─ App 患者 ─ 患者档案
   ├─ 设备绑定 ─ 30 天测量/睡眠/活动
   ├─ CVD16 特征 ─ Mock 风险结果
   ├─ Mock 干预计划 ─ 执行反馈
   └─ 7 天 RHI/RDI ─ RDI 因子贡献
```

| 范围 | 数量 | 说明 |
|---|---:|---|
| 医疗租户 | 2 | 租户 `9261`、`9262`，用于验证隔离 |
| 员工账号 | 4 | 每个租户各 1 个管理员、1 个医生 |
| App 患者 | 24 | 全部为虚构姓名和保留测试联系方式 |
| 患者档案 | 24 | 覆盖年龄、性别、BMI、病史和边界状态 |
| 设备绑定 | 18 | 其余患者用于未绑定设备场景 |
| Mock 风险/计划 | 20/20 | 明确标记 `is_mock=1`，不得用于临床判断 |
| 执行反馈 | 60 | 每个计划 3 条，覆盖待确认和已确认 |
| RHI/RDI/贡献 | 140/140/420 | 20 人 × 7 天；每个 RDI 3 个解释因子 |
| 硬件批次 | 18 | 30 天、每人 1 批次 |
| 测量/睡眠/活动 | 2700/540/540 | 每人每天 5 项测量、1 段睡眠、1 次步行 |

覆盖的边界场景包括：无风险记录、无设备、停用账号、逻辑删除账号，以及同时属于两个租户而被患者查询链路 fail-closed 排除的账号。风险、干预与 RDI 均为合成/Mock 结果；患者详情页按产品安全规则不会把它们伪装成真实模型结论。

当前 `/rehealth/admin/v1/patients` 查询的是“当前租户内、未逻辑删除且仅属于一个有效租户的用户”，而不是按 `app_user` 角色或用户启用状态筛选，因此机构员工和停用患者也可能出现在列表中。这是现有读取规则的客观结果，测试数据没有通过篡改关系规避该行为。

## 登录账号

以下账号仅供本地测试，统一密码为 `123456`：

| 租户 | 管理员 | 医生 |
|---|---|---|
| 滨江（9261） | `local_medical_admin` | `local_medical_doctor` |
| 南山（9262） | `local_medical_admin_nanshan` | `local_medical_doctor_nanshan` |

24 个患者账号没有密码，不能用于后台登录。员工角色复用 Jeecg 权限体系，并授予 `rehealth:admin:patient:view`。

## 生成、重复执行与清理

先确认 `rehealth-software-db-1` 和 `rehealth-hardware-db-1` 已启动，然后从仓库根目录执行：

```powershell
.\backend\deploy\rehealth\scripts\seed-medical-workspace-test-data.ps1
```

固定业务日期便于复现时间窗口：

```powershell
.\backend\deploy\rehealth\scripts\seed-medical-workspace-test-data.ps1 -AnchorDate '2026-08-18'
```

脚本可重复执行：软件侧使用确定性 ID 和 upsert；硬件侧先按确定性批次删除，再依赖外键级联重建。每次运行都会断言完整行数，不满足预期立即失败。

精准清理仅删除带有本数据集标记的业务数据、固定测试账号与测试租户：

```powershell
.\backend\deploy\rehealth\scripts\seed-medical-workspace-test-data.ps1 -Cleanup
```

清理脚本保留产品本身的患者查看权限元数据；若该权限原本不存在，种子脚本会按产品 Flyway 迁移中的固定 ID 和权限码补齐。

## 安全与数据口径

- 姓名、手机号、邮箱、地址均为虚构测试值；邮箱使用 `.invalid` 保留域。
- 模型产物使用 `LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL`，并带 `is_mock=1`、`clinicalUseAllowed=false`。
- 硬件来源固定为 `LOCAL_MEDICAL_TEST_SEED`，会被现有 provenance 规则识别为 synthetic。
- 脚本对固定租户 ID、权限 ID、用户名及联系方式做冲突保护；发现被非测试数据占用时 fail-closed。
- 不创建新表、不改变 schema，不包含真实身份证、真实手机号或真实病历。
