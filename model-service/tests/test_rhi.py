from copy import deepcopy

from fastapi.testclient import TestClient

from app.main import create_app
from app.rhi import RHI_FEATURE_FIELDS
from app.risk_scorer import MockRiskScorer
from app.runtime_config import RuntimeConfig, RuntimeMode


client = TestClient(create_app(scorer=MockRiskScorer()))


def payload() -> dict:
    values = {
        "age": 42,
        "biological_sex": "female",
        "waist_circumference_cm": 82.0,
        "bmi": 23.0,
        "sbp_7d_mean": 124.0,
        "total_cholesterol": 4.6,
        "hdl_c": 1.4,
        "ldl_c": 2.5,
        "triglycerides": 1.2,
        "glycemia_value": 5.4,
        "egfr": 96.0,
        "nicotine_exposure": 0,
        "diabetes_status": 0,
        "antihypertensive_medication": 0,
        "lipid_lowering_medication": 0,
        "premature_cvd_family_history": 0,
        "dbp_7d_mean": 78.0,
        "resting_hr_14d_median": 64.0,
        "resting_hr_change_28d_pct": -3.0,
        "nocturnal_hrv_14d_median": 42.0,
        "hrv_change_28d_pct": 8.0,
        "cardiorespiratory_fitness_score": 68.0,
        "sleep_duration_7d_mean_hours": 7.2,
        "sleep_regularity_14d_pct": 82.0,
        "sleep_efficiency_14d_pct": 88.0,
        "nocturnal_spo2_drop_burden_14d_pct": 1.0,
        "steps_7d_mean": 7200.0,
        "mvpa_minutes_7d": 165.0,
        "sedentary_hours_7d_mean": 7.0,
        "active_day_regularity_14d_pct": 75.0,
        "weight_change_28d_pct": -1.0,
        "adherence_composite_28d_pct": 84.0,
    }
    quality = {
        name: {
            "status": "VALID",
            "source": "DERIVED",
            "reason": "synthetic test fixture",
        }
        for name in RHI_FEATURE_FIELDS
    }
    return {
        "featureVector": values,
        "featureQuality": quality,
        "productTier": "clinical",
        "glycemiaMetric": "hba1c_percent",
        "personalBaselines": {
            "steps_7d_mean": {
                "median": 6000,
                "mad": 500,
                "sample_count": 28,
            },
            "nocturnal_hrv_14d_median": {
                "median": 38,
                "mad": 3,
                "sample_count": 28,
                "device_fingerprint": "same-device",
            },
        },
        "history": {
            "available_days": 35,
            "previous_display_score": 70,
            "display_score_7d_ago": 68,
            "display_score_28d_ago": 64,
        },
        "clinicalRisk": {
            "model": "china_par_reviewed_v1",
            "risk_10y": 0.028,
            "risk_level": "low",
            "applicable": True,
            "last_updated_at": "2026-07-30",
            "model_version": "1.0.0",
        },
        "deviceContext": {
            "brand": "synthetic",
            "model": "test",
            "signal_quality": 0.9,
            "device_change_detected": False,
        },
        "safetyFlags": [],
        "requestId": "rhi-test-1",
    }


def test_rhi_preview_returns_four_separate_outputs_and_trace_status():
    response = client.post("/v2/rhi/evaluate", json=payload())

    assert response.status_code == 200
    body = response.json()
    assert body["schema_version"] == "rhi-core32-v2-preview"
    assert body["algorithm_status"] == "research_preview_not_clinically_validated"
    assert body["clinical_risk"]["risk_10y"] == 0.028
    assert 0 <= body["dynamic_health_index"]["score"] <= 100
    assert body["dynamic_health_index"]["delta_28d"] is not None
    assert body["domains"]["activity_fitness"] is not None
    assert 0 <= body["data_confidence"]["score"] <= 1
    assert body["request_id"] == "rhi-test-1"
    assert 'operation="rhi_evaluate",outcome="success"' in client.get("/metrics").text


def test_rhi_does_not_invent_clinical_probability():
    request = payload()
    request.pop("clinicalRisk")

    response = client.post("/v2/rhi/evaluate", json=request)

    assert response.status_code == 200
    clinical = response.json()["clinical_risk"]
    assert clinical["applicable"] is False
    assert clinical["risk_10y"] is None
    assert clinical["model"] == "not_available"


