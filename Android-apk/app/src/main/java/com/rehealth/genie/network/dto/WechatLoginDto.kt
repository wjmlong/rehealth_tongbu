package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * 微信移动应用登录请求（POST /jeecg-boot/rehealth/mobile/wechat/app-login）。
 * code 由微信 SDK SendAuth.Resp 返回，一次性、5 分钟有效，只提交一次。
 */
@JsonClass(generateAdapter = true)
data class WechatAppLoginRequest(
    val code: String,
)
