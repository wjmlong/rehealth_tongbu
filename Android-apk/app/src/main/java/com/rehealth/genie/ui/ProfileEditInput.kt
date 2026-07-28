package com.rehealth.genie.ui

internal data class ValidatedProfileEditInput(
    val name: String,
    val gender: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
)

internal fun normalizeProfileGender(value: String?): String? = when (value?.trim()?.lowercase()) {
    "male", "man", "m", "男" -> "male"
    "female", "woman", "f", "女" -> "female"
    else -> null
}

internal fun validateProfileEditInput(
    name: String,
    gender: String?,
    age: String,
    heightCm: String,
    weightKg: String,
): Result<ValidatedProfileEditInput> = runCatching {
    val normalizedName = name.trim().take(32)
    require(normalizedName.isNotBlank()) { "请输入姓名/昵称" }
    val normalizedGender = normalizeProfileGender(gender)
    requireNotNull(normalizedGender) { "请选择性别" }
    val normalizedAge = age.trim().toIntOrNull()
    require(normalizedAge != null && normalizedAge in 1..120) { "请输入 1–120 岁的有效年龄" }
    val normalizedHeight = heightCm.trim().toDoubleOrNull()
    require(normalizedHeight != null && normalizedHeight in 50.0..250.0) { "请输入 50–250 cm 的有效身高" }
    val normalizedWeight = weightKg.trim().toDoubleOrNull()
    require(normalizedWeight != null && normalizedWeight in 10.0..300.0) { "请输入 10–300 kg 的有效体重" }
    ValidatedProfileEditInput(
        name = normalizedName,
        gender = normalizedGender,
        age = normalizedAge,
        heightCm = normalizedHeight,
        weightKg = normalizedWeight,
    )
}
