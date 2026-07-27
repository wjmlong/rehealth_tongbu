from __future__ import annotations

import re
from collections.abc import Awaitable, Callable
from time import perf_counter
from typing import Final
from uuid import uuid4

from fastapi import Request, Response
from prometheus_client import CollectorRegistry, Counter, Histogram, generate_latest


REQUEST_ID_PATTERN: Final = re.compile(r"^[A-Za-z0-9._:-]{1,128}$")
CONTENT_TYPE: Final = "text/plain; version=0.0.4; charset=utf-8"
OPERATIONS: Final = {
    "/health": "liveness",
    "/ready": "readiness",
    "/metrics": "metrics",
    "/runtime": "runtime",
    "/v1/models/active": "model_registry",
    "/v1/cvd/risk/evaluate": "risk_evaluate",
    "/v1/cvd/intervention/generate": "intervention_generate",
    "/v1/cvd/attribution/individual": "attribution_individual",
}
ALLOWED_OUTCOMES: Final = frozenset(
    {"success", "client_error", "unavailable", "timeout", "circuit_open", "server_error"}
)


def correlation_id(value: str | None) -> str:
    if value is not None and REQUEST_ID_PATTERN.fullmatch(value):
        return value
    return str(uuid4())


class ServiceMetrics:
    def __init__(self) -> None:
        self._registry = CollectorRegistry()
        self._requests = Counter(
            "rehealth_model_requests_total",
            "Completed model-service requests.",
            ("operation", "outcome"),
            registry=self._registry,
        )
        self._latency = Histogram(
            "rehealth_model_request_duration_seconds",
            "Model-service request latency.",
            ("operation", "outcome"),
            registry=self._registry,
            buckets=(0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0),
        )

    def observe(self, operation: str, outcome: str, duration_seconds: float) -> None:
        bounded_operation = OPERATIONS.get(operation, "other")
        bounded_outcome = outcome if outcome in ALLOWED_OUTCOMES else "server_error"
        self._requests.labels(bounded_operation, bounded_outcome).inc()
        self._latency.labels(bounded_operation, bounded_outcome).observe(duration_seconds)

    def render(self) -> bytes:
        return generate_latest(self._registry)


async def observe_request(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]],
    metrics: ServiceMetrics,
) -> Response:
    request.state.correlation_id = correlation_id(request.headers.get("X-Request-ID"))
    started = perf_counter()
    try:
        response = await call_next(request)
    except Exception:  # noqa: BROAD_EXCEPT_OK
        metrics.observe(request.url.path, "server_error", perf_counter() - started)
        raise
    outcome = getattr(request.state, "model_outcome", _outcome_for_status(response.status_code))
    metrics.observe(request.url.path, outcome, perf_counter() - started)
    response.headers["X-Request-ID"] = request.state.correlation_id
    return response


def _outcome_for_status(status_code: int) -> str:
    if status_code < 400:
        return "success"
    if status_code < 500:
        return "client_error"
    return "server_error"
