from __future__ import annotations

import json

from cutover_types import GateError


TELEMETRY_ROUTE_ID = "rehealth-device-telemetry"
TELEMETRY_PATHS = (
    "/jeecg-boot/rehealth/mobile/measurements/batch",
    "/jeecg-boot/rehealth/mobile/measurements/recent",
)


def route_paths(route: dict[str, object]) -> set[str]:
    predicates = route.get("predicates")
    if not isinstance(predicates, list):
        return set()
    paths: set[str] = set()
    for predicate in predicates:
        if isinstance(predicate, dict) and predicate.get("name") == "Path":
            args = predicate.get("args")
            if isinstance(args, dict):
                paths.update(value for value in args.values() if isinstance(value, str))
    return paths


def parse_routes(content: bytes) -> list[dict[str, object]]:
    try:
        value = json.loads(content)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise GateError("ROUTE_CONFIG_INVALID") from error
    if not isinstance(value, list) or not all(isinstance(route, dict) for route in value):
        raise GateError("ROUTE_CONFIG_INVALID")
    return value


def device_routes(routes: list[dict[str, object]]) -> list[dict[str, object]]:
    for route in routes:
        if route.get("id") not in {TELEMETRY_ROUTE_ID, "rehealth-business"}:
            if set(TELEMETRY_PATHS) & route_paths(route):
                raise GateError("ROUTE_COLLISION")
    business = next((route for route in routes if route.get("id") == "rehealth-business"), None)
    if business is None or business.get("uri") != "lb://jeecg-system":
        raise GateError("BUSINESS_ROUTE_INVALID")
    telemetry = {
        "id": TELEMETRY_ROUTE_ID,
        "order": -100,
        "predicates": [
            {
                "name": "Path",
                "args": {f"_genkey_{index}": path for index, path in enumerate(TELEMETRY_PATHS)},
            }
        ],
        "filters": [
            {"name": "StripPrefix", "args": {"parts": "1"}},
            {
                "name": "RemoveRequestHeader",
                "args": {"name": "X-ReHealth-User-Id"},
            },
            {
                "name": "RemoveRequestHeader",
                "args": {"name": "X-ReHealth-Tenant-Id"},
            },
        ],
        "uri": "lb://rehealth-device-service",
    }
    remaining = [route for route in routes if route.get("id") != TELEMETRY_ROUTE_ID]
    remaining.append(telemetry)
    return sorted(remaining, key=lambda route: int(route.get("order", 0)))
