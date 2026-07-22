package com.rehealth.genie.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AttributionPalette {
    val Page = Canvas
    val Surface = Color.White
    val SurfaceSubtle = Color(0xFFF7FBFA)
    val SurfaceMetric = Color(0xFFF8FBFA)
    val SurfaceWarning = Color(0xFFFFF4E4)
    val ActivitySurface = Color(0xFFFFF8EE)
    val ActivityBadge = Color(0xFFFFE7C6)
    val TextPrimary = Ink
    val TextSecondary = Muted
    val Border = Line
    val Accent = Mint
    val AccentSoft = MintSoft
    val Transparent = Color.Transparent
    val OnAccent = Color.White
    val ImprovementWorsening = Color(0xFFE36B61)
    val ContributionRisk = Color(0xFFE39A22)
    val ActivityAccent = Color(0xFFE88625)
    val ForecastNoAction = Color(0xFFF28B82)
    val ForecastReduction = Color(0xFF4E7BFF)
    val ForecastInterval = Color(0xFF9BAFAA)
    val ChartGrid = Color(0xFFE7F0ED)
    val FactorTrack = Color(0xFFF0F5F3)
}

internal object AttributionTypography {
    val PageTitle = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold)
    val PageSubtitle = TextStyle(fontSize = 12.sp)
    val CardTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val Selector = TextStyle(fontSize = 12.sp)
    val Body = TextStyle(fontSize = 11.sp, lineHeight = 17.sp)
    val Detail = TextStyle(fontSize = 10.sp, lineHeight = 15.sp)
    val Micro = TextStyle(fontSize = 9.sp, lineHeight = 14.sp)
    val MetricLabel = TextStyle(fontSize = 8.sp)
    val SummaryScore = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Bold)
    val RiskScore = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val ActivityGlyph = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val FactorTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val FactorScore = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val ForecastMetric = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val ButtonLabel = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

internal object AttributionDimensions {
    val ScreenPadding = 18.dp
    val SectionGap = 12.dp
    val PageSubtitleTop = 4.dp

    val DisclaimerRadius = 14.dp
    val DisclaimerPadding = 14.dp
    val DisclaimerIcon = 18.dp
    val DisclaimerIconGap = 8.dp

    val SelectorRadius = 16.dp
    val SelectorPadding = 4.dp
    val SelectorGap = 4.dp
    val SelectorItemRadius = 12.dp
    val SelectorItemVerticalPadding = 9.dp

    val BannerRadius = 13.dp
    val BannerHorizontalPadding = 12.dp
    val BannerVerticalPadding = 9.dp
    val BannerSpinner = 16.dp
    val BannerSpinnerStroke = 2.dp
    val BannerSpinnerGap = 8.dp

    val SummaryScoreTop = 6.dp
    val SummarySupportingTop = 3.dp
    val HistoryChartHeight = 76.dp
    val HistoryChartTop = 10.dp
    val HistoryEmptyHeight = 60.dp
    val ContentRadius = 12.dp

    val StatusHorizontalPadding = 9.dp
    val StatusVerticalPadding = 5.dp
    val AccumulationProgressHeight = 6.dp

    val ForecastChartHeight = 150.dp
    val ForecastChartTop = 12.dp
    val LegendTop = 8.dp
    val LegendGap = 14.dp
    val ForecastMetricsTop = 14.dp
    val ForecastMetricGap = 8.dp
    val AttTop = 11.dp

    val ActivityContentTop = 12.dp
    val ActivityContentRadius = 14.dp
    val ActivityContentPadding = 12.dp
    val ActivityBadgeSize = 42.dp
    val ActivityTextGap = 10.dp
    val ActivitySupportingTop = 3.dp
    val ActivityMetricsTop = 10.dp
    val ActivityMetricGap = 8.dp

    val FactorGroupTop = 14.dp
    val FactorRowTop = 12.dp
    val FactorRankSize = 28.dp
    val FactorContentGap = 10.dp
    val FactorSupportingTop = 3.dp
    val FactorIndent = 38.dp
    val FactorBarTop = 8.dp
    val FactorBarHeight = 5.dp
    val FactorDetailTop = 8.dp
    val FactorDetailPadding = 10.dp
    val FactorEvidenceTop = 5.dp
    val FactorDividerTop = 12.dp

    val PlanFeedbackTop = 8.dp
    val PlanButtonTop = 12.dp
    val PlanButtonHeight = 46.dp
    val PlanButtonRadius = 14.dp

    val InterventionTop = 12.dp
    val InterventionRankSize = 27.dp
    val InterventionContentGap = 9.dp
    val InterventionSupportingTop = 3.dp
    val InterventionActionIndent = 36.dp
    val InterventionActionsTop = 8.dp
    val InterventionActionGap = 6.dp
    val InterventionDividerTop = 10.dp

    val FeedbackButtonHeight = 32.dp
    val FeedbackButtonHorizontalPadding = 5.dp
    val FeedbackIconSize = 12.dp
    val FeedbackIconGap = 3.dp

    val CardRadius = 18.dp
    val CardBorder = 1.dp
    val CardPadding = 16.dp

    val MessageTop = 12.dp
    val MessagePadding = 11.dp
    val MessageSpinner = 17.dp
    val MessageSpinnerStroke = 2.dp
    val MessageSpinnerGap = 9.dp

    val ActivityMetricRadius = 10.dp
    val ActivityMetricVerticalPadding = 7.dp
    val ActivityMetricLabelTop = 2.dp

    val LegendDot = 7.dp
    val LegendLabelGap = 4.dp
    val ForecastMetricHorizontalPadding = 9.dp
    val ForecastMetricVerticalPadding = 8.dp
    val ForecastMetricValueTop = 3.dp

    val HistoryChartInset = 5.dp
    val HistoryChartDotRadius = 3.dp
    val HistoryChartStroke = 2.dp
    val ForecastChartInset = 8.dp
    val ForecastGridStroke = 1.dp
    val ForecastDotRadius = 2.6.dp
    val ForecastNoActionStroke = 2.dp
    val ForecastPlanStroke = 2.5.dp
}

internal object AttributionMotion {
    const val ProgressMillis = 650
}

internal object AttributionOpacity {
    const val MetricTint = 0.08f
    const val ForecastInterval = 0.16f
}
