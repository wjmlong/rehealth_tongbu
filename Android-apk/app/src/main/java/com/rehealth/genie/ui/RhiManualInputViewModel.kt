package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.rhi.RhiManualHealthInputEntity
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RhiManualInputUiState(
    val input: RhiManualHealthInputEntity? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedVersion: Long = 0L,
)

data class RhiManualInputDraft(
    val sedentaryHoursPerDay: String,
    val waistCircumferenceCm: String,
    val vo2MaxMlKgMin: String,
    val hba1cPercent: String,
    val egfrMlMin173m2: String,
    val cuffSbp7dMean: String = "",
    val cuffDbp7dMean: String = "",
    val cuffValidDays: String = "",
    val cuffConfirmed: Boolean = false,
    val fastingGlucoseMmolL: String = "",
    val totalCholesterolMmolL: String = "",
    val ldlMmolL: String = "",
    val hdlMmolL: String = "",
    val triglyceridesMmolL: String = "",
    val labConfirmed: Boolean = false,
    val labRecordedDate: String = "",
)

class RhiManualInputViewModel(context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(RhiManualInputUiState())
    val uiState: StateFlow<RhiManualInputUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var observedUserId: String? = null

    fun observeCurrentUser() {
        val userId = app.sessionStore.userId ?: return
        if (userId == observedUserId && observeJob?.isActive == true) return
        observedUserId = userId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            app.database.rhiManualHealthInputDao().observe(userId).collect { input ->
                _uiState.value = _uiState.value.copy(input = input)
            }
        }
    }

    fun save(draft: RhiManualInputDraft) {
        val userId = app.sessionStore.userId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "请登录后再保存健康档案")
            return
        }
        val parsed = validateRhiManualInput(draft).getOrElse { error ->
            _uiState.value = _uiState.value.copy(errorMessage = error.message)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val existing = _uiState.value.input ?: RhiManualHealthInputEntity(
                    userId = userId,
                    updatedAt = System.currentTimeMillis(),
                )
                app.database.rhiManualHealthInputDao().upsert(
                    existing.copy(
                        sedentaryHoursPerDay = parsed.sedentaryHoursPerDay,
                        waistCircumferenceCm = parsed.waistCircumferenceCm,
                        vo2MaxMlKgMin = parsed.vo2MaxMlKgMin,
                        hba1cPercent = parsed.hba1cPercent,
                        egfrMlMin173m2 = parsed.egfrMlMin173m2,
                        cuffSbp7dMean = parsed.cuffSbp7dMean,
                        cuffDbp7dMean = parsed.cuffDbp7dMean,
                        cuffValidDays = parsed.cuffValidDays,
                        cuffConfirmed = parsed.cuffConfirmed,
                        fastingGlucoseMmolL = parsed.fastingGlucoseMmolL,
                        totalCholesterolMmolL = parsed.totalCholesterolMmolL,
                        ldlMmolL = parsed.ldlMmolL,
                        hdlMmolL = parsed.hdlMmolL,
                        triglyceridesMmolL = parsed.triglyceridesMmolL,
                        labConfirmed = parsed.labConfirmed,
                        labRecordedAt = parsed.labRecordedAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = null,
                    savedVersion = _uiState.value.savedVersion + 1,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "本地健康档案保存失败，请重试",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RhiManualInputViewModel(context) as T
    }
}

internal data class ParsedRhiManualInput(
    val sedentaryHoursPerDay: Double?,
    val waistCircumferenceCm: Double?,
    val vo2MaxMlKgMin: Double?,
    val hba1cPercent: Double?,
    val egfrMlMin173m2: Double?,
    val cuffSbp7dMean: Double?,
    val cuffDbp7dMean: Double?,
    val cuffValidDays: Int?,
    val cuffConfirmed: Boolean,
    val fastingGlucoseMmolL: Double?,
    val totalCholesterolMmolL: Double?,
    val ldlMmolL: Double?,
    val hdlMmolL: Double?,
    val triglyceridesMmolL: Double?,
    val labConfirmed: Boolean,
    val labRecordedAt: Long?,
)

internal fun validateRhiManualInput(draft: RhiManualInputDraft): Result<ParsedRhiManualInput> =
    runCatching {
        val cuffSbp = draft.cuffSbp7dMean.optionalDouble("7日平均收缩压", 70.0, 250.0)
        val cuffDbp = draft.cuffDbp7dMean.optionalDouble("7日平均舒张压", 40.0, 150.0)
        val cuffDays = draft.cuffValidDays.optionalInt("袖带有效天数", 3, 7)
        if (draft.cuffConfirmed) {
            require(cuffSbp != null && cuffDbp != null && cuffDays != null) {
                "确认上臂袖带来源前，请填写收缩压、舒张压和 3–7 个有效日"
            }
            require(cuffSbp > cuffDbp) { "收缩压必须高于舒张压" }
        }
        val fastingGlucose = draft.fastingGlucoseMmolL.optionalDouble("空腹血糖", 1.0, 40.0)
        val totalCholesterol = draft.totalCholesterolMmolL.optionalDouble("总胆固醇", 0.5, 30.0)
        val ldl = draft.ldlMmolL.optionalDouble("LDL-C", 0.1, 20.0)
        val hdl = draft.hdlMmolL.optionalDouble("HDL-C", 0.1, 10.0)
        val triglycerides = draft.triglyceridesMmolL.optionalDouble("甘油三酯", 0.1, 50.0)
        val anyLab = listOf(fastingGlucose, totalCholesterol, ldl, hdl, triglycerides).any { it != null }
        val labRecordedAt = draft.labRecordedDate.optionalDateMillis("血检报告日期")
        if (draft.labConfirmed) {
            require(anyLab) { "确认医院报告前，请至少填写一项血检结果" }
            require(labRecordedAt != null) { "确认医院报告前，请填写报告日期（YYYY-MM-DD）" }
        }
        ParsedRhiManualInput(
            sedentaryHoursPerDay = draft.sedentaryHoursPerDay.optionalDouble("日均久坐", 0.0, 24.0),
            waistCircumferenceCm = draft.waistCircumferenceCm.optionalDouble("腰围", 40.0, 200.0),
            vo2MaxMlKgMin = draft.vo2MaxMlKgMin.optionalDouble("VO₂max", 5.0, 100.0),
            hba1cPercent = draft.hba1cPercent.optionalDouble("HbA1c", 3.0, 20.0),
            egfrMlMin173m2 = draft.egfrMlMin173m2.optionalDouble("eGFR", 0.0, 250.0),
            cuffSbp7dMean = cuffSbp,
            cuffDbp7dMean = cuffDbp,
            cuffValidDays = cuffDays,
            cuffConfirmed = draft.cuffConfirmed,
            fastingGlucoseMmolL = fastingGlucose,
            totalCholesterolMmolL = totalCholesterol,
            ldlMmolL = ldl,
            hdlMmolL = hdl,
            triglyceridesMmolL = triglycerides,
            labConfirmed = draft.labConfirmed,
            labRecordedAt = labRecordedAt,
        )
    }

private fun String.optionalDouble(label: String, minimum: Double, maximum: Double): Double? {
    if (isBlank()) return null
    val value = trim().toDoubleOrNull() ?: error("$label 必须填写数字")
    require(value.isFinite() && value in minimum..maximum) {
        "$label 应在 ${minimum.compact()}–${maximum.compact()} 之间"
    }
    return value
}

private fun String.optionalInt(label: String, minimum: Int, maximum: Int): Int? {
    if (isBlank()) return null
    val value = trim().toIntOrNull() ?: error("$label 必须填写整数")
    require(value in minimum..maximum) { "$label 应在 $minimum–$maximum 之间" }
    return value
}

private fun String.optionalDateMillis(label: String): Long? {
    if (isBlank()) return null
    val date = runCatching { LocalDate.parse(trim()) }.getOrNull()
        ?: error("$label 格式应为 YYYY-MM-DD")
    require(!date.isAfter(LocalDate.now())) { "$label 不能晚于今天" }
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
