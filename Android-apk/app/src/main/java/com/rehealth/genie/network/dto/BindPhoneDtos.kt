package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * 强制绑定手机号请求体（需登录）：
 *   POST /rehealth/mobile/account/bind-phone/sms  -> [BindPhoneSmsRequest]
 *   POST /rehealth/mobile/account/bind-phone      -> [BindPhoneRequest]
 */
@JsonClass(generateAdapter = true)
data class BindPhoneSmsRequest(
    val phone: String,
)

@JsonClass(generateAdapter = true)
data class BindPhoneRequest(
    val phone: String,
    val smsCode: String,
)

/** 绑定成功响应 result 载荷：{ userInfo }。 */
@JsonClass(generateAdapter = true)
data class BindPhoneResponse(
    val userInfo: LoginUserInfo? = null,
)
