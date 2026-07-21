import numpy as np
from datetime import datetime
from typing import List, Optional


class HealthFeatureExtractor:

    def __init__(self, window_days: int = 14):
        self.window_days = window_days

    def extract_bp_features(
        self, sbp_hourly: List[float], dbp_hourly: List[float]
    ) -> dict:
        expected = self.window_days * 24
        sbp = np.array(sbp_hourly[-expected:], dtype=float)
        dbp = np.array(dbp_hourly[-expected:], dtype=float)

        if len(sbp) < 24:
            return {}

        n_days = len(sbp) // 24
        sbp_daily = sbp[: n_days * 24].reshape(n_days, 24)

        night = sbp_daily[:, 0:6].mean(axis=1)
        day = sbp_daily[:, 10:22].mean(axis=1)
        dip_ratios = (day - night) / np.where(day > 0, day, 1)
        avg_dip = float(dip_ratios.mean())

        if avg_dip >= 0.20:
            pattern = "extreme_dipper"
        elif avg_dip >= 0.10:
            pattern = "dipper"
        elif avg_dip >= 0.0:
            pattern = "non_dipper"
        else:
            pattern = "reverse_dipper"

        t = np.arange(len(sbp), dtype=float)
        slope = float(np.polyfit(t, sbp, 1)[0]) if len(sbp) > 1 else 0.0

        return {
            "sbp_mean": float(sbp.mean()),
            "sbp_std": float(sbp.std()),
            "sbp_slope": slope,
            "dbp_mean": float(dbp.mean()),
            "dbp_std": float(dbp.std()),
            "night_bp_pattern": pattern,
        }

    def extract_cardiac_features(
        self, hr_series: List[float], hrv_series: List[float]
    ) -> dict:
        expected_hr = self.window_days * 24
        hr = np.array(hr_series[-expected_hr:], dtype=float)
        hrv = np.array(hrv_series[-self.window_days :], dtype=float)

        return {
            "resting_hr_mean": float(hr.mean()) if len(hr) > 0 else 0.0,
            "hrv_rmssd_mean": float(hrv.mean()) if len(hrv) > 0 else 0.0,
        }

    def extract_activity_features(
        self,
        calories_daily: List[float],
        sport_events: List[dict],
        sleep_hours: List[float],
    ) -> dict:
        cal = np.array(calories_daily[-self.window_days :], dtype=float)
        slp = np.array(sleep_hours[-self.window_days :], dtype=float)

        cal_trend = 0.0
        if len(cal) > 1:
            cal_trend = float(np.polyfit(np.arange(len(cal)), cal, 1)[0])

        weeks = max(self.window_days / 7.0, 1.0)
        sport_freq = len(sport_events) / weeks

        return {
            "active_calories_daily_mean": float(cal.mean()) if len(cal) > 0 else 0.0,
            "active_calories_trend": cal_trend,
            "sport_freq_weekly": round(sport_freq, 2),
            "sleep_hours_mean": float(slp.mean()) if len(slp) > 0 else 0.0,
        }

    @staticmethod
    def encode_time_confounders(dt: Optional[datetime] = None) -> dict:
        dt = dt or datetime.now()
        day_of_year = dt.timetuple().tm_yday
        angle = 2 * np.pi * day_of_year / 365.25
        return {
            "season_sin": round(float(np.sin(angle)), 6),
            "season_cos": round(float(np.cos(angle)), 6),
            "day_of_week": dt.weekday(),
            "is_weekend": int(dt.weekday() >= 5),
        }

    def extract_all(
        self,
        sbp_hourly: List[float],
        dbp_hourly: List[float],
        hr_series: List[float],
        hrv_series: List[float],
        calories_daily: List[float],
        sport_events: List[dict],
        sleep_hours: List[float],
    ) -> dict:
        features = {}
        features.update(self.extract_bp_features(sbp_hourly, dbp_hourly))
        features.update(self.extract_cardiac_features(hr_series, hrv_series))
        features.update(
            self.extract_activity_features(calories_daily, sport_events, sleep_hours)
        )
        features.update(self.encode_time_confounders())
        return features
