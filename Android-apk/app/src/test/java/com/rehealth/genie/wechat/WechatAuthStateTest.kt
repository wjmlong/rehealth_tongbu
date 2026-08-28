package com.rehealth.genie.wechat

import org.junit.Assert.assertEquals
import org.junit.Test

class WechatAuthStateTest {

    @Test
    fun `authorized response with code maps to Authorized`() {
        assertEquals(
            WechatAuthState.Authorized("code-1"),
            WechatAuthState.fromSendAuthResp(WechatAuthState.ERR_OK, "code-1"),
        )
    }

    @Test
    fun `ok response without code maps to Failed`() {
        assertEquals(
            WechatAuthState.Failed(WechatAuthState.ERR_OK),
            WechatAuthState.fromSendAuthResp(WechatAuthState.ERR_OK, null),
        )
    }

    @Test
    fun `user cancel maps to Canceled`() {
        assertEquals(
            WechatAuthState.Canceled,
            WechatAuthState.fromSendAuthResp(WechatAuthState.ERR_USER_CANCEL, null),
        )
    }

    @Test
    fun `other error codes map to Failed with the code`() {
        assertEquals(
            WechatAuthState.Failed(-4),
            WechatAuthState.fromSendAuthResp(-4, null),
        )
    }
}
