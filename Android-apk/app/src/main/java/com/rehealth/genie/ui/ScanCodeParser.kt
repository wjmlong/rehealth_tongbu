package com.rehealth.genie.ui

/**
 * 员工二维码内容解析：
 * - 标准格式 `rehealth://insurance/scan?c=<code>&t=<tenantId>` → 取 c 参数
 * - 兼容纯文本（8 位员工码）
 * 返回 null 表示无法解析。
 */
object ScanCodeParser {
    private val EMPLOYEE_CODE = Regex("[A-Z0-9]{4,16}", RegexOption.IGNORE_CASE)

    fun parseEmployeeCode(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("rehealth://insurance/scan")) {
            val code = value.substringAfter("c=", "").substringBefore('&').trim()
            return code.takeIf { EMPLOYEE_CODE.matches(it) }?.uppercase()
        }
        return value.takeIf { EMPLOYEE_CODE.matches(it) }?.uppercase()
    }
}
