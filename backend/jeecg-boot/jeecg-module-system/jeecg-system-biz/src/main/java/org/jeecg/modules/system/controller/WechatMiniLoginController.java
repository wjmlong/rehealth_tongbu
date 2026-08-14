package org.jeecg.modules.system.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.system.service.IWechatMiniLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信小程序登录接口
 *
 * 路径对齐 Android 端预留契约：POST /rehealth/mobile/wechat/login
 * 请求体：{ "code": "wx.login 返回的临时凭证" }
 * 响应：Result<JSONObject>，result 含 { success, token, userInfo }
 */
@RestController
@RequestMapping("/rehealth/mobile/wechat")
@Tag(name = "微信小程序登录")
@Slf4j
public class WechatMiniLoginController {

    @Autowired
    private IWechatMiniLoginService wechatMiniLoginService;

    @IgnoreAuth
    @PostMapping("/login")
    @Operation(summary = "微信小程序 code 登录")
    public Result<JSONObject> login(@RequestBody JSONObject body) {
        String code = body == null ? null : body.getString("code");
        JSONObject data = wechatMiniLoginService.login(code);
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
}
