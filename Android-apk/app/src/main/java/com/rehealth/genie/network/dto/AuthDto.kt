package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /jeecg-boot/sys/sms` (register mode).
 *
 * Matches `LoginController.sms(@RequestBody JSONObject)`: reads `mobile` + `smsmode`.
 * `smsmode = "1"` selects the register template; the backend sends an SMS containing a
 * 6-digit code that is stored in Redis and later verified by `/sys/user/register`.
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
 * The `smscode` must match the Redis-stored code from `/sys/sms`.
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
