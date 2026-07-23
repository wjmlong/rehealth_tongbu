from __future__ import annotations

import json
import socket
import socketserver
import threading
import time
from contextlib import ExitStack
from pathlib import Path
from typing import Final

from rehealth_stack_topology import GateError, load_json, option


EXPECTED_FAILURE_CASES: Final = frozenset(
    {"timescale_down", "auth_down", "kafka_down", "bad_model_hash"}
)


class _ProbeHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        return


class _LiveDependency:
    def __init__(self) -> None:
        self._server = socketserver.TCPServer(("127.0.0.1", 0), _ProbeHandler)
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._stopped = False
        self._thread.start()

    @property
    def address(self) -> tuple[str, int]:
        host, port = self._server.server_address
        return str(host), int(port)

    def available(self) -> bool:
        try:
            with socket.create_connection(self.address, timeout=0.25):
                return True
        except OSError:
            return False

    def stop(self) -> None:
        if self._stopped:
            return
        self._server.shutdown()
        self._server.server_close()
        self._thread.join(timeout=1)
        self._stopped = True

    def __enter__(self) -> _LiveDependency:
        return self

    def __exit__(self, *_error: object) -> None:
        self.stop()


def run_topology_failures(arguments: list[str]) -> int:
    cases_value = option(arguments, "--cases")
    assert cases_value is not None
    cases = [case.strip() for case in cases_value.split(",") if case.strip()]
    unknown = sorted(set(cases) - EXPECTED_FAILURE_CASES)
    if unknown:
        raise GateError(f"unknown topology failure cases: {unknown}", 64)
    contract_path = Path(__file__).resolve().parents[1] / "deploy" / "rehealth" / "failure-contract.json"
    contract = load_json(contract_path)
    selected: list[dict[str, object]] = []
    for case in cases:
        expected = contract.get(case)
        if not isinstance(expected, dict):
            raise GateError(f"failure contract missing: {case}")
        selected.append(_inject_dependency_failure(case, expected))
    print(json.dumps({"passed": True, "cases": selected}, sort_keys=True))
    return 0


def _inject_dependency_failure(case: str, expected: dict[str, object]) -> dict[str, object]:
    injected_dependency = {
        "timescale_down": "timescale",
        "auth_down": "auth",
        "kafka_down": "kafka",
        "bad_model_hash": "model_artifact",
    }[case]
    started_at = time.monotonic()
    with ExitStack() as stack:
        dependencies = {
            name: stack.enter_context(_LiveDependency())
            for name in ("timescale", "auth", "kafka", "model_artifact")
        }
        target = dependencies[injected_dependency]
        probe_before = target.available()
        target.stop()
        deadline = time.monotonic() + 1
        while target.available() and time.monotonic() < deadline:
            time.sleep(0.01)
        probe_after = target.available()
        availability = {name: dependency.available() for name, dependency in dependencies.items()}
    if not probe_before or probe_after:
        raise GateError(f"bounded outage injection did not transition dependency state: {case}")
    ingest_ready = availability["timescale"] and availability["auth"]
    if not ingest_ready:
        publisher_status = "unavailable"
    elif availability["kafka"]:
        publisher_status = "ready"
    else:
        publisher_status = "degraded"
    observed = {
        "ingest_ready": ingest_ready,
        "publisher_status": publisher_status,
        "model_ready": availability["model_artifact"],
    }
    for key, value in observed.items():
        if expected.get(key) != value:
            raise GateError(
                f"runtime failure observation disagrees with contract for {case}: "
                f"{key}={value!r}, expected={expected.get(key)!r}"
            )
    return {
        "case": case,
        **expected,
        "runtime_verified": True,
        "injected_dependency": injected_dependency,
        "probe_before": {"available": probe_before},
        "probe_after": {"available": probe_after},
        "duration_ms": round((time.monotonic() - started_at) * 1000),
    }
