#!/usr/bin/env python3
"""生成 ReHealth 多数据库统一 SQL 交付资产。

结构快照只读取本地开发数据库 catalog、现有迁移/测试脚本与 Room 导出
schema；不会读取业务数据，也不会写入或输出数据库密码。
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import re
import subprocess
import sys
from datetime import date
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MYSQL_CONTAINER = "rehealth-software-db-1"
POSTGRES_CONTAINER = "rehealth-hardware-db-1"
MYSQL_MIGRATIONS = ROOT / "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql"
ROOM_SCHEMA = ROOT / "Android-apk/app/schemas/com.rehealth.genie.data.AppDatabase/19.json"
TIMESCALE_MIGRATIONS = ROOT / "backend/device-service/src/main/resources/db/migration/timescale"


def run(command: list[str], *, stdin: str | None = None) -> str:
    completed = subprocess.run(
        command,
        input=stdin.encode("utf-8") if stdin is not None else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if completed.returncode:
        error = completed.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"命令执行失败（{completed.returncode}）：{' '.join(command)}\n{error}")
    return completed.stdout.decode("utf-8")


def load_descriptions() -> tuple[dict[str, tuple[str, str]], dict[str, tuple[str, str]], dict]:
    source = ROOT / "tools/generate_database_schema_docs.py"
    spec = importlib.util.spec_from_file_location("database_schema_docs", source)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"无法加载字段说明来源：{source}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module.TABLE_INFO, module.COMMON_COLUMN_INFO, module.LOGICAL_RELATIONS


TABLE_INFO, COLUMN_INFO, LOGICAL_RELATIONS = load_descriptions()


def mysql(sql: str) -> str:
    return run([
        "docker", "exec", "-i", MYSQL_CONTAINER, "sh", "-lc",
        'export MYSQL_PWD="$(cat \"$MYSQL_PASSWORD_FILE\")"; '
        'exec mysql --default-character-set=utf8mb4 -N -B --raw '
        '-u"$MYSQL_USER" "$MYSQL_DATABASE"',
    ], stdin=sql)


def postgres(sql: str) -> str:
    return run([
        "docker", "exec", "-i", POSTGRES_CONTAINER, "sh", "-lc",
        'exec psql -X -A -t -F "|" -U "$POSTGRES_USER" -d "$POSTGRES_DB"',
    ], stdin=sql)


def mysql_dump_schema() -> str:
    return run([
        "docker", "exec", "-i", MYSQL_CONTAINER, "sh", "-lc",
        'export MYSQL_PWD="$(cat \"$MYSQL_PASSWORD_FILE\")"; '
        'exec mysqldump --default-character-set=utf8mb4 --no-data --skip-comments '
        '--skip-add-drop-table --compact --single-transaction '
        '-u"$MYSQL_USER" "$MYSQL_DATABASE"',
    ])


def has_chinese(value: str) -> bool:
    return bool(re.search(r"[\u4e00-\u9fff]", value))


def sql_comment(value: str, limit: int = 900) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")[:limit]


def table_description(table: str, current: str = "") -> tuple[str, str]:
    if table in TABLE_INFO:
        return TABLE_INFO[table]
    if current and has_chinese(current):
        return current, current
    return f"{table} 表", "TODO：表的中文业务用途待对应模块负责人确认。"


def column_description(column: str, current: str = "") -> str:
    if current and has_chinese(current):
        return current
    if column in COLUMN_INFO:
        name, purpose = COLUMN_INFO[column]
        prefix = f"原注释：{current}；" if current else ""
        return f"{prefix}{name}；{purpose}"
    prefix = f"原注释：{current}；" if current else ""
    return f"{prefix}TODO：字段中文业务含义待确认"


def mysql_catalog() -> tuple[dict[str, str], dict[tuple[str, str], str], int, int]:
    rows = mysql("""
SELECT TABLE_NAME, REPLACE(REPLACE(COALESCE(TABLE_COMMENT, ''), CHAR(10), ' '), CHAR(13), ' ')
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
""").splitlines()
    tables = {row.split("\t", 1)[0]: row.split("\t", 1)[1] if "\t" in row else "" for row in rows}
    column_rows = mysql("""
