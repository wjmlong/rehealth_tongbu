# ReHealth 统一 SQL 资产

本目录是多数据库的“当前结构快照 + 基础数据 + 测试数据”交付入口，不替代历史 Flyway。生成依据为 2026-08-19 本地开发 catalog、现有迁移、测试脚本和 Room 导出 schema。

| 数据域 | 数据库/ORM | 当前基线 | 文件 |
|---|---|---|---|
| 软件业务与 Jeecg 平台 | MySQL 8.4 / MyBatis-Plus、JDBC | `rehealth_software`，194 张表 | `mysql/00_database.sql`、`01_schema.sql`、`03_init_data.sql`、`04_test_data.sql` |
| 软件业务 QA 扩展种子 | MySQL 8.4 | 手工维护，命名空间 `REHEALTH_QA_TD_V1` | `mysql/05_testdata_qa.sql`（合并单文件，见下） |
| 硬件遥测 | PostgreSQL 17 + TimescaleDB 2.21 / JDBC、Flyway | `rehealth_hardware`，10 张业务表 | `timescaledb/00_database.sql`、`01_schema.sql`、`04_test_data.sql` |
| Android 本地 | SQLite / Room | `rehealth-local.db`，版本 19，23 张表 | `sqlite/01_room_schema.sql`、`sqlite/04_test_data.sql` |

索引均已放在对应结构文件的建表语句中，因此不再提供会重复执行的 `02_index.sql`。来源迁移不删除、不移动，完整清单见 `SOURCE_INVENTORY.md`；结构风险和 TODO 见 `ISSUES.md`。

## 推荐执行顺序

### MySQL 本地/测试环境

1. 创建数据库：`mysql -uroot -p < sql/mysql/00_database.sql`
2. 首次完整环境先执行 JeecgBoot 官方平台基线 `backend/jeecg-boot/db/jeecgboot-mysql-5.7.sql`，将目标库明确设置为 `rehealth_software` 并验证 MySQL 8.4 兼容性。
3. 导入当前结构：`mysql -uroot -p rehealth_software < sql/mysql/01_schema.sql`
4. 导入生产可用权限/角色：`mysql -uroot -p rehealth_software < sql/mysql/03_init_data.sql`
5. 仅在测试环境导入样本：`mysql -uroot -p rehealth_software < sql/mysql/04_test_data.sql`
6. 可选，QA 扩展种子 `mysql/05_testdata_qa.sql`（合并单文件，与 04 互不冲突，按文件内四个部分顺序执行）：
   - 第 1 部分：9201 医疗 / 9202 保险演示机构、部门与员工账号
   - 第 2 部分：9202 保险域完整链路（依赖第 1 部分）
   - 第 3 部分：9102 机构 30 名分类 APP 用户（依赖 04 的 9102 机构）
   - 第 4 部分：9102 版本化关怀计划与依从性执行事实（依赖第 3 部分）

已有数据库应继续运行原 Flyway/模块迁移，不应拿 `01_schema.sql` 代替增量迁移。生产库通常由运维预创建，`00_database.sql` 只用于本地或明确授权的初始化。

### TimescaleDB 本地/测试环境

1. 使用具有建库权限的账号执行：`psql -U postgres -d postgres -f sql/timescaledb/00_database.sql`
2. 导入结构：`psql -U rehealth -d rehealth_hardware -f sql/timescaledb/01_schema.sql`
3. 仅测试环境导入样本：`psql -U rehealth -d rehealth_hardware -v anchor_date=2026-08-19 -f sql/timescaledb/04_test_data.sql`

结构快照使用 application.yml 默认保留期：测量 730 天、信号元数据 90 天、运营数据 1095 天、已发布 Outbox 30 天。生产环境如覆盖这些值，应继续使用 Flyway placeholder 运行原迁移。

### SQLite/Room

`sqlite/01_room_schema.sql` 用于结构审阅和空库验证；测试样本使用 `sqlite3 rehealth-local-qa.db < sql/sqlite/04_test_data.sql`。Android 正式升级必须继续通过 AppDatabase 显式 Migration，不要用该快照覆盖用户现有数据库。

## 初始化与测试数据边界

- `03_init_data.sql` 仅包含幂等的 ReHealth 权限、菜单、角色和角色权限，不包含管理员账号、患者、保险主体或硬件测量。
- `04_test_data.sql` 明确只供测试，固定 UTC 与锚点日期，尽量沿用原脚本幂等键。
- `05_testdata_qa.sql` 为手工维护的 QA 扩展种子（合并单文件，内含四个部分）：统一使用 `REHEALTH_QA_TD_V1` 命名空间与稳定 MD5/SHA2 派生主键，全部幂等可重复执行；不经过 `generate_unified_sql_assets.py` 重新生成。所有账号密码统一 `123456`（PBEWithMD5AndDES，salt `LQA26081`），手机号/邮箱均为不可路由测试标识。
- 保险侧展示名称按真实业务样式生成，不附加“（测试）”；医疗专用样本继续保留测试标记，避免被误认成真实患者。
- 清理脚本及需要动态解析环境 ID 的 PowerShell 种子继续保留原位，避免静态合并后失去安全边界。

## 重新生成

本地 MySQL/Timescale 容器运行且 Room 19.json 存在时执行：`python tools/generate_unified_sql_assets.py`。生成器只读数据库结构元数据，不读取业务行、不输出密钥。
