package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * Request body for JeecgBoot app login (`POST /jeecg-boot/sys/mLogin`).
 *
 * Verified against the live backend (`LoginController.mLogin` → `SysLoginModel`):
 * the endpoint reads `username` + `password` (password may be AES-encrypted or
 * plaintext; the server tries AES first and falls back to plaintext). It does NOT
 * validate a graphic captcha. `loginOrgCode` is optional (department selection).
 *
 * (The `mobile` + `captcha` SMS flow lives on a different endpoint, `/sys/phoneLogin`,
 * which additionally requires a real SMS code — not used here.)
 */
@JsonClass(generateAdapter = true)
data class MobileLoginRequest(
    val username: String,
    val password: String,
    val loginOrgCode: String? = null,
)

/** `result` payload of a successful `/sys/mLogin` response. */
@JsonClass(generateAdapter = true)
data class MobileLoginResponse(
    val token: String? = null,
    val userInfo: LoginUserInfo? = null,
)

@JsonClass(generateAdapter = true)
data class LoginUserInfo(
    val id: String? = null,
    val username: String? = null,
    val realname: String? = null,
)