SELECT TABLE_NAME, COLUMN_NAME,
       REPLACE(REPLACE(COALESCE(COLUMN_COMMENT, ''), CHAR(10), ' '), CHAR(13), ' ')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, ORDINAL_POSITION;
""").splitlines()
    columns: dict[tuple[str, str], str] = {}
    for row in column_rows:
        parts = row.split("\t", 2)
        if len(parts) < 2:
            raise RuntimeError(f"无法解析 MySQL 字段元数据：{row!r}")
        columns[(parts[0], parts[1])] = parts[2] if len(parts) > 2 else ""
    missing_tables = sum(not has_chinese(comment) for comment in tables.values())
    missing_columns = sum(not has_chinese(comment) for comment in columns.values())
    return tables, columns, missing_tables, missing_columns


def relation_lines(table: str) -> list[str]:
    lines = []
    for (source_table, source_column), (target_table, target_column, note) in LOGICAL_RELATIONS.items():
        if source_table == table:
            lines.append(f"-- 逻辑关联：{source_column} -> {target_table}.{target_column}（{note}）")
    return lines


def add_mysql_comments(create_sql: str, table: str, table_comment: str, columns: dict[tuple[str, str], str]) -> str:
    create_sql = create_sql.replace(f"CREATE TABLE `{table}`", f"CREATE TABLE IF NOT EXISTS `{table}`", 1)
    output: list[str] = []
    for line in create_sql.splitlines():
        match = re.match(r"^(\s*)`([^`]+)`\s", line)
        if match:
            column = match.group(2)
            current = columns.get((table, column), "")
            wanted = sql_comment(column_description(column, current))
            comment_match = re.search(r"\sCOMMENT\s+'((?:''|[^'])*)'", line, flags=re.I)
            if comment_match:
                if not has_chinese(comment_match.group(1)):
                    line = line[:comment_match.start()] + f" COMMENT '{wanted}'" + line[comment_match.end():]
            else:
                comma = "," if line.rstrip().endswith(",") else ""
                body = line.rstrip()[:-1] if comma else line.rstrip()
                line = f"{body} COMMENT '{wanted}'{comma}"
        output.append(line)
    result = "\n".join(output)
    chinese_name, purpose = table_description(table, table_comment)
    wanted_table = sql_comment(f"{chinese_name}；{purpose}")
    existing = re.search(r"\sCOMMENT='((?:''|[^'])*)'", result, flags=re.I)
    if existing:
        if not has_chinese(existing.group(1)):
            result = result[:existing.start()] + f" COMMENT='{wanted_table}'" + result[existing.end():]
    else:
        result = result[:-1] + f" COMMENT='{wanted_table}';" if result.endswith(";") else result + f" COMMENT='{wanted_table}'"
    return result


def generate_mysql_schema(output: Path) -> tuple[int, int, int]:
    tables, columns, missing_tables, missing_columns = mysql_catalog()
    dump = mysql_dump_schema()
    creates = re.findall(r"CREATE TABLE `([^`]+)` \(.*?\n\)[^;]*;", dump, flags=re.S)
    statements = re.findall(r"CREATE TABLE `[^`]+` \(.*?\n\)[^;]*;", dump, flags=re.S)
    if set(creates) != set(tables):
        missing = sorted(set(tables) - set(creates))
        raise RuntimeError(f"mysqldump 未覆盖全部表：{missing}")
    by_name = {name: statement for name, statement in zip(creates, statements)}
    blocks = [
        "-- ReHealth software_db 当前结构快照（由本地 MySQL catalog 生成）",
        "-- 目标版本：MySQL 8.4；字符集 utf8mb4；时区 UTC",
        "-- 索引与物理外键保留在各 CREATE TABLE 中，避免重复创建。",
        "SET NAMES utf8mb4;",
        "SET time_zone = '+00:00';",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]
    for table in sorted(tables):
        chinese_name, purpose = table_description(table, tables[table])
        blocks.extend([
            "-- ============================================================================",
            f"-- 表：{table}",
            f"-- 中文名称：{chinese_name}",
            f"-- 业务用途：{purpose}",
            *relation_lines(table),
            "-- ============================================================================",
            add_mysql_comments(by_name[table], table, tables[table], columns),
            "",
        ])
    blocks.append("SET FOREIGN_KEY_CHECKS = 1;")
    (output / "mysql/01_schema.sql").write_text("\n".join(blocks) + "\n", encoding="utf-8", newline="\n")
    return len(tables), missing_tables, missing_columns


def strip_leading_comments(statement: str) -> str:
    return re.sub(r"^(?:\s*--[^\n]*(?:\n|$)|\s*/\*.*?\*/\s*)+", "", statement, flags=re.S)


def split_sql(text: str) -> list[str]:
    statements: list[str] = []
    start = 0
    quote: str | None = None
    escaped = False
    line_comment = False
    block_comment = False
    i = 0
    while i < len(text):
        char = text[i]
        pair = text[i:i + 2]
        if line_comment:
            if char == "\n":
                line_comment = False
        elif block_comment:
            if pair == "*/":
                block_comment = False
                i += 1
        elif quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                if i + 1 < len(text) and text[i + 1] == quote:
                    i += 1
                else:
                    quote = None
        elif pair == "--" and (i + 2 == len(text) or text[i + 2].isspace()):
            line_comment = True
            i += 1
        elif pair == "/*":
            block_comment = True
            i += 1
        elif char in ("'", '"', "`"):
            quote = char
        elif char == ";":
            statement = text[start:i + 1].strip()
            if statement:
                statements.append(statement)
            start = i + 1
        i += 1
    tail = text[start:].strip()
    if tail:
        statements.append(tail)
    return statements


def generate_mysql_init(output: Path) -> list[str]:
    relative_sources = [
        "backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/src/main/resources/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260811_1__seed_insurance_risk_permission.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260812_3__seed_insurer_roles.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_2__seed_insurer_workflow_permissions.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_5__rename_insurer_roles_cn.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_6__create_insurance_settings.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_1__grant_insurance_settings_view.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_2__create_insurance_intervention_actions.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260819_1__create_versioned_care_plans.sql",
    ]
    blocks = [
        "-- ReHealth 生产可用的基础权限、角色与菜单初始化数据",
        "-- 前置条件：已执行 JeecgBoot 官方基础库脚本和 mysql/01_schema.sql。",
        "-- 本文件不创建管理员用户、不写业务测试样本，且不伪造 flyway/迁移历史。",
        "SET NAMES utf8mb4;",
        "SET time_zone = '+00:00';",
        "",
    ]
    for relative in relative_sources:
        source = ROOT / relative
        selected = []
        for statement in split_sql(source.read_text(encoding="utf-8-sig")):
            clean = strip_leading_comments(statement).lstrip()
            if re.match(r"^(INSERT|UPDATE)\b", clean, flags=re.I) and "rehealth_schema_migration" not in clean.lower():
                selected.append(clean)
        if selected:
            blocks.extend([f"-- 来源：{relative}", *selected, ""])
    (output / "mysql/03_init_data.sql").write_text("\n\n".join(blocks).rstrip() + "\n", encoding="utf-8", newline="\n")
    return relative_sources


def concatenate_sources(target: Path, title: str, sources: list[str], prelude: list[str]) -> None:
    blocks = [title, *prelude]
    for relative in sources:
        content = (ROOT / relative).read_text(encoding="utf-8-sig").rstrip()
        blocks.extend(["", f"-- ============================================================================", f"-- 原始来源：{relative}", "-- ============================================================================", content])
    target.write_text("\n".join(blocks) + "\n", encoding="utf-8", newline="\n")


def generate_test_data(output: Path) -> tuple[list[str], list[str]]:
    mysql_sources = [
        "backend/deploy/rehealth/scripts/seed-multi-insurer-tenant-test-data.sql",
        "backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-test-data.sql",
        "backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/testdata/software/mysql/seed-versioned-care-plan-test-data.sql",
        "backend/deploy/rehealth/scripts/seed-insurance-workflow-test-data.sql",
        "backend/deploy/rehealth/scripts/seed-medical-workspace-test-data.sql",
    ]
    timescale_sources = [
        "backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-hardware-test-data.sql",
        "backend/deploy/rehealth/scripts/seed-medical-workspace-hardware-test-data.sql",
    ]
    concatenate_sources(
        output / "mysql/04_test_data.sql",
        "-- 警告：仅限本地/测试环境。严禁在生产数据库执行。",
        mysql_sources,
        [
            "-- 统一夹具把保险工作流挂到多保险机构脚本创建的 9101 租户，保证可在空库连续执行。",
            "SET NAMES utf8mb4;",
            "SET time_zone = '+00:00';",
            "SET @seed_tenant_id = 9101;",
            "SET @seed_actor = 'local_ins_9101_admin';",
            "SET @anchor_date = DATE('2026-08-19');",
            "SET @seed_time = TIMESTAMP('2026-08-19 09:00:00');",
        ],
    )
    mysql_test_path = output / "mysql/04_test_data.sql"
    mysql_test_path.write_text(
        mysql_test_path.read_text(encoding="utf-8")
        + """

