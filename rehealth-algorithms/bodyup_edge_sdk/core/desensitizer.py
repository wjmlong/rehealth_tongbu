class HealthDesensitizer:

    AGE_BRACKETS = [
        (0, 30, 1), (30, 40, 2), (40, 50, 3),
        (50, 60, 4), (60, 70, 5), (70, 120, 6),
    ]
    BMI_LEVELS = [
        (0, 18.5, "underweight"), (18.5, 24, "normal"),
        (24, 28, "overweight"), (28, 100, "obese"),
    ]
    ACTIVITY_LEVELS = [
        (0, 100, "sedentary"), (100, 200, "low"),
        (200, 350, "moderate"), (350, 9999, "active"),
    ]
    BP_GRADES = [
        (0, 120, "normal"), (120, 130, "elevated"),
        (130, 140, "stage1"), (140, 180, "stage2"), (180, 999, "crisis"),
    ]

    def desensitize(self, memory: dict) -> dict:
        sb = memory.get("static_baseline", {})
        dv = memory.get("dynamic_memory_vectors", {})
        ic = memory.get("intervention_compliance", {})

        age_bracket = self._bucket(sb.get("age"), self.AGE_BRACKETS, default=0)
        bmi_level = self._bucket_label(sb.get("bmi"), self.BMI_LEVELS, default="unknown")
        activity_level = self._bucket_label(
            dv.get("active_calories_daily_mean"), self.ACTIVITY_LEVELS, default="unknown"
        )
        bp_grade = self._bucket_label(
            dv.get("sbp_mean"), self.BP_GRADES, default="unknown"
        )

        return {
            "device_id": memory.get("device_id"),
            "disease_type": memory.get("disease_type", "CVD"),
            "memory_snapshot": {
                "age_bracket": age_bracket,
                "bmi_level": bmi_level,
                "bp_variability": dv.get("sbp_std"),
                "bp_baseline_grade": bp_grade,
                "night_bp_pattern": dv.get("night_bp_pattern"),
                "activity_level": activity_level,
                "sleep_quality_index": dv.get("sleep_hours_mean"),
                "resting_hr_mean": dv.get("resting_hr_mean"),
                "hrv_rmssd_mean": dv.get("hrv_rmssd_mean"),
                "intervention_compliance": ic.get("diet_compliance_rate"),
            },
            "attribution_timeseries": memory.get("risk_score_history", []),
        }

    @staticmethod
    def _bucket(value, brackets, default=None):
        if value is None:
            return default
        for lo, hi, label in brackets:
            if lo <= value < hi:
                return label
        return default

    @staticmethod
    def _bucket_label(value, brackets, default="unknown"):
        if value is None:
            return default
        for lo, hi, label in brackets:
            if lo <= value < hi:
                return label
        return default
