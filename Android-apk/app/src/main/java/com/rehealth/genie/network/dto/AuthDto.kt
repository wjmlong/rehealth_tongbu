package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /jeecg-boot/sys/registerSms`.
 *
 * Matches `LoginController.sms(@RequestBody JSONObject)`: reads `mobile` + `smsmode`.
 * `smsmode = "1"` selects registration. Production delegates six-digit code generation,
 * delivery, and verification to Aliyun Dypnsapi; Redis stores only opaque session/rate state.
 */
@JsonClass(generateAdapter = true)
data class SendSmsRequest(
    val mobile: String,
    val smsmode: String = "1",
)

/**
 * Request body for `POST /jeecg-boot/sys/user/register` (public, no sign required).
 *
 * Matches `SysUserController.userRegister(@RequestBody JSONObject)`: reads `phone`,
 * `smscode`, `username` (defaults to phone when blank), `password`, `realname`, `email`.
 * Production accepts `smscode` only when Aliyun `CheckSmsVerifyCode` returns `VerifyResult=PASS`.
 */
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val phone: String,
    val smscode: String,
    val username: String,
    val password: String,
    val realname: String? = null,
    val email: String? = null,
)
