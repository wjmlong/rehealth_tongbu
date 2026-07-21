from datetime import datetime
from typing import Optional

from .feature_extractor import HealthFeatureExtractor
from .memory_manager import HealthMemoryManager
from .desensitizer import HealthDesensitizer


class DreamScheduler:

    def __init__(
        self,
        feature_extractor: HealthFeatureExtractor,
        memory_manager: HealthMemoryManager,
        desensitizer: HealthDesensitizer,
    ):
        self.extractor = feature_extractor
        self.memory = memory_manager
        self.desensitizer = desensitizer

    @staticmethod
    def should_trigger(
        now: Optional[datetime] = None,
        is_charging: bool = True,
        is_wifi: bool = True,
        is_screen_locked: bool = True,
    ) -> bool:
        now = now or datetime.now()
        time_ok = 2 <= now.hour < 5
        return time_ok and is_charging and is_wifi and is_screen_locked

    def run_dream_cycle(self, raw_data: dict) -> dict:
        features = self.extractor.extract_all(
            sbp_hourly=raw_data.get("sbp_hourly", []),
            dbp_hourly=raw_data.get("dbp_hourly", []),
            hr_series=raw_data.get("hr_series", []),
            hrv_series=raw_data.get("hrv_series", []),
            calories_daily=raw_data.get("calories_daily", []),
            sport_events=raw_data.get("sport_events", []),
            sleep_hours=raw_data.get("sleep_hours", []),
        )

        mem = self.memory.load()
        mem = self.memory.update_dynamic_vectors(mem, features)
        confounders = self.extractor.encode_time_confounders()
        mem = self.memory.update_time_confounders(mem, confounders)
        self.memory.save(mem)

        payload = self.desensitizer.desensitize(mem)
        return payload
