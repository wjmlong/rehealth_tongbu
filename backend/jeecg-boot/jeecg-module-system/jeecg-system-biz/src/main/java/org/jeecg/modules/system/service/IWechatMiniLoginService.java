package org.jeecg.modules.system.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 微信小程序登录服务
 *
 * 通过 wx.login 返回的 code 调用微信 jscode2session 换取 openid，
 * 再复用 JeecgBoot 的第三方账号体系（sys_third_account）完成登录或自动注册，
 * 最后签发 APP 客户端类型 token。
 */
public interface IWechatMiniLoginService {

    /**
     * 微信小程序登录
     *
     * @param code wx.login 返回的临时登录凭证
     * @return { token, userInfo } 或包含错误信息的对象
     */
    JSONObject login(String code);
}
