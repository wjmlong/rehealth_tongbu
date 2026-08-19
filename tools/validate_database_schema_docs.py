#!/usr/bin/env python3
"""Validate generated database documentation against live schema metadata."""

from __future__ import annotations

import re
from pathlib import Path

from generate_database_schema_docs import (
    ROOT,
    Table,
    mysql_metadata,
    postgres_metadata,
    room_metadata,
    table_anchor,
)


DOCS = ROOT / "backend" / "docs"
APPENDICES = {
    DOCS / "database" / "ROOM_SCHEMA_V19.md": room_metadata,
    DOCS / "database" / "SOFTWARE_DB_TABLES.md": mysql_metadata,
    DOCS / "database" / "HARDWARE_DB_TABLES.md": postgres_metadata,
}


def fail(message: str) -> None:
    raise AssertionError(message)


def detail_sections(markdown: str) -> dict[str, str]:
    matches = list(re.finditer(r"^## \d+\. 表：`([^`]+)`.*$", markdown, re.MULTILINE))
    result: dict[str, str] = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(markdown)
        table_name = match.group(1)
        if table_name in result:
            fail(f"duplicate table section: {table_name}")
        result[table_name] = markdown[match.start():end]
    return result


def block(section: str, heading: str, next_heading: str) -> str:
    start_marker = f"### {heading}"
    end_marker = f"### {next_heading}"
    start = section.find(start_marker)
    end = section.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        fail(f"missing block {heading!r} or {next_heading!r}")
    return section[start:end]


def validate_table_section(table: Table, section: str, source: Path) -> None:
    fields = block(section, "字段", "索引")
    documented_columns = re.findall(r"^\| \d+ \| `([^`]+)` \|", fields, re.MULTILINE)
    expected_columns = [column.name for column in table.columns]
    if documented_columns != expected_columns:
        fail(
            f"{source.name}:{table.name} column mismatch: "
            f"expected {expected_columns}, got {documented_columns}"
        )

    indexes = block(section, "索引", "关联关系")
    documented_indexes = re.findall(r"^\| `([^`]+)` \|", indexes, re.MULTILINE)
    expected_indexes = [index.name for index in table.indexes]
    if documented_indexes != expected_indexes:
        fail(
            f"{source.name}:{table.name} index mismatch: "
            f"expected {expected_indexes}, got {documented_indexes}"
        )

    relations = block(section, "关联关系", "枚举与约束")
    for relation in table.relations:
        target_columns = ", ".join(relation.target_columns)
        rendered_target = f"`{relation.target_table}.({target_columns})`"
        if rendered_target not in relations:
            fail(
                f"{source.name}:{table.name} missing relation target {rendered_target}"
            )


def validate_appendix(path: Path, tables: list[Table]) -> tuple[int, int, int, int]:
    if not path.is_file():
        fail(f"missing appendix: {path}")
    markdown = path.read_text(encoding="utf-8")
    sections = detail_sections(markdown)
    expected_names = [table.name for table in tables]
    inventory_end = markdown.find("## 模块统计")
    inventory = markdown[:inventory_end]
    inventory_rows = re.findall(
        r"^\| \d+ \| \[`([^`]+)`\]\(#([^)]+)\) \|",
        inventory,
        re.MULTILINE,
    )
    if [name for name, _anchor in inventory_rows] != expected_names:
        fail(f"{path.name} inventory does not match live table order")
    for name, anchor in inventory_rows:
        if anchor != table_anchor(name):
            fail(f"{path.name}:{name} has incorrect anchor #{anchor}")
    if list(sections) != expected_names:
        missing = sorted(set(expected_names) - set(sections))
        extra = sorted(set(sections) - set(expected_names))
        fail(f"{path.name} table mismatch: missing={missing}, extra={extra}")

    for table in tables:
        validate_table_section(table, sections[table.name], path)

    return (
        len(tables),
        sum(len(table.columns) for table in tables),
        sum(len(table.indexes) for table in tables),
        sum(len(table.relations) for table in tables),
    )


def validate_master(total_tables: int) -> None:
    path = DOCS / "REHEALTH_DB_SCHEMA.md"
    markdown = path.read_text(encoding="utf-8")
    if f"总计 **{total_tables} 张基础表**" not in markdown:
        fail(f"master document does not state total table count {total_tables}")
    for relative in (
        "database/ROOM_SCHEMA_V19.md",
        "database/SOFTWARE_DB_TABLES.md",
        "database/HARDWARE_DB_TABLES.md",
    ):
        if f"]({relative})" not in markdown:
            fail(f"master document missing link: {relative}")
        if not (DOCS / relative).is_file():
            fail(f"master document has broken local link: {relative}")


def validate_no_secrets(paths: list[Path]) -> None:
    forbidden = {
        "credential-bearing database URL": re.compile(
            r"(?:mysql|postgres(?:ql)?)://[^\s/:]+:[^\s@]+@",
            re.IGNORECASE,
        ),
        "runtime password assignment": re.compile(r"\b(?:MYSQL_PWD|PGPASSWORD)\s*="),
        "private key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    }
    for path in paths:
        text = path.read_text(encoding="utf-8")
        for label, pattern in forbidden.items():
            if pattern.search(text):
                fail(f"{path} contains forbidden {label}")


def main() -> None:
    totals = [0, 0, 0, 0]
    generated_paths: list[Path] = [DOCS / "REHEALTH_DB_SCHEMA.md"]
    for path, loader in APPENDICES.items():
        tables, _identity = loader()
        counts = validate_appendix(path, tables)
        totals = [left + right for left, right in zip(totals, counts)]
        generated_paths.append(path)

    validate_master(totals[0])
    validate_no_secrets(generated_paths)
    print(
        "database documentation validated: "
        f"tables={totals[0]}, columns={totals[1]}, indexes={totals[2]}, "
        f"relations={totals[3]}, sensitive-values=absent"
    )


if __name__ == "__main__":
    main()
