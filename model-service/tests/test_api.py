from fastapi.testclient import TestClient

from app.main import app, create_app
from app.runtime_config import AttributionMode, RuntimeConfig, RuntimeMode


client = TestClient(app)


def feature_vector():
    quality = {
        field: {
            "status": "VALID",
            "source": "USER_REPORTED",
            "reason": "test fixture",
        }
        for field in [
            "age",
            "gender",
            "bmi",
            "sbp",
            "dbp",
            "fasting_glucose",
            "total_cholesterol",
            "ldl",
            "hdl",
            "triglycerides",
            "exercise_days",
            "smoking",
            "drinking",
            "diabetes_history",
            "hypertension_history",
            "family_history",
        ]
    }
    return {
        "age": 52,
        "gender": 1,
        "bmi": 27.4,
        "sbp": 136.0,
        "dbp": 86.0,
        "fasting_glucose": None,
        "total_cholesterol": None,
        "ldl": None,
        "hdl": None,
        "triglycerides": None,
        "exercise_days": 3,
        "smoking": 0,
        "drinking": 0,
        "diabetes_history": 0,
        "hypertension_history": 1,
        "family_history": 1,
        "featureQuality": quality,
    }


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["service"] == "model-service"
    assert body["model_version"]
    assert body["model_registry_version"] == "model-registry-v1"
    assert body["feature_schema_version"] == "cvd-16-v1"
    assert body["scorer_mode"] in {"mock", "real_available", "real_unavailable"}
    assert body["model_available"] is (body["scorer_mode"] == "real_available")
    if body["model_available"]:
        assert body["loaded_artifact_name"] == "rehealth_cvd_catboost.pkl"
        assert "model_unavailable_reason" not in body
    else:
        assert "model_unavailable_reason" in body
    assert body["expected_model_artifacts"] == [
        "models/rehealth_cvd_catboost.pkl",
        "models/rehealth_v2_final.pkl",
    ]
    assert body["supported_model_artifact_aliases"] == [
        "models/rehealth_cvd_catboost.pkl",
        "models/rehealth_v2_final.pkl",
    ]
    assert body["expected_feature_order_artifacts"] == [
        "models/feature_cols.pkl",
        "models/feature_cols_v2.pkl",
        "models/cvd_features.json",
    ]


def test_risk_endpoint_accepts_flat_android_contract():
    response = client.post("/v1/cvd/risk/evaluate", json=feature_vector())

    assert response.status_code == 200
    body = response.json()
    assert body["model_version"]
    health = client.get("/health").json()
    assert body["is_mock"] is (not health["model_available"])
    assert 0 <= body["risk_score"] <= 1
    assert body["risk_level"] in {"low", "moderate", "high", "very_high"}
    assert "feature_contributions" in body
    assert "fasting_glucose" in body["missing_fields"]
    assert body["model_trace"]["feature_schema_version"] == "cvd-16-v1"
    assert body["model_trace"]["model_version"] == body["model_version"]
    assert body["model_trace"]["scorer_mode"] == health["scorer_mode"]
    if body["is_mock"]:
        assert body["model_trace"]["fallback_reason"]
    else:
        assert body["model_trace"]["artifact_name"] == "rehealth_cvd_catboost.pkl"
        assert "fallback_reason" not in body["model_trace"]


def test_risk_endpoint_preserves_request_id_when_provided():
    response = client.post(
        "/v1/cvd/risk/evaluate",
        json={"requestId": "req-test-123", "featureVector": feature_vector()},
    )

    assert response.status_code == 200
    assert response.json()["request_id"] == "req-test-123"
    assert response.json()["model_trace"]["request_id"] == "req-test-123"


def test_risk_endpoint_response_field_names_are_stable():
    response = client.post("/v1/cvd/risk/evaluate", json=feature_vector())

    assert response.status_code == 200
    assert {
        "risk_score",
        "risk_level",
        "feature_contributions",
        "model_version",
        "is_mock",
        "missing_fields",
        "quality_warnings",
        "summary",
        "model_trace",
    }.issubset(response.json())
    assert set(response.json()) <= {
        "risk_score",
        "risk_level",
        "feature_contributions",
        "model_version",
        "is_mock",
        "missing_fields",
        "quality_warnings",
        "summary",
        "request_id",
        "contribution_method",
        "base_value",
        "model_trace",
    }


def test_risk_endpoint_accepts_wrapped_android_contract_and_camel_names():
    payload = feature_vector()
    payload["fastingGlucose"] = payload.pop("fasting_glucose")
    payload["totalCholesterol"] = payload.pop("total_cholesterol")
    payload["exerciseDays"] = payload.pop("exercise_days")
    payload["diabetesHistory"] = payload.pop("diabetes_history")
    payload["hypertensionHistory"] = payload.pop("hypertension_history")
    payload["familyHistory"] = payload.pop("family_history")

    response = client.post("/v1/cvd/risk/evaluate", json={"featureVector": payload})

    assert response.status_code == 200
    assert response.json()["model_version"]


def test_intervention_endpoint_returns_conservative_disclaimer():
    risk = client.post("/v1/cvd/risk/evaluate", json=feature_vector()).json()

    response = client.post(
        "/v1/cvd/intervention/generate",
        json={"riskResult": risk, "featureVector": feature_vector()},
    )

    assert response.status_code == 200
    body = response.json()
    assert set(body) == {
        "plan_id",
        "generated_at",
        "priority_intervention",
        "rationale",
        "expected_impact",
        "contraindications",
        "confidence",
        "model_version",
        "is_mock",
        "medical_disclaimer",
    }
    assert body["plan_id"].startswith("plan-")
    assert body["priority_intervention"]
    assert body["rationale"]
    assert body["expected_impact"]
    assert body["contraindications"]
    assert 0 <= body["confidence"] <= 1
    assert body["is_mock"] is risk["is_mock"]
    assert "does not diagnose" in body["medical_disclaimer"]


def test_risk_endpoint_rejects_missing_quality_entries():
    payload = feature_vector()
    payload["featureQuality"].pop("age")

    response = client.post("/v1/cvd/risk/evaluate", json=payload)

    assert response.status_code == 422


def test_individual_attribution_endpoint_shape():
    demo_client = TestClient(create_app(RuntimeConfig(
        runtime_mode=RuntimeMode.DEMO,
        attribution_mode=AttributionMode.DEMO_MOCK,
        demo_enabled=True,
        mock_attribution_enabled=True,
        provenance="demo_mock",
    )))
    response = demo_client.post(
        "/v1/cvd/attribution/individual",
        json={
            "baselineRiskScore": 0.40,
            "events": [
                {"date": "2026-07-01", "risk_score": 0.40, "intervention_id": "walking", "adherence": 0.5},
                {"date": "2026-07-08", "risk_score": 0.34, "intervention_id": "walking", "adherence": 0.8},
            ],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["model_version"]
    assert body["trend_delta"] == -0.06
    assert body["adherence_average"] == 0.65


def test_mock_attribution_endpoint_is_absent_by_default():
    response = client.post(
        "/v1/cvd/attribution/individual",
        json={"baselineRiskScore": 0.4, "events": []},
    )

    assert response.status_code == 404