-- 当前版本计划任务执行事实：从已生成的任务实例构造幂等反馈，覆盖移动端展示链路。
INSERT INTO rehealth_care_plan_execution (
    id, tenant_id, occurrence_id, plan_id, revision_id, plan_item_id,
    logical_item_id, subject_ref, feedback_type, score_value,
    verification_type, note, occurred_at, source_system,
    source_record_id, created_at
)
SELECT
    LOWER(MD5(CONCAT('LOCAL_CARE_PLAN_EXECUTION_QA:', occurrence.id))),
    occurrence.tenant_id, occurrence.id, occurrence.plan_id,
    occurrence.revision_id, occurrence.plan_item_id, occurrence.logical_item_id,
    occurrence.subject_ref,
    CASE MOD(CRC32(occurrence.id), 3)
        WHEN 0 THEN 'completed'
        WHEN 1 THEN 'partially_completed'
        ELSE 'skipped'
    END,
    CASE MOD(CRC32(occurrence.id), 3)
        WHEN 0 THEN 1.0000
        WHEN 1 THEN 0.5000
        ELSE 0.0000
    END,
    'self_report', '按计划完成情况提交', occurrence.scheduled_at,
    'LOCAL_CARE_PLAN_EXECUTION_QA',
    CONCAT('LOCAL_CARE_PLAN_EXECUTION_QA:', occurrence.id), @seed_time
