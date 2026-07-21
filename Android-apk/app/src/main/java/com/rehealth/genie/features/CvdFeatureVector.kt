package com.rehealth.genie.features

data class CvdFeatureVector(
    val age: Int?,
    val gender: Int?,
    val bmi: Double?,
    val sbp: Double?,
    val dbp: Double?,
    val fastingGlucose: Double?,
    val totalCholesterol: Double?,
    val ldl: Double?,
    val hdl: Double?,
    val triglycerides: Double?,
    val exerciseDays: Int?,
    val smoking: Int?,
    val drinking: Int?,
    val diabetesHistory: Int?,
    val hypertensionHistory: Int?,
    val familyHistory: Int?,
    val featureQuality: Map<String, FeatureQuality>,
) {
    val missingFields: List<String>
        get() = CvdFeatureFields.ALL.filter { featureQuality[it]?.status == FeatureQualityStatus.MISSING }

    fun asModelInput(): Map<String, Any?> =
        mapOf(
            CvdFeatureFields.AGE to age,
            CvdFeatureFields.GENDER to gender,
            CvdFeatureFields.BMI to bmi,
            CvdFeatureFields.SBP to sbp,
            CvdFeatureFields.DBP to dbp,
            CvdFeatureFields.FASTING_GLUCOSE to fastingGlucose,
            CvdFeatureFields.TOTAL_CHOLESTEROL to totalCholesterol,
            CvdFeatureFields.LDL to ldl,
            CvdFeatureFields.HDL to hdl,
            CvdFeatureFields.TRIGLYCERIDES to triglycerides,
            CvdFeatureFields.EXERCISE_DAYS to exerciseDays,
            CvdFeatureFields.SMOKING to smoking,
            CvdFeatureFields.DRINKING to drinking,
            CvdFeatureFields.DIABETES_HISTORY to diabetesHistory,
            CvdFeatureFields.HYPERTENSION_HISTORY to hypertensionHistory,
            CvdFeatureFields.FAMILY_HISTORY to familyHistory,
        )
}

object CvdFeatureFields {
    const val AGE = "age"
    const val GENDER = "gender"
    const val BMI = "bmi"
    const val SBP = "sbp"
    const val DBP = "dbp"
    const val FASTING_GLUCOSE = "fasting_glucose"
    const val TOTAL_CHOLESTEROL = "total_cholesterol"
    const val LDL = "ldl"
    const val HDL = "hdl"
    const val TRIGLYCERIDES = "triglycerides"
    const val EXERCISE_DAYS = "exercise_days"
    const val SMOKING = "smoking"
    const val DRINKING = "drinking"
    const val DIABETES_HISTORY = "diabetes_history"
    const val HYPERTENSION_HISTORY = "hypertension_history"
    const val FAMILY_HISTORY = "family_history"

    val ALL = listOf(
        AGE,
        GENDER,
        BMI,
        SBP,
        DBP,
        FASTING_GLUCOSE,
        TOTAL_CHOLESTEROL,
        LDL,
        HDL,
        TRIGLYCERIDES,
        EXERCISE_DAYS,
        SMOKING,
        DRINKING,
        DIABETES_HISTORY,
        HYPERTENSION_HISTORY,
        FAMILY_HISTORY,
    )
}
