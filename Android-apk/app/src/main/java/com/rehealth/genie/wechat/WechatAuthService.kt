package com.rehealth.genie.wechat

import android.content.Context
import android.content.Intent
import com.rehealth.genie.BuildConfig
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 微信开放平台移动应用授权封装。
 *
 * - AppID 来自 BuildConfig.WECHAT_APP_ID（非敏感）；AppSecret 只在 JeecgBoot 服务端；
 * - 授权结果经 [authState] 广播，由 WXEntryActivity 回调后 emit；
 * - AppID 为空或未安装微信时直接发 [WechatAuthState.NotInstalled]，失败关闭。
 */
object WechatAuthService {

    private const val AUTH_SCOPE = "snsapi_userinfo"
    private const val AUTH_STATE = "rehealth_login"

    private val _authState = MutableSharedFlow<WechatAuthState>(extraBufferCapacity = 1)
    val authState = _authState.asSharedFlow()

    @Volatile
    private var api: IWXAPI? = null

    fun init(context: Context) {
        if (api == null) {
            synchronized(this) {
                if (api == null) {
                    val appContext = context.applicationContext
                    api = if (BuildConfig.WECHAT_APP_ID.isBlank()) {
                        null
                    } else {
                        WXAPIFactory.createWXAPI(appContext, BuildConfig.WECHAT_APP_ID, false).apply {
                            registerApp(BuildConfig.WECHAT_APP_ID)
                        }
                    }
                }
            }
        }
    }

    fun isWxInstalled(context: Context): Boolean {
        init(context)
        return api?.isWXAppInstalled == true
    }

    /** 拉起微信授权。调用方通过 [authState] 观察结果。 */
    fun startAuth(context: Context): WechatAuthState {
        init(context)
        val wxApi = api
        if (wxApi == null || !wxApi.isWXAppInstalled) {
            val state = WechatAuthState.NotInstalled
            _authState.tryEmit(state)
            return state
        }
        val request = SendAuth.Req().apply {
            scope = AUTH_SCOPE
            state = AUTH_STATE
        }
        if (!wxApi.sendReq(request)) {
            val state = WechatAuthState.Failed(WechatAuthState.ERR_OK)
            _authState.tryEmit(state)
            return state
        }
        val state = WechatAuthState.Launching
        _authState.tryEmit(state)
        return state
    }

    /** WXEntryActivity 转发微信回调。 */
    fun handleIntent(intent: Intent?, context: Context) {
        init(context)
        api?.handleIntent(intent, null)
    }

    /** WXEntryActivity 收到 SDK 响应后回传（req 透传便于校验 state）。 */
    fun onRespReceived(resp: BaseResp) {
        if (resp is SendAuth.Resp) {
            _authState.tryEmit(WechatAuthState.fromSendAuthResp(resp.errCode, resp.code))
        }
    }

    fun onReqReceived(req: BaseReq) {
        if (req.type == ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX) {
            // 未接入微信消息能力，无需处理
        }
    }
}
