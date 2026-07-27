from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import create_app
from app.risk_scorer import RealCatBoostRiskScorer
from app.runtime_config import RuntimeConfig
from tests.test_api import feature_vector
from tests.test_risk_scorer import FakeProbabilityModel
from app.schemas import FEATURE_FIELDS


def real_scorer() -> RealCatBoostRiskScorer:
    return RealCatBoostRiskScorer(
        model=FakeProbabilityModel(),
        feature_order=FEATURE_FIELDS,
        model_version="reviewed-real-v1",
        loaded_artifact_name="reviewed.pkl",
    )


def test_matching_correlation_id_is_returned_in_response_and_model_trace() -> None:
    client = TestClient(create_app(RuntimeConfig(), scorer=real_scorer()))

    response = client.post(
        "/v1/cvd/risk/evaluate",
        headers={"X-Request-ID": "trace-test-123"},
        json=feature_vector(),
    )

    assert response.status_code == 200
    assert response.headers["X-Request-ID"] == "trace-test-123"
    assert response.json()["request_id"] == "trace-test-123"
    assert response.json()["model_trace"]["request_id"] == "trace-test-123"


def test_prometheus_metrics_use_only_bounded_operation_and_outcome_labels() -> None:
    client = TestClient(create_app(RuntimeConfig(), scorer=real_scorer()))
    client.post(
        "/v1/cvd/risk/evaluate",
        headers={"Authorization": "Bearer must-not-appear", "X-Request-ID": "trace-secret-test"},
        json=feature_vector(),
    )

    response = client.get("/metrics")

    assert response.status_code == 200
    body = response.text
    assert 'operation="risk_evaluate"' in body
    assert 'outcome="success"' in body
    assert "trace-secret-test" not in body
    assert "must-not-appear" not in body
    assert "fasting_glucose" not in body

