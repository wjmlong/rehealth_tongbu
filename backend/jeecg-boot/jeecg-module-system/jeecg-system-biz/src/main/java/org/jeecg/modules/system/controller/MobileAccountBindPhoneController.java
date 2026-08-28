package org.jeecg.modules.system.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.IpUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.service.IMobileAccountBindPhoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

//update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号接口-----------
/**
 * 移动端账号绑定手机号接口（微信新建账号强制绑定）。
 *
 * 两个接口都需要登录（Shiro 默认 jwt 链保护 /rehealth/mobile/account/**）：
 *   POST /rehealth/mobile/account/bind-phone/sms   { "phone": "..." }
 *   POST /rehealth/mobile/account/bind-phone       { "phone": "...", "smsCode": "..." }
 */
@RestController
@RequestMapping("/rehealth/mobile/account")
@Tag(name = "移动端账号绑定")
@Slf4j
public class MobileAccountBindPhoneController {

    @Autowired
    private IMobileAccountBindPhoneService bindPhoneService;

    @PostMapping("/bind-phone/sms")
    @Operation(summary = "发送绑定手机号验证码")
    public Result<String> bindPhoneSms(@RequestBody JSONObject body, HttpServletRequest request) {
        String phone = body == null ? null : body.getString("phone");
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null || oConvertUtils.isEmpty(loginUser.getId())) {
            return Result.error("请先登录");
        }
        String clientIp = IpUtils.getIpAddr(request);
        return bindPhoneService.sendCode(loginUser.getId(), phone, clientIp);
    }

    @PostMapping("/bind-phone")
    @Operation(summary = "校验验证码并绑定手机号")
    public Result<JSONObject> bindPhone(@RequestBody JSONObject body) {
        String phone = body == null ? null : body.getString("phone");
        String smsCode = body == null ? null : body.getString("smsCode");
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null || oConvertUtils.isEmpty(loginUser.getId())) {
            return Result.error("请先登录");
        }
        return bindPhoneService.bind(loginUser.getId(), phone, smsCode);
    }

    private LoginUser currentLoginUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        return principal instanceof LoginUser ? (LoginUser) principal : null;
    }
}
//update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号接口-----------
