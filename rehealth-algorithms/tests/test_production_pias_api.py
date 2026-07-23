from __future__ import annotations

import os

from fastapi.testclient import TestClient

os.environ.setdefault("REHEALTH_PIAS_INTERNAL_TOKEN", "synthetic-import-token")

from api.production_config import PiasSettings
from api.production_main import create_app


TOKEN = "synthetic-internal-token"
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "X-Request-ID": "req-ready",
    "Idempotency-Key": "idem-ready",
}


def client() -> TestClient:
    return TestClient(create_app(PiasSettings(internal_token=TOKEN)))


def history(days: int) -> list[dict[str, str | float | int]]:
    return [
        {
            "date": f"2026-07-{index + 1:02d}",
            "Y": round(0.42 - index * 0.005, 4),
            "Z": index % 2,
        }
        for index in range(days)
    ]


def test_exposes_only_production_routes() -> None:
    api = create_app(PiasSettings(internal_token=TOKEN))

    routes = {route.path for route in api.routes if not route.path.startswith(("/openapi", "/docs", "/redoc"))}

    assert routes == {"/health", "/health/readiness", "/api/pias/v2/attribute/individual"}


def test_rejects_missing_internal_authentication() -> None:
    response = client().post(
        "/api/pias/v2/attribute/individual",
        headers={"X-Request-ID": "req-unauthorized", "Idempotency-Key": "idem-unauthorized"},
        json={"risk_history": history(14)},
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "PIAS_AUTH_REQUIRED"


def test_returns_ready_ui_contract_with_engine_provenance() -> None:
    response = client().post(
        "/api/pias/v2/attribute/individual",
        headers=HEADERS,
        json={"risk_history": history(14), "forecast_days": 30, "language": "zh"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["request_id"] == "req-ready"
    result = body["result"]
    assert result["status"] == "ready"
    assert result["attribution_mode"] == "pias"
    assert result["is_mock"] is False
    assert result["provider"] == "pias"
    assert result["model_version"] == "pias-individual-v2"
    assert len(result["forecast"]["raw"]["dates"]) == 30
    assert "individual_att" in result["intervention_effect"]


def test_returns_accumulating_without_inventing_forecast_or_att() -> None:
    response = client().post(
        "/api/pias/v2/attribute/individual",
        headers={**HEADERS, "X-Request-ID": "req-acc", "Idempotency-Key": "idem-acc"},
        json={"risk_history": history(2)},
    )

    result = response.json()["result"]
    assert result["status"] == "accumulating"
    assert result["forecast"]["raw"]["dates"] == []
    assert result["intervention_effect"]["individual_att"] is None
    assert result["history_days"] == 2


def test_replays_same_idempotent_response_and_rejects_key_reuse() -> None:
    api_client = client()
    first = api_client.post(
        "/api/pias/v2/attribute/individual",
        headers=HEADERS,
        json={"risk_history": history(14)},
    )
    replay = api_client.post(
        "/api/pias/v2/attribute/individual",
        headers={**HEADERS, "X-Request-ID": "req-replay"},
        json={"risk_history": history(14)},
    )
    conflict = api_client.post(
        "/api/pias/v2/attribute/individual",
        headers=HEADERS,
        json={"risk_history": history(2)},
    )

    assert replay.status_code == 200
    assert replay.json() == first.json()
    assert conflict.status_code == 409
    assert conflict.json()["error"]["code"] == "PIAS_IDEMPOTENCY_CONFLICT"


def test_validation_failure_uses_stable_error_envelope() -> None:
    response = client().post(
        "/api/pias/v2/attribute/individual",
        headers={**HEADERS, "X-Request-ID": "req-invalid", "Idempotency-Key": "idem-invalid"},
        json={"risk_history": [{"date": "invalid", "Y": 4.2, "Z": 9}]},
    )

    assert response.status_code == 422
    assert response.json()["request_id"] == "req-invalid"
    assert response.json()["error"]["code"] == "PIAS_INVALID_REQUEST"
