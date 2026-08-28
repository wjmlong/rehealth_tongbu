package org.jeecg.modules.system.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.system.service.IWechatAppLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信开放平台移动应用登录接口（App 侧微信登录）
 *
 * 路径：POST /rehealth/mobile/wechat/app-login
 * 请求体：{ "code": "微信 SDK SendAuth.Resp 返回的一次性凭证" }
 * 响应：Result<JSONObject>，result 含 { token, userInfo }，与 /sys/mLogin 同构
 *
 * AppSecret 仅存服务端；Shiro 已对该路径放行（anon），接口自身无敏感参数。
 */
@RestController
@RequestMapping("/rehealth/mobile/wechat")
@Tag(name = "微信 App 登录")
@Slf4j
public class WechatAppLoginController {

    //update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用 code 登录-----------
    @Autowired
    private IWechatAppLoginService wechatAppLoginService;

    @IgnoreAuth
    @PostMapping("/app-login")
    @Operation(summary = "微信移动应用 code 登录")
    public Result<JSONObject> appLogin(@RequestBody JSONObject body) {
        String code = body == null ? null : body.getString("code");
        JSONObject data = wechatAppLoginService.login(code);
        if (data == null || Boolean.FALSE.equals(data.getBoolean("success"))) {
            String message = data == null ? "登录失败" : data.getString("message");
            return Result.error(message == null ? "登录失败" : message);
        }
        Result<JSONObject> result = new Result<>();
        result.setSuccess(true);
        result.setResult(data);
        result.setCode(200);
        return result;
    }
    //update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用 code 登录-----------
}
