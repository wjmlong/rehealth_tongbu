import json
import os
import time
from typing import Optional
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

SCHEMA_VERSION = "1.0.0"


def _empty_memory(device_id: str, disease_type: str = "CVD") -> dict:
    return {
        "schema_version": SCHEMA_VERSION,
        "device_id": device_id,
        "disease_type": disease_type,
        "created_at": int(time.time()),
        "last_updated": int(time.time()),
        "update_count": 0,
        "static_baseline": {
            "age": None, "gender": None, "bmi": None,
            "total_cholesterol": None, "ldl": None, "hdl": None,
            "triglycerides": None, "fbg": None,
            "smoking_status": None, "family_cvd_history": None,
        },
        "dynamic_memory_vectors": {
            "window_days": 14,
            "sbp_mean": None, "sbp_std": None, "sbp_slope": None,
            "dbp_mean": None, "dbp_std": None,
            "resting_hr_mean": None, "hrv_rmssd_mean": None,
            "night_bp_pattern": None,
            "active_calories_daily_mean": None,
            "active_calories_trend": None,
            "sport_freq_weekly": None,
            "sleep_hours_mean": None, "sleep_quality_index": None,
        },
        "intervention_compliance": {
            "current_plan_id": None,
            "plan_start_date": None,
            "diet_compliance_rate": None,
            "exercise_trigger_count": 0,
            "consecutive_active_days": 0,
        },
        "risk_score_history": [],
        "time_confounders": {
            "season_sin": None, "season_cos": None,
            "day_of_week": None, "is_weekend": None,
        },
    }


class HealthMemoryManager:

    def __init__(self, memory_path: str, encryption_key: bytes):
        self.memory_path = memory_path
        if len(encryption_key) != 32:
            raise ValueError("AES-256 key must be 32 bytes")
        self._aesgcm = AESGCM(encryption_key)

    def load(self) -> dict:
        if not os.path.exists(self.memory_path):
            return _empty_memory(device_id="unknown")
        with open(self.memory_path, "rb") as f:
            data = f.read()
        nonce = data[:12]
        ciphertext = data[12:]
        plaintext = self._aesgcm.decrypt(nonce, ciphertext, None)
        return json.loads(plaintext.decode("utf-8"))

    def save(self, memory: dict):
        memory["last_updated"] = int(time.time())
        memory["update_count"] = memory.get("update_count", 0) + 1
        plaintext = json.dumps(memory, ensure_ascii=False).encode("utf-8")
        nonce = os.urandom(12)
        ciphertext = self._aesgcm.encrypt(nonce, plaintext, None)
        os.makedirs(os.path.dirname(self.memory_path) or ".", exist_ok=True)
        with open(self.memory_path, "wb") as f:
            f.write(nonce + ciphertext)

    def update_dynamic_vectors(self, memory: dict, features: dict) -> dict:
        dv = memory.setdefault("dynamic_memory_vectors", {})
        for key in [
            "sbp_mean", "sbp_std", "sbp_slope",
            "dbp_mean", "dbp_std",
            "resting_hr_mean", "hrv_rmssd_mean",
            "night_bp_pattern",
            "active_calories_daily_mean", "active_calories_trend",
            "sport_freq_weekly", "sleep_hours_mean",
        ]:
            if key in features:
                dv[key] = features[key]
        return memory

    def update_time_confounders(self, memory: dict, confounders: dict) -> dict:
        tc = memory.setdefault("time_confounders", {})
        for key in ["season_sin", "season_cos", "day_of_week", "is_weekend"]:
            if key in confounders:
                tc[key] = confounders[key]
        return memory

    def append_risk_score(
        self, memory: dict, date: str, Y: float, Z: int
    ) -> dict:
        history = memory.setdefault("risk_score_history", [])
        history.append({"date": date, "Y": round(Y, 6), "Z": Z})
        if len(history) > 365:
            memory["risk_score_history"] = history[-365:]
        return memory

    def set_static_baseline(self, memory: dict, baseline: dict) -> dict:
        sb = memory.setdefault("static_baseline", {})
        sb.update(baseline)
        return memory

    def create_new(self, device_id: str, disease_type: str = "CVD") -> dict:
        return _empty_memory(device_id, disease_type)
