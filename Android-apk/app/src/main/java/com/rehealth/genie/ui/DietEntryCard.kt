package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.diet.DietMealType
import com.rehealth.genie.diet.DietRecordDraft
import com.rehealth.genie.diet.DietRecordWithUploadState
import com.rehealth.genie.ui.theme.AttributionPalette as Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AttributionDietCard(
    state: DietEntryUiState,
    onSave: (DietRecordDraft) -> Unit,
    onClearMessage: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("今日餐食记录", color = Palette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (state.records.isEmpty()) "先保存到本机，再由离线上传队列同步" else todaySummary(state.records),
                        color = Palette.TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                TextButton(
                    onClick = {
                        onClearMessage()
                        showDialog = true
                    },
                ) {
                    Text("记录餐食", color = Palette.Accent)
                }
            }

            state.message?.let { message ->
                Text(
                    message,
                    color = if (state.isError) Color(0xFFC64B45) else Palette.Accent,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            if (state.records.isEmpty()) {
                Text(
                    "今天还没有餐食记录。录入餐次、内容和热量后，个性化干预会优先参考今天的行为。",
                    color = Palette.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        .background(Palette.SurfaceSubtle).padding(12.dp),
                )
            } else {
                state.records.take(MAX_VISIBLE_RECORDS).forEach { item ->
                    DietRecordRow(item)
                }
            }
        }
    }

    if (showDialog) {
        DietEntryDialog(
            saving = state.saving,
            onDismiss = { if (!state.saving) showDialog = false },
            onSave = { draft ->
                onSave(draft)
                showDialog = false
            },
        )
    }
}

@Composable
private fun DietRecordRow(item: DietRecordWithUploadState) {
    val record = item.record
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .background(Palette.SurfaceSubtle).padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${mealTypeLabel(record.mealType)} · ${formatTime(record.consumedAt)}",
                color = Palette.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(uploadLabel(item.uploadStatus), color = Palette.Accent, fontSize = 9.sp)
        }
        Text(
            record.description,
            color = Palette.TextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        val macro = listOfNotNull(
            record.proteinGrams?.let { "蛋白质 ${formatNumber(it)} g" },
            record.carbohydrateGrams?.let { "碳水 ${formatNumber(it)} g" },
            record.fatGrams?.let { "脂肪 ${formatNumber(it)} g" },
        ).joinToString(" · ")
        Text(
            buildString {
                append("${formatNumber(record.caloriesKcal)} kcal")
                if (macro.isNotBlank()) append(" · $macro")
            },
            color = Palette.TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DietEntryDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (DietRecordDraft) -> Unit,
) {
    var mealType by remember { mutableStateOf(DietMealType.LUNCH) }
    var description by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbohydrate by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录今天的餐食") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DietMealType.entries.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(mealTypeLabel(type.wireValue), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 256) description = it },
                    label = { Text("餐食内容") },
                    placeholder = { Text("例如：红烧牛肉面 + 鸡蛋 + 无糖茶") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = numericInput(it) },
                    label = { Text("热量 kcal（必填）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroField("蛋白质 g", protein, { protein = numericInput(it) }, Modifier.weight(1f))
                    MacroField("碳水 g", carbohydrate, { carbohydrate = numericInput(it) }, Modifier.weight(1f))
                    MacroField("脂肪 g", fat, { fat = numericInput(it) }, Modifier.weight(1f))
                }
                Text("记录时间为当前时间；营养素可留空。", color = Palette.TextSecondary, fontSize = 9.sp)
                validationError?.let {
                    Text(it, color = Color(0xFFC64B45), fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent),
                onClick = {
                    val kcal = calories.toDoubleOrNull()
                    validationError = when {
                        description.trim().isEmpty() -> "请输入餐食内容。"
                        kcal == null || kcal <= 0.0 -> "请输入大于 0 的热量。"
                        else -> null
                    }
                    if (validationError == null && kcal != null) {
                        onSave(
                            DietRecordDraft(
                                mealType = mealType.wireValue,
                                description = description,
                                caloriesKcal = kcal,
                                proteinGrams = protein.toOptionalDouble(),
                                carbohydrateGrams = carbohydrate.toOptionalDouble(),
                                fatGrams = fat.toOptionalDouble(),
                            ),
                        )
                    }
                },
            ) {
                Text(if (saving) "保存中" else "保存并同步")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun MacroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 9.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}

private fun todaySummary(records: List<DietRecordWithUploadState>): String =
    "已记录 ${records.size} 餐 · 共 ${formatNumber(records.sumOf { it.record.caloriesKcal })} kcal"

private fun mealTypeLabel(value: String): String = when (value) {
    DietMealType.BREAKFAST.wireValue -> "早餐"
    DietMealType.LUNCH.wireValue -> "午餐"
    DietMealType.DINNER.wireValue -> "晚餐"
    DietMealType.SNACK.wireValue -> "加餐"
    else -> "餐食"
}

private fun uploadLabel(status: String?): String = when (status) {
    "done" -> "已同步"
    "pending", "uploading" -> "待同步"
    "failed" -> "等待重试"
    "dead_letter" -> "同步失败"
    else -> "已存本机"
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun numericInput(value: String): String = value.filterIndexed { index, char ->
    char.isDigit() || (char == '.' && index > 0 && value.indexOf('.') == index)
}

private fun String.toOptionalDouble(): Double? = takeIf(String::isNotBlank)?.toDoubleOrNull()

private const val MAX_VISIBLE_RECORDS = 6