def test_higher_sbp_cannot_improve_hemodynamic_domain():
    baseline = payload()
    higher = deepcopy(baseline)
    baseline["featureVector"]["sbp_7d_mean"] = 120.0
    higher["featureVector"]["sbp_7d_mean"] = 160.0
    baseline["history"]["previous_display_score"] = None
    higher["history"]["previous_display_score"] = None

    lower_result = client.post("/v2/rhi/evaluate", json=baseline).json()
    higher_result = client.post("/v2/rhi/evaluate", json=higher).json()

    assert higher_result["domains"]["hemodynamic"] < lower_result["domains"]["hemodynamic"]


def test_missing_data_shrinks_feature_effect_and_reduces_confidence():
    complete = payload()
    missing = deepcopy(complete)
    missing["featureVector"]["steps_7d_mean"] = None
    missing["featureQuality"]["steps_7d_mean"] = {
        "status": "MISSING",
        "source": "UNKNOWN",
        "reason": "not observed",
    }

    complete_result = client.post("/v2/rhi/evaluate", json=complete).json()
    missing_result = client.post("/v2/rhi/evaluate", json=missing).json()

    assert missing_result["data_confidence"]["score"] < complete_result["data_confidence"]["score"]
    assert "steps_7d_mean" in missing_result["data_confidence"]["missing_fields"]
    assert missing_result["domains"]["activity_fitness"] < complete_result["domains"]["activity_fitness"]


def test_all_32_quality_entries_are_required():
    request = payload()
    request["featureQuality"].pop("age")

    response = client.post("/v2/rhi/evaluate", json=request)

    assert response.status_code == 422
    assert "feature_quality missing entries" in response.text


def test_lite_tier_does_not_penalize_unavailable_clinical_labs():
    request = payload()
    request["productTier"] = "lite"
    for name in ("glycemia_value", "ldl_c", "hdl_c", "triglycerides"):
        request["featureVector"][name] = None
        request["featureQuality"][name] = {
            "status": "MISSING",
            "source": "UNKNOWN",
            "reason": "not required for lite",
        }

    response = client.post("/v2/rhi/evaluate", json=request)

    assert response.status_code == 200
    body = response.json()
    assert body["domains"]["metabolic_control"] is None
    assert "glycemia_value" not in body["data_confidence"]["missing_fields"]


def test_missing_wearable_data_cannot_raise_display_score():
    complete = payload()
    complete["productTier"] = "lite"
    complete["featureVector"]["steps_7d_mean"] = 1000
    complete["history"]["previous_display_score"] = 40
    missing = deepcopy(complete)
    missing["featureVector"]["steps_7d_mean"] = None
    missing["featureQuality"]["steps_7d_mean"] = {
        "status": "MISSING",
        "source": "UNKNOWN",
        "reason": "watch was not worn",
    }

    missing_result = client.post("/v2/rhi/evaluate", json=missing).json()

    assert missing_result["dynamic_health_index"]["score"] <= 40


def test_device_change_disables_personal_baseline_effect():
    same_device = payload()
    changed_device = deepcopy(same_device)
    changed_device["deviceContext"]["device_change_detected"] = True
    same_device["history"]["previous_display_score"] = None
    changed_device["history"]["previous_display_score"] = None

    same_result = client.post("/v2/rhi/evaluate", json=same_device).json()
    changed_result = client.post("/v2/rhi/evaluate", json=changed_device).json()

    assert changed_result["data_confidence"]["score"] < same_result["data_confidence"]["score"]
    assert changed_result["data_confidence"]["device_change_detected"] is True


def test_implausible_seven_day_blood_pressure_is_rejected():
    request = payload()
    request["featureVector"]["sbp_7d_mean"] = 60

    response = client.post("/v2/rhi/evaluate", json=request)

    assert response.status_code == 422
    assert "sbp_7d_mean must be between 70 and 250" in response.text


def test_rhi_preview_fails_closed_in_production():
    protected_client = TestClient(
        create_app(
            runtime_config=RuntimeConfig(
                runtime_mode=RuntimeMode.PRODUCTION,
                service_base_url="https://model.internal.example",
                provider_credential_file="/run/secrets/provider",
            ),
            scorer=MockRiskScorer(),
        ),
    )

    response = protected_client.post("/v2/rhi/evaluate", json=payload())

    assert response.status_code == 503
    assert response.json()["detail"]["code"] == "rhi_preview_disabled"