FROM rehealth_care_plan_occurrence occurrence
WHERE occurrence.status = 'scheduled'
ON DUPLICATE KEY UPDATE
    feedback_type = VALUES(feedback_type),
    score_value = VALUES(score_value),
    note = VALUES(note),
    occurred_at = VALUES(occurred_at);
""",
        encoding="utf-8", newline="\n",
    )
    concatenate_sources(
        output / "timescaledb/04_test_data.sql",
        "-- 警告：仅限本地/测试环境。严禁在生产数据库执行。",
        timescale_sources,
        ["-- 使用 psql -v anchor_date=2026-08-19 执行；原脚本通过 :anchor_date 锚定日期。", "SET TIME ZONE 'UTC';"],
    )
    return mysql_sources, timescale_sources


def generate_room_test_data(output: Path) -> None:
    content = """-- 警告：仅限 SQLite/Room 空库或本地测试环境。严禁覆盖真实用户数据库。
PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

INSERT OR REPLACE INTO health_records(id, type, value, unit, recordedAt, source)
VALUES ('local-health-001', 'resting_heart_rate', '68', 'bpm', 1787101200000, 'LOCAL_SQL_QA');

INSERT OR REPLACE INTO ring_measurements(
    id, metric_type, measured_at, primary_value, secondary_value,
    unit, quality, source, raw_payload, owner_user_id, device_id
) VALUES (
    'local-ring-measurement-001', 'heart_rate', 1787101200000, 68.0, NULL,
    'bpm', 100, 'LOCAL_SQL_QA', NULL, 'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO ring_sleep_sessions(
    id, started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source, raw_payload,
    total_sleep_minutes, owner_user_id, device_id
) VALUES (
    'local-ring-sleep-001', 1787058000000, 1787086800000, 105, 245, 25,
    105, 20, 'LOCAL_SQL_QA', NULL, 455, 'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO ring_activities(
    id, started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source,
    raw_payload, owner_user_id, device_id
) VALUES (
    'local-ring-activity-001', 1787104800000, 1787108400000, 'walking',
    6200, 4520.0, 285.0, 60, 102.0, 'LOCAL_SQL_QA', NULL,
    'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO cvd_risk_history(
    user_id, evaluated_on, risk_score, risk_level, evaluated_at
) VALUES ('local-user-001', '2026-08-19', 0.240000, 'low', 1787108400000);

