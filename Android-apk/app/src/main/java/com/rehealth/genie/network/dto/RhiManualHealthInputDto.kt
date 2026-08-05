package com.rehealth.genie.network.dto

import com.rehealth.genie.rhi.RhiManualHealthInputEntity

data class RhiManualHealthInputDto(
    val sedentaryHoursPerDay: Double? = null,
    val waistCircumferenceCm: Double? = null,
    val vo2MaxMlKgMin: Double? = null,
    val hba1cPercent: Double? = null,
    val egfrMlMin173m2: Double? = null,
    val cuffSbp7dMean: Double? = null,
    val cuffDbp7dMean: Double? = null,
    val cuffValidDays: Int? = null,
    val cuffConfirmed: Boolean = false,
    val fastingGlucoseMmolL: Double? = null,
    val totalCholesterolMmolL: Double? = null,
    val ldlMmolL: Double? = null,
    val hdlMmolL: Double? = null,
    val triglyceridesMmolL: Double? = null,
    val labConfirmed: Boolean = false,
    val labRecordedAt: Long? = null,
    val updatedAt: Long,
)

fun RhiManualHealthInputEntity.toNetworkDto() = RhiManualHealthInputDto(
    sedentaryHoursPerDay = sedentaryHoursPerDay,
    waistCircumferenceCm = waistCircumferenceCm,
    vo2MaxMlKgMin = vo2MaxMlKgMin,
    hba1cPercent = hba1cPercent,
    egfrMlMin173m2 = egfrMlMin173m2,
    cuffSbp7dMean = cuffSbp7dMean,
    cuffDbp7dMean = cuffDbp7dMean,
    cuffValidDays = cuffValidDays,
    cuffConfirmed = cuffConfirmed,
    fastingGlucoseMmolL = fastingGlucoseMmolL,
    totalCholesterolMmolL = totalCholesterolMmolL,
    ldlMmolL = ldlMmolL,
    hdlMmolL = hdlMmolL,
    triglyceridesMmolL = triglyceridesMmolL,
    labConfirmed = labConfirmed,
    labRecordedAt = labRecordedAt,
    updatedAt = updatedAt,
)

fun RhiManualHealthInputDto.toEntity(userId: String) = RhiManualHealthInputEntity(
    userId = userId,
    sedentaryHoursPerDay = sedentaryHoursPerDay,
    waistCircumferenceCm = waistCircumferenceCm,
    vo2MaxMlKgMin = vo2MaxMlKgMin,
    hba1cPercent = hba1cPercent,
    egfrMlMin173m2 = egfrMlMin173m2,
    cuffSbp7dMean = cuffSbp7dMean,
    cuffDbp7dMean = cuffDbp7dMean,
    cuffValidDays = cuffValidDays,
    cuffConfirmed = cuffConfirmed,
    fastingGlucoseMmolL = fastingGlucoseMmolL,
    totalCholesterolMmolL = totalCholesterolMmolL,
    ldlMmolL = ldlMmolL,
    hdlMmolL = hdlMmolL,
    triglyceridesMmolL = triglyceridesMmolL,
    labConfirmed = labConfirmed,
    labRecordedAt = labRecordedAt,
    updatedAt = updatedAt,
)
