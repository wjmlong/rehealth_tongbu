# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
# ─── How to run ───
# D:\rehealthAI\model-service\.venv\Scripts\python.exe backend/contracts/scripts/validate_contracts.py --all --fixtures backend/contracts/fixtures/valid

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Final, NewType, assert_never

JsonScalar = None | bool | int | float | str
JsonValue = JsonScalar | list["JsonValue"] | dict[str, "JsonValue"]
Reason = NewType("Reason", str)
ROOT: Final = Path(__file__).resolve().parents[3]
CONTRACTS: Final = ROOT / "backend" / "contracts"
CONTROLLER: Final = ROOT / "backend" / "jeecg-boot" / "jeecg-boot-module" / "jeecg-module-rehealth" / "src" / "main" / "java" / "org" / "jeecg" / "modules" / "rehealth" / "mobile" / "controller" / "ReHealthMobileController.java"
DTO_ROOT: Final = CONTROLLER.parents[1] / "dto"
FORBIDDEN_KEYS: Final = {
    "$ref": Reason("schema_ref"),
    "token": Reason("token"),
    "access_token": Reason("token"),
    "raw_signal": Reason("raw_signal"),
    "raw_signal_bytes": Reason("raw_signal"),
    "raw_ppg": Reason("raw_signal"),
    "ppg": Reason("raw_signal"),
    "raw_rri": Reason("raw_signal"),
    "rri": Reason("raw_signal"),
    "metrics": Reason("metric_value"),
    "metric_values": Reason("metric_value"),
    "heart_rate": Reason("metric_value"),
    "spo2": Reason("metric_value"),
    "blood_oxygen": Reason("metric_value"),
    "primary_value": Reason("metric_value"),
    "secondary_value": Reason("metric_value"),
    "client_owner": Reason("client_owner"),
    "owner_id": Reason("client_owner"),
    "tenant_id": Reason("client_owner"),
    "user_id": Reason("client_owner"),
}
EVENT_SCHEMAS: Final = {
    "rehealth.telemetry.persisted.v1": "persisted-v1.schema.json",
    "rehealth.telemetry.quality.v1": "quality-v1.schema.json",
    "rehealth.telemetry.dlq.v1": "dlq-v1.schema.json",
}


@dataclass(frozen=True, slots=True)
class Check:
    artifact: str
    accepted: bool
    reasons: tuple[Reason, ...]


def load_json(path: Path) -> JsonValue:
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def policy_reasons(value: JsonValue) -> set[Reason]:
    reasons: set[Reason] = set()
    match value:
        case dict() as mapping:
            for key, child in mapping.items():
                normalized = re.sub(r"(?<!^)(?=[A-Z])", "_", key).lower().replace("-", "_")
                reason = FORBIDDEN_KEYS.get(normalized)
                if reason is not None:
                    reasons.add(reason)
                reasons.update(policy_reasons(child))
        case list() as items:
            for child in items:
                reasons.update(policy_reasons(child))
        case None | bool() | int() | float() | str():
            pass
        case unreachable:
            assert_never(unreachable)
    return reasons


def schema_reasons(value: JsonValue, schema: dict[str, JsonValue]) -> set[Reason]:
    reasons: set[Reason] = set()
    if not isinstance(value, dict):
        return {Reason("schema:type")}
    required = schema.get("required", [])
    properties = schema.get("properties", {})
    if not isinstance(required, list) or not isinstance(properties, dict):
        return {Reason("schema:definition")}
    for field in required:
        if isinstance(field, str) and field not in value:
            reasons.add(Reason(f"schema:missing:{field}"))
    if schema.get("additionalProperties") is False:
        for field in value:
            if field not in properties:
                reasons.add(Reason(f"schema:additional:{field}"))
    for field, field_schema in properties.items():
        if field not in value or not isinstance(field_schema, dict):
            continue
        expected = field_schema.get("type")
        actual = value[field]
        valid_type = {
            "string": isinstance(actual, str),
            "integer": isinstance(actual, int) and not isinstance(actual, bool),
            "number": isinstance(actual, int | float) and not isinstance(actual, bool),
            "boolean": isinstance(actual, bool),
            "object": isinstance(actual, dict),
        }.get(expected, True)
        if not valid_type:
            reasons.add(Reason(f"schema:type:{field}"))
        allowed = field_schema.get("enum")
        if isinstance(allowed, list) and actual not in allowed:
            reasons.add(Reason(f"schema:enum:{field}"))
        pattern = field_schema.get("pattern")
        if isinstance(pattern, str) and isinstance(actual, str) and re.fullmatch(pattern, actual) is None:
            reasons.add(Reason(f"schema:pattern:{field}"))
    return reasons


def controller_routes() -> set[tuple[str, str]]:
    source = CONTROLLER.read_text(encoding="utf-8")
    prefix_match = re.search(r'@RequestMapping\("([^"]+)"\)', source)
    if prefix_match is None:
        return set()
    prefix = prefix_match.group(1)
    return {
        (method.upper(), prefix + path)
        for method, path in re.findall(r'@(Get|Post|Put|Delete)Mapping\("([^"]+)"\)', source)
    }


def dto_fields(dto_name: str) -> set[str]:
    source = (DTO_ROOT / f"{dto_name}.java").read_text(encoding="utf-8")
    fields: set[str] = set()
    pending_name: str | None = None
    for line in source.splitlines():
        json_name = re.search(r'@(?:JSONField|JsonProperty)\(name?\s*=\s*"([^"]+)"\)|@JsonProperty\("([^"]+)"\)', line)
        if json_name is not None:
            pending_name = next(group for group in json_name.groups() if group is not None)
        declaration = re.search(r'\bpublic\s+(?!class|static|interface)(?:[\w<>?, ]+)\s+(\w+)\s*(?:=|;)', line)
        if declaration is not None:
            fields.add(pending_name or declaration.group(1))
            pending_name = None
    return fields


