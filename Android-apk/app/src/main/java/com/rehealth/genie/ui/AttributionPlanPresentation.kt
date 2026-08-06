package com.rehealth.genie.ui

internal fun interventionPlanSubtitle(inputCount: Int): String =
    "围绕 $inputCount 项健康输入安排下一步行动"

internal fun interventionPlanStateLabel(hasPlan: Boolean, expanded: Boolean): String = when {
    !hasPlan -> "待生成"
    expanded -> "已展开"
    else -> "已收起"
}

internal fun interventionPlanToggleLabel(expanded: Boolean): String =
    if (expanded) "收起干预计划" else "展开干预计划"

internal fun interventionPlanActionText(action: String?, timing: String?): String? {
    val actionText = action?.trim()?.takeIf(String::isNotEmpty)
    val timingText = timing?.trim()?.takeIf(String::isNotEmpty)
        ?.takeUnless { value -> actionText?.contains(value) == true }
    return listOfNotNull(actionText, timingText).takeIf(List<String>::isNotEmpty)?.joinToString("；")
}
