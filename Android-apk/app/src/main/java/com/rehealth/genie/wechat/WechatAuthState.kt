package com.rehealth.genie.wechat

/**
 * 微信授权状态机。WXEntryActivity 将 SDK 回调映射为这些状态，
 * 登录页据此发起 code 登录或展示提示。
 */
sealed class WechatAuthState {
    data object Idle : WechatAuthState()

    /** 已拉起微信授权，等待用户操作。 */
    data object Launching : WechatAuthState()

    /** 授权成功，携带一次性 code（5 分钟有效，只消费一次）。 */
    data class Authorized(val code: String) : WechatAuthState()

    /** 用户取消授权，静默返回登录页。 */
    data object Canceled : WechatAuthState()

    /** 授权失败（SDK errCode）。 */
    data class Failed(val errCode: Int) : WechatAuthState()

    /** 未安装微信客户端。 */
    data object NotInstalled : WechatAuthState()

    companion object {
        /** 微信 SDK SendAuth.Resp 的 errCode -> 状态映射（纯函数，便于单测）。 */
        fun fromSendAuthResp(errCode: Int, code: String?): WechatAuthState = when (errCode) {
            ERR_OK -> if (code.isNullOrBlank()) Failed(ERR_OK) else Authorized(code)
            ERR_USER_CANCEL -> Canceled
            else -> Failed(errCode)
        }

        const val ERR_OK = 0
        const val ERR_USER_CANCEL = -2
    }
}