INSERT OR REPLACE INTO intervention_feedback_queue(
    id, owner_user_id, intervention_id, binding_id, tenant_id,
    plan_item_id, occurrence_id, status, note, expected_count,
    completed_count, verification_type, checked_at, created_at,
    upload_status, upload_attempts, last_error, next_retry_at
) VALUES (
    'local-feedback-001', 'local-user-001', 'local-plan-001',
    'local-binding-001', 9101, 'local-plan-item-001',
    'local-occurrence-001', 'completed', '按计划完成', 1.0, 1.0,
    'self_report', 1787108400000, 1787108400000, 'pending', 0, NULL,
    1787108400000
);

COMMIT;
"""
    (output / "sqlite/04_test_data.sql").write_text(content, encoding="utf-8", newline="\n")


def pg_column_comments() -> str:
    rows = postgres("""
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name LIKE 'hardware_%'
ORDER BY table_name, ordinal_position;
""").splitlines()
    tables = sorted({row.split("|", 1)[0] for row in rows})
    index_rows = postgres("SELECT indexname FROM pg_indexes WHERE schemaname='public' AND tablename LIKE 'hardware_%' ORDER BY indexname;").splitlines()
    blocks = ["", "-- 中文数据库注释"]
    for table in tables:
        name, purpose = table_description(table)
        blocks.append(f"COMMENT ON TABLE {table} IS '{sql_comment(name + '；' + purpose)}';")
    for row in rows:
        table, column = row.split("|", 1)
        blocks.append(f"COMMENT ON COLUMN {table}.{column} IS '{sql_comment(column_description(column))}';")
    for index in index_rows:
        blocks.append(f"COMMENT ON INDEX {index} IS '用于 {index} 对应业务查询或生命周期清理的索引；具体列见索引定义。';")
    blocks.append("COMMENT ON PROCEDURE rehealth_apply_ordinary_retention(integer, jsonb) IS '按配置清理普通硬件运营表与已发布 Outbox 历史数据。';")
    return "\n".join(blocks)


def generate_timescale_schema(output: Path) -> int:
    v1 = (TIMESCALE_MIGRATIONS / "V1__verify_timescale_prerequisites.sql").read_text(encoding="utf-8-sig")
    v2 = (TIMESCALE_MIGRATIONS / "V2__create_hardware_schema.sql").read_text(encoding="utf-8-sig")
    v3 = (TIMESCALE_MIGRATIONS / "V3__create_hypertables_and_lifecycle_policies.sql").read_text(encoding="utf-8-sig")
    v4 = (TIMESCALE_MIGRATIONS / "V4__create_diet_behavior_records.sql").read_text(encoding="utf-8-sig")
    v1 = v1.split("DO $$", 1)[0].strip()
    diet_column = "    diet_record_count integer NOT NULL DEFAULT 0 CHECK (diet_record_count >= 0),\n"
    marker = "    quality_summary jsonb NOT NULL DEFAULT '{}'::jsonb,"
    if marker not in v2:
        raise RuntimeError("无法将 diet_record_count 合并到 hardware_upload_batch")
    v2 = v2.replace(marker, diet_column + marker, 1)
    v4 = re.sub(r"^ALTER TABLE hardware_upload_batch.*?;\s*", "", v4, count=1, flags=re.S)
    replacements = {
        "${measurementRetentionDays}": "730",
        "${signalMetadataRetentionDays}": "90",
        "${operationalRetentionDays}": "1095",
        "${publishedOutboxRetentionDays}": "30",
    }
    for key, value in replacements.items():
        v3 = v3.replace(key, value)
        v4 = v4.replace(key, value)
    table_count = int(postgres("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name LIKE 'hardware_%';").strip())
    schema = "\n\n".join([
        "-- ReHealth hardware_db 当前结构快照\n-- 目标版本：PostgreSQL 17 + TimescaleDB 2.21；时区 UTC\n-- 默认保留期来自 device-service/application.yml：测量 730 天、信号元数据 90 天、运营数据 1095 天、已发布 Outbox 30 天。\nSET TIME ZONE 'UTC';",
        v1,
        v2,
        v4,
        v3,
        pg_column_comments(),
    ])
    (output / "timescaledb/01_schema.sql").write_text(schema.rstrip() + "\n", encoding="utf-8", newline="\n")
    return table_count


def generate_room_schema(output: Path) -> tuple[int, int]:
    document = json.loads(ROOM_SCHEMA.read_text(encoding="utf-8"))
    database = document["database"]
    blocks = [
        f"-- Android Room 当前结构快照，数据库版本 {database['version']}，文件名 rehealth-local.db",
        "-- SQLite 不支持 COMMENT ON；每张表和字段的中文注释使用相邻 -- 注释表达。",
        "PRAGMA foreign_keys = OFF;",
        "BEGIN TRANSACTION;",
        "",
    ]
    for entity in database["entities"]:
        table = entity["tableName"]
        name, purpose = table_description(table)
        blocks.extend(["-- ============================================================================", f"-- 表：{table}", f"-- 中文名称：{name}", f"-- 业务用途：{purpose}"])
        for field in entity.get("fields", []):
            column = field["columnName"]
            blocks.append(f"-- 字段 {column}：{column_description(column)}")
        blocks.append(entity["createSql"].replace("${TABLE_NAME}", table) + ";")
        for index in entity.get("indices", []):
            blocks.append(f"-- 索引 {index['name']}：用于当前表的业务查询或唯一性约束。")
            blocks.append(index["createSql"].replace("${TABLE_NAME}", table) + ";")
        blocks.append("")
    blocks.extend(["COMMIT;", "PRAGMA foreign_keys = ON;"])
    (output / "sqlite/01_room_schema.sql").write_text("\n".join(blocks) + "\n", encoding="utf-8", newline="\n")
    return database["version"], len(database["entities"])


def classify_sql(path: Path) -> tuple[str, str]:
    value = path.as_posix().lower()
    if "/target/" in value or "/build/" in value:
        return "构建产物", "不纳入；由源文件生成"
    if "db/migration" in value or "/flyway/" in value:
        return "版本迁移", "保留原位；统一目录只提供当前快照"
    if "testdata" in value or "/scripts/seed-" in value or "/scripts/cleanup-" in value:
        return "测试/清理数据", "种子脚本已按适用范围汇总，清理脚本保留原位"
    if "/db/" in value or value.endswith("init.sql"):
        return "基础结构/厂商脚本", "保留原位并在 README 说明依赖"
    return "业务或工具 SQL", "保留原位；运行时 SQL 不复制为建表脚本"


def generate_inventory(output: Path, init_sources: list[str], mysql_tests: list[str], timescale_tests: list[str]) -> None:
    paths = sorted(path for path in ROOT.rglob("*.sql") if ".git" not in path.parts and "sql" not in path.relative_to(ROOT).parts[:1])
    lines = [
        "# SQL 来源清单",
        "",
        f"> 扫描日期：{date.today().isoformat()}。统一资产不删除、不改写历史 Flyway；业务 Mapper/JDBC SQL 继续由代码维护。",
        "",
        "| 类型 | 原始位置 | 处理方式 |",
        "|---|---|---|",
    ]
    for path in paths:
        relative = path.relative_to(ROOT).as_posix()
        kind, action = classify_sql(path)
        if relative in init_sources:
            action = "生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留"
        elif relative in mysql_tests:
            action = "按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留"
        elif relative in timescale_tests:
            action = "汇总至 `timescaledb/04_test_data.sql`；原脚本保留"
        lines.append(f"| {kind} | `{relative}` | {action} |")
    lines.extend([
        "",
        "## 非 `.sql` SQL 来源",
        "",
        "- Android `AppDatabase.kt`、`RiskHistoryMigrationSql.kt` 与各 `@Entity`：结构以 Room 19.json 导出为准，统一到 `sqlite/01_room_schema.sql`。",
        "- Device Service Java Repository：只包含运行时 DML，结构由 Timescale Flyway 管理，不复制。",
        "- JeecgBoot Java Repository、MyBatis Mapper/XML：只包含运行时查询/DML，结构由软件库迁移和平台基线管理，不复制。",
        "- `seed-admin-rhi-test-data.ps1`、`seed-admin-intervention-test-data.ps1`：包含按运行环境动态解析 ID 的 SQL，保留为运行时测试工具，未静态拼接。",
        "- Nacos、XXL-Job 与 JeecgBoot 官方大脚本属于第三方平台基线，未混入 ReHealth 业务初始化数据。",
    ])
    (output / "SOURCE_INVENTORY.md").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def entity_table_names() -> set[str]:
    result: set[str] = set()
    root = ROOT / "backend/jeecg-boot"
    for path in root.rglob("*.java"):
        if "src/main/" not in path.as_posix():
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        result.update(re.findall(r'@TableName\s*\(\s*["\']([^"\']+)["\']', text))
    return result


def generate_issues(output: Path, mysql_tables: int, missing_tables: int, missing_columns: int, room_version: int, room_tables: int, timescale_tables: int) -> None:
    actual_mysql = set(mysql("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE';").splitlines())
    missing_entities = sorted(entity_table_names() - actual_mysql)
    issues = f"""# 数据库结构审计与待办