def validate_characterization(spec: dict[str, JsonValue]) -> list[Check]:
    expected_routes = {
        (method.upper(), path)
        for path, item in spec.get("paths", {}).items()
        if isinstance(item, dict)
        for method in item
        if method.lower() in {"get", "post", "put", "delete"}
    }
    checks = [Check("controller:routes", controller_routes() == expected_routes, () if controller_routes() == expected_routes else (Reason("stale_state:routes"),))]
    characterization = spec.get("x-rehealth-characterization", {})
    if isinstance(characterization, dict):
        dto_contracts = characterization.get("dtoFields", {})
        if isinstance(dto_contracts, dict):
            for dto_name, expected in dto_contracts.items():
                if isinstance(expected, list) and all(isinstance(field, str) for field in expected):
                    actual = dto_fields(dto_name)
                    wanted = set(expected)
                    checks.append(Check(f"controller:dto:{dto_name}", actual == wanted, () if actual == wanted else (Reason(f"stale_state:dto:{dto_name}"),)))
            components = spec.get("components", {})
            schemas = components.get("schemas", {}) if isinstance(components, dict) else {}
            paths = spec.get("paths", {})
            referenced: set[str] = set()
            if isinstance(paths, dict):
                for item in paths.values():
                    if not isinstance(item, dict):
                        continue
                    for operation in item.values():
                        if not isinstance(operation, dict):
                            continue
                        for extension in ("x-request-dto", "x-response-dto"):
                            dto_name = operation.get(extension)
                            if isinstance(dto_name, str):
                                referenced.add(dto_name)
            for dto_name in sorted(referenced):
                accepted = isinstance(schemas, dict) and dto_name in schemas
                checks.append(Check(f"openapi:component:{dto_name}", accepted, () if accepted else (Reason(f"openapi:missing:{dto_name}"),)))
    return checks


def validate_all() -> list[Check]:
    checks: list[Check] = []
    openapi_path = CONTRACTS / "openapi" / "rehealth-mobile-v1.openapi.json"
    if not openapi_path.exists():
        return [Check("openapi", False, (Reason("missing:openapi"),))]
    spec = load_json(openapi_path)
    if not isinstance(spec, dict):
        return [Check("openapi", False, (Reason("schema:type"),))]
    checks.append(Check("openapi:version", spec.get("openapi") == "3.1.0", () if spec.get("openapi") == "3.1.0" else (Reason("openapi:version"),)))
    checks.extend(validate_characterization(spec))
    for schema_path in sorted((CONTRACTS / "schemas" / "events").glob("*.schema.json")):
        schema = load_json(schema_path)
        accepted = isinstance(schema, dict) and schema.get("additionalProperties") is False
        checks.append(Check(f"schema:{schema_path.name}", accepted, () if accepted else (Reason("schema:closed"),)))
    adr_text = "\n".join(path.read_text(encoding="utf-8") for path in sorted((CONTRACTS / "adrs").glob("*.md")))
    for phrase in ("durable write before success", "at-least-once", "idempotent consumer"):
        checks.append(Check(f"adr:{phrase}", phrase in adr_text.lower(), () if phrase in adr_text.lower() else (Reason(f"adr:missing:{phrase}"),)))
    return checks


def validate_fixtures(directory: Path) -> list[Check]:
    checks: list[Check] = []
    for path in sorted(directory.glob("*.json")):
        try:
            value = load_json(path)
        except (OSError, json.JSONDecodeError):
            checks.append(Check(str(path), False, (Reason("malformed_json"),)))
            continue
        reasons = policy_reasons(value)
        if not isinstance(value, dict) or not isinstance(value.get("event_type"), str):
            reasons.add(Reason("schema:missing:event_type"))
        else:
            schema_name = EVENT_SCHEMAS.get(value["event_type"])
            if schema_name is None:
                reasons.add(Reason("schema:unknown_event_type"))
            else:
                schema = load_json(CONTRACTS / "schemas" / "events" / schema_name)
                if isinstance(schema, dict):
                    reasons.update(schema_reasons(value, schema))
                else:
                    reasons.add(Reason("schema:definition"))
        checks.append(Check(str(path), not reasons, tuple(sorted(reasons))))
    return checks


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--fixtures", type=Path)
    parser.add_argument("--expect-rejected")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    checks = validate_all() if args.all else []
    if args.fixtures is not None:
        checks.extend(validate_fixtures(args.fixtures))
    rejected = sorted({str(reason) for check in checks for reason in check.reasons})
    expected = sorted(args.expect_rejected.split(",")) if args.expect_rejected else []
    success = (set(expected).issubset(rejected) and all(not check.accepted for check in checks)) if expected else all(check.accepted for check in checks)
    report = {
        "accepted": sum(check.accepted for check in checks),
        "checks": [{"artifact": check.artifact, "accepted": check.accepted, "reasons": list(check.reasons)} for check in checks],
        "expected_rejected_reasons": expected,
        "rejected": sum(not check.accepted for check in checks),
        "rejected_reasons": rejected,
        "status": "passed" if success else "failed",
    }
    rendered = json.dumps(report, indent=2, sort_keys=True)
    if args.report is not None:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    print(rendered)
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
