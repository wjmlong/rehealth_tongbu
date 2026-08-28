package com.rehealth.genie.wxapi

import android.app.Activity
import android.os.Bundle
import com.rehealth.genie.wechat.WechatAuthService
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

/**
 * 微信 SDK 回调入口。类名与包名由微信客户端约定固定：
 * `{applicationId}.wxapi.WXEntryActivity`（即 com.rehealth.genie.wxapi.WXEntryActivity）。
 *
 * 只负责把回调转给 [WechatAuthService]，自身无业务逻辑。
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WechatAuthService.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WechatAuthService.handleIntent(intent, this)
    }

    override fun onResp(resp: BaseResp) {
        WechatAuthService.onRespReceived(resp)
        finish()
    }

    override fun onReq(req: BaseReq) {
        WechatAuthService.onReqReceived(req)
        finish()
    }
}
