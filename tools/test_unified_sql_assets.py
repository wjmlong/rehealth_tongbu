from __future__ import annotations

import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql"


class UnifiedSqlAssetsTest(unittest.TestCase):
    def read(self, relative: str) -> str:
        path = SQL / relative
        self.assertTrue(path.is_file(), f"缺少统一 SQL 资产：{path}")
        return path.read_text(encoding="utf-8")

    def test_required_assets_exist(self) -> None:
        for relative in (
            "README.md",
            "SOURCE_INVENTORY.md",
            "ISSUES.md",
            "mysql/00_database.sql",
            "mysql/01_schema.sql",
            "mysql/03_init_data.sql",
            "mysql/04_test_data.sql",
            "timescaledb/00_database.sql",
            "timescaledb/01_schema.sql",
            "timescaledb/04_test_data.sql",
            "sqlite/01_room_schema.sql",
            "sqlite/04_test_data.sql",
        ):
            self.read(relative)

    def test_mysql_snapshot_is_complete_and_non_destructive(self) -> None:
        schema = self.read("mysql/01_schema.sql")
        self.assertEqual(194, len(re.findall(r"^CREATE TABLE IF NOT EXISTS `", schema, re.M)))
        self.assertNotRegex(schema, r"(?im)^\s*DROP\s+(?:TABLE|DATABASE)\b")
        self.assertIn("COMMENT='", schema)
        self.assertIn("COMMENT '计划任务执行事实主键'", schema)

    def test_init_and_test_data_boundaries(self) -> None:
        init_data = self.read("mysql/03_init_data.sql")
        self.assertNotIn("rehealth_schema_migration", init_data.lower())
        self.assertNotRegex(init_data, r"(?i)INSERT\s+INTO\s+sys_user\b")

        test_data = self.read("mysql/04_test_data.sql")
        self.assertTrue(test_data.startswith("-- 警告：仅限本地/测试环境"))
        self.assertIn("SET @seed_tenant_id = 9101;", test_data)
        self.assertIn("INSERT INTO rehealth_care_plan_execution", test_data)
        medical_marker = "-- 原始来源：backend/deploy/rehealth/scripts/seed-medical-workspace-test-data.sql"
        insurance_section = test_data[:test_data.index(medical_marker)]
        self.assertNotIn("（测试）", insurance_section)

    def test_timescale_snapshot_folds_current_alter_and_placeholders(self) -> None:
        schema = self.read("timescaledb/01_schema.sql")
        self.assertEqual(10, len(re.findall(r"^CREATE TABLE hardware_", schema, re.M)))
        self.assertNotIn("${", schema)
        self.assertNotRegex(schema, r"ALTER TABLE hardware_upload_batch\s+ADD COLUMN diet_record_count")
        self.assertIn("diet_record_count integer NOT NULL", schema)
        self.assertIn("COMMENT ON TABLE hardware_upload_batch IS", schema)

    def test_room_snapshot_matches_export_v19(self) -> None:
        exported = json.loads((
            ROOT / "Android-apk/app/schemas/com.rehealth.genie.data.AppDatabase/19.json"
        ).read_text(encoding="utf-8"))["database"]
        schema = self.read("sqlite/01_room_schema.sql")
        self.assertEqual(19, exported["version"])
        self.assertEqual(len(exported["entities"]), len(re.findall(r"^-- 表：", schema, re.M)))
        for entity in exported["entities"]:
            self.assertIn(f"-- 表：{entity['tableName']}", schema)

    def test_inventory_mentions_every_source_sql(self) -> None:
        inventory = self.read("SOURCE_INVENTORY.md")
        for path in ROOT.rglob("*.sql"):
            relative = path.relative_to(ROOT).as_posix()
            if ".git" in path.parts or relative.startswith("sql/"):
                continue
            self.assertIn(f"`{relative}`", inventory, relative)


if __name__ == "__main__":
    unittest.main()