## 审计结论

- MySQL 软件库：{mysql_tables} 张当前基础表；快照生成前 catalog 中 {missing_tables} 张表、{missing_columns} 个字段缺少中文数据库注释。统一快照已补齐中文说明或明确 TODO，但未回写线上库、未篡改历史迁移。
- TimescaleDB 硬件库：{timescale_tables} 张 `hardware_*` 表；V1–V4 是权威迁移，统一快照合并了 V4 的 `diet_record_count` ALTER，并补齐表、字段、索引中文注释。
- Android Room：版本 {room_version}、{room_tables} 张实际注册表；统一快照直接来自 19.json，Entity/索引/列名以导出结果为准。
- MyBatis-Plus `@TableName` 映射在当前软件库中未找到的表：{', '.join(f'`{name}`' for name in missing_entities) if missing_entities else '无'}。
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
"""
    (output / "ISSUES.md").write_text(issues, encoding="utf-8", newline="\n")


def generate_readme(output: Path, mysql_tables: int, room_version: int, room_tables: int, timescale_tables: int) -> None:
    readme = f"""# ReHealth 统一 SQL 资产

本目录是多数据库的“当前结构快照 + 基础数据 + 测试数据”交付入口，不替代历史 Flyway。生成依据为 2026-08-19 本地开发 catalog、现有迁移、测试脚本和 Room 导出 schema。

