package org.jeecg.modules.system.service;

import com.alibaba.fastjson.JSONObject;

//update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用登录服务接口-----------
/**
 * 微信开放平台移动应用登录服务（App 侧微信登录）。
 *
 * 链路：微信 SDK 授权 code -> sns/oauth2/access_token -> openid
 * -> sys_third_account(thirdType="wechat_app") 登录或自动注册 -> 签发 APP token。
 */
public interface IWechatAppLoginService {

    /**
     * 用微信 SDK 返回的一次性 code 完成登录或自动注册。
     *
     * @param code 微信授权 code（一次性，5 分钟有效）
     * @return { success, message?, token?, userInfo? }
     */
    JSONObject login(String code);
}
//update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用登录服务接口-----------
