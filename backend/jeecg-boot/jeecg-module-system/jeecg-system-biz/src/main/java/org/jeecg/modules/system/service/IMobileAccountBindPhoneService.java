package org.jeecg.modules.system.service;

import org.jeecg.common.api.vo.Result;

//update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号服务接口-----------
/**
 * 移动端账号绑定手机号服务（微信新建账号强制绑定）。
 *
 * 复用注册短信基础设施：Redis 频控/会话/锁 + 阿里云 Dypnsapi 校验，
 * 只写当前登录用户 sys_user.phone，不修改用户名/密码。
 */
public interface IMobileAccountBindPhoneService {

    /** 发送绑定验证码。 */
    Result<String> sendCode(String userId, String phone, String clientIp);

    /** 校验验证码并绑定手机号。 */
    Result<com.alibaba.fastjson.JSONObject> bind(String userId, String phone, String smsCode);
}
//update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号服务接口-----------