| 数据域 | 数据库/ORM | 当前基线 | 文件 |
|---|---|---|---|
| 软件业务与 Jeecg 平台 | MySQL 8.4 / MyBatis-Plus、JDBC | `rehealth_software`，{mysql_tables} 张表 | `mysql/00_database.sql`、`01_schema.sql`、`03_init_data.sql`、`04_test_data.sql` |
| 硬件遥测 | PostgreSQL 17 + TimescaleDB 2.21 / JDBC、Flyway | `rehealth_hardware`，{timescale_tables} 张业务表 | `timescaledb/00_database.sql`、`01_schema.sql`、`04_test_data.sql` |
| Android 本地 | SQLite / Room | `rehealth-local.db`，版本 {room_version}，{room_tables} 张表 | `sqlite/01_room_schema.sql`、`sqlite/04_test_data.sql` |

索引均已放在对应结构文件的建表语句中，因此不再提供会重复执行的 `02_index.sql`。来源迁移不删除、不移动，完整清单见 `SOURCE_INVENTORY.md`；结构风险和 TODO 见 `ISSUES.md`。

## 推荐执行顺序

### MySQL 本地/测试环境

1. 创建数据库：`mysql -uroot -p < sql/mysql/00_database.sql`
2. 首次完整环境先执行 JeecgBoot 官方平台基线 `backend/jeecg-boot/db/jeecgboot-mysql-5.7.sql`，将目标库明确设置为 `rehealth_software` 并验证 MySQL 8.4 兼容性。
3. 导入当前结构：`mysql -uroot -p rehealth_software < sql/mysql/01_schema.sql`
4. 导入生产可用权限/角色：`mysql -uroot -p rehealth_software < sql/mysql/03_init_data.sql`
5. 仅在测试环境导入样本：`mysql -uroot -p rehealth_software < sql/mysql/04_test_data.sql`

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
- 保险侧展示名称按真实业务样式生成，不附加“（测试）”；医疗专用样本继续保留测试标记，避免被误认成真实患者。
- 清理脚本及需要动态解析环境 ID 的 PowerShell 种子继续保留原位，避免静态合并后失去安全边界。

