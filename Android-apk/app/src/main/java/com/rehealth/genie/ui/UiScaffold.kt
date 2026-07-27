package com.rehealth.genie.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun Page(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
        item { content() }
    }
}

internal fun formatSyncTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

/** Real sleep duration from the latest sleep session (deep+light+rem+awake minutes). Computed, never canned. */
internal fun formatSleepMinutes(entity: RingSleepSessionEntity?): String {
    if (entity == null) return "--"
    val total = entity.deepMinutes + entity.lightMinutes + entity.remMinutes + entity.awakeMinutes
    if (total <= 0) return "--"
    return "${total / 60}h${total % 60}m"
}

/** Real step total from today's STEPS measurement, formatted with thousands separator. */
internal fun formatSteps(value: Double?): String =
    if (value == null) "--" else String.format("%,d", value.roundToInt())

@Composable
internal fun SectionTitle(text: String) {
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun ReHealthCardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
