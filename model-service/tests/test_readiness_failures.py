from __future__ import annotations

import pickle
from threading import Event

from fastapi.testclient import TestClient

from app.main import create_app
from app.risk_scorer import (
    MockRiskScorer,
    RealCatBoostRiskScorer,
    RealModelArtifacts,
    load_risk_scorer,
)
from app.runtime_config import RuntimeConfig, RuntimeMode
from app.schemas import FEATURE_FIELDS
from tests.test_api import feature_vector
from tests.test_risk_scorer import FakeProbabilityModel


class BlockingRealScorer(RealCatBoostRiskScorer):
    def __init__(self) -> None:
        super().__init__(
            model=FakeProbabilityModel(),
            feature_order=FEATURE_FIELDS,
            model_version="blocking-real-v1",
            loaded_artifact_name="blocking.pkl",
        )
        self.release = Event()

    def evaluate(self, vector):
        self.release.wait()
        return super().evaluate(vector)


def production_config(verification_file: str) -> RuntimeConfig:
    return RuntimeConfig(
        runtime_mode=RuntimeMode.PRODUCTION,
        service_base_url="https://model.internal.example",
        provider_credential_file="/run/secrets/provider",
        model_verification_file=verification_file,
        model_evaluation_timeout_seconds=0.05,
        model_circuit_failure_threshold=1,
    )


def test_liveness_stays_up_while_unavailable_model_is_not_ready(tmp_path) -> None:
    config = production_config(str(tmp_path / "missing-verification"))
    client = TestClient(create_app(config, scorer=MockRiskScorer(scorer_mode="real_unavailable")))

    live = client.get("/health")
    ready = client.get("/ready")
    risk = client.post("/v1/cvd/risk/evaluate", json=feature_vector())

    assert live.status_code == 200
    assert live.json()["status"] == "ok"
    assert ready.status_code == 503
    assert ready.json()["status"] == "unavailable"
    assert ready.json()["code"] == "model_unavailable"
    assert risk.status_code == 503
    assert risk.json()["detail"]["code"] == "model_unavailable"
    assert "risk_score" not in risk.json()


def test_production_readiness_requires_external_artifact_verification(tmp_path) -> None:
    scorer = RealCatBoostRiskScorer(
        model=FakeProbabilityModel(),
        feature_order=FEATURE_FIELDS,
        model_version="reviewed-real-v1",
        loaded_artifact_name="reviewed.pkl",
    )
    config = production_config(str(tmp_path / "missing-verification"))

    response = TestClient(create_app(config, scorer=scorer)).get("/ready")

    assert response.status_code == 503
    assert response.json()["code"] == "artifact_not_verified"


def test_safe_registry_metadata_never_exposes_artifact_paths_or_validation_details(tmp_path) -> None:
    missing_path = tmp_path / "private" / "patient-model.pkl"
    scorer = MockRiskScorer(
        scorer_mode="real_unavailable",
        unavailable_reason=f"model artifact missing: {missing_path}",
    )
    client = TestClient(create_app(RuntimeConfig(), scorer=scorer))

    response = client.get("/v1/models/active")

    assert response.status_code == 200
    body = response.text
    assert str(tmp_path) not in body
    assert "patient-model.pkl" not in body
    assert response.json()["readiness_code"] == "model_unavailable"


def test_wrong_feature_order_stays_not_ready_and_evaluation_never_fakes_success(tmp_path) -> None:
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "reviewed.pkl"
    feature_order_path = artifact_root / "feature-order.pkl"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(["age", "gender"]))
    scorer = load_risk_scorer(RealModelArtifacts(
        artifact_root=artifact_root,
        model_path=model_path,
        feature_order_path=feature_order_path,
    ))
    client = TestClient(create_app(RuntimeConfig(), scorer=scorer))

    ready = client.get("/ready")
    risk = client.post("/v1/cvd/risk/evaluate", json=feature_vector())

    assert ready.status_code == 503
    assert ready.json()["code"] == "model_unavailable"
    assert risk.status_code == 503
    assert risk.json()["detail"]["code"] == "model_unavailable"
    assert "risk_score" not in risk.json()


def test_evaluation_timeout_and_open_circuit_return_stable_503_without_fake_success() -> None:
    scorer = BlockingRealScorer()
    config = RuntimeConfig(
        model_evaluation_timeout_seconds=0.01,
        model_circuit_failure_threshold=1,
        model_circuit_reset_seconds=30.0,
    )
    with TestClient(create_app(config, scorer=scorer)) as client:
        timed_out = client.post("/v1/cvd/risk/evaluate", json=feature_vector())
        circuit_open = client.post("/v1/cvd/risk/evaluate", json=feature_vector())
        scorer.release.set()

    assert timed_out.status_code == 503
    assert timed_out.json()["detail"]["code"] == "model_timeout"
    assert circuit_open.status_code == 503
    assert circuit_open.json()["detail"]["code"] == "model_circuit_open"
    assert "risk_score" not in timed_out.json()
    assert "risk_score" not in circuit_open.json()