## 重新生成

本地 MySQL/Timescale 容器运行且 Room 19.json 存在时执行：`python tools/generate_unified_sql_assets.py`。生成器只读数据库结构元数据，不读取业务行、不输出密钥。
"""
    (output / "README.md").write_text(readme, encoding="utf-8", newline="\n")


def write_database_scripts(output: Path) -> None:
    (output / "mysql/00_database.sql").write_text(
        "-- 仅用于本地/测试环境或经授权的首次初始化。\n"
        "CREATE DATABASE IF NOT EXISTS `rehealth_software` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;\n"
        "USE `rehealth_software`;\nSET time_zone = '+00:00';\n",
        encoding="utf-8", newline="\n",
    )
    (output / "timescaledb/00_database.sql").write_text(
        "-- 由 psql 执行；生产环境数据库和权限通常应由运维/IaC 管理。\n"
        "SELECT 'CREATE DATABASE rehealth_hardware WITH ENCODING = ''UTF8'''\n"
        "WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'rehealth_hardware')\\gexec\n"
        "\\connect rehealth_hardware\nSET TIME ZONE 'UTC';\nCREATE EXTENSION IF NOT EXISTS timescaledb;\n",
        encoding="utf-8", newline="\n",
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "sql")
    args = parser.parse_args()
    output = args.output.resolve()
    for child in ("mysql", "timescaledb", "sqlite"):
        (output / child).mkdir(parents=True, exist_ok=True)
    write_database_scripts(output)
    mysql_tables, missing_tables, missing_columns = generate_mysql_schema(output)
    init_sources = generate_mysql_init(output)
    mysql_tests, timescale_tests = generate_test_data(output)
    timescale_tables = generate_timescale_schema(output)
    room_version, room_tables = generate_room_schema(output)
    generate_room_test_data(output)
    generate_inventory(output, init_sources, mysql_tests, timescale_tests)
    generate_issues(output, mysql_tables, missing_tables, missing_columns, room_version, room_tables, timescale_tables)
    generate_readme(output, mysql_tables, room_version, room_tables, timescale_tables)
    print(f"已生成 {output}：MySQL {mysql_tables} 表，TimescaleDB {timescale_tables} 表，Room v{room_version} {room_tables} 表。")


if __name__ == "__main__":
    main()
