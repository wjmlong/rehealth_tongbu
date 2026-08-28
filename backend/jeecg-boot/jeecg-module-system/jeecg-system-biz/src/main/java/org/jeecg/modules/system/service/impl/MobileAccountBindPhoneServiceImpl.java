package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.sms.SmsVerificationException;
import org.jeecg.common.sms.SmsVerificationService;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.IMobileAccountBindPhoneService;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.system.service.RegistrationSmsState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号实现-----------
/**
 * 移动端账号绑定手机号实现。
 *
 * 发送与校验完整复用注册短信链路（LoginController.registerSms /
 * SysUserController.userRegister）：
 *   - Redis 60s 冷却、手机号/IP 频控、5 分钟会话与绑定锁；
 *   - JEECG_SMS_DEV_MODE=true 时使用固定测试码 123456，不调用短信网关；
 *   - 生产使用阿里云 Dypnsapi 号码认证，VerifyResult=PASS 为唯一通过条件；
 *   - 手机号已被其他账号占用时拒绝，保证手机号与账号一对一。
 *
 * 只写当前登录用户的 sys_user.phone，不改用户名/密码。
 */
@Slf4j
@Service
public class MobileAccountBindPhoneServiceImpl implements IMobileAccountBindPhoneService {

    private static final String SMS_DEV_CODE = "123456";
    private static final int SMS_VALID_SECONDS = 300;
    private static final int SMS_INTERVAL_SECONDS = 60;
    private static final String PHONE_PATTERN = "^1\\d{10}$";

    private final boolean smsDevMode = "true".equalsIgnoreCase(System.getenv("JEECG_SMS_DEV_MODE"));

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private RegistrationSmsState registrationSmsState;

    @Autowired
    private SmsVerificationService smsVerificationService;

    @Override
    public Result<String> sendCode(String userId, String phone, String clientIp) {
        Result<String> result = new Result<>();
        if (oConvertUtils.isEmpty(phone) || !phone.matches(PHONE_PATTERN)) {
            result.error500("请输入正确的手机号");
            return result;
        }

        SysUser owner = sysUserService.getUserByPhone(phone);
        if (owner != null) {
            if (userId.equals(owner.getId())) {
                result.error500("该手机号已绑定当前账号");
            } else {
                result.error500("该手机号已注册，请使用账号密码登录");
            }
            return result;
        }

        if (!registrationSmsState.allowSend(phone, clientIp)) {
            log.warn("绑定手机号短信请求过多，已触发限流");
            result.setMessage("短信接口请求太多，请稍后再试！");
            result.setCode(CommonConstant.PHONE_SMS_FAIL_CODE);
            result.setSuccess(false);
            return result;
        }

        // 开发模式：不真实下发短信，与注册短信同策略
        if (smsDevMode) {
            registrationSmsState.markSent(phone, "dev", null, SMS_VALID_SECONDS, SMS_INTERVAL_SECONDS);
            log.warn("【DEV短信】已为手机号 {} 生成固定测试验证码（未真实下发短信）", maskMobile(phone));
            result.setSuccess(true);
            result.setMessage("测试验证码已生成");
            return result;
        }

        String outId = IdWorker.getIdStr();
        try {
            SmsVerificationService.SendReceipt receipt =
                    smsVerificationService.sendRegistrationCode(phone, outId);
            registrationSmsState.markSent(
                    phone,
                    "dypns",
                    receipt.outId(),
                    receipt.validSeconds(),
                    receipt.intervalSeconds()
            );
            result.setSuccess(true);
            result.setMessage("验证码已发送");
        } catch (SmsVerificationException exception) {
            log.warn(
                    "绑定手机号短信发送失败，providerCode={}, requestId={}",
                    exception.getProviderCode(),
                    exception.getRequestId()
            );
            result.error500("短信验证码发送失败，请稍后重试");
        }
        return result;
    }

    @Override
    public Result<JSONObject> bind(String userId, String phone, String smsCode) {
        Result<JSONObject> result = new Result<>();
        if (oConvertUtils.isEmpty(phone) || !phone.matches(PHONE_PATTERN)) {
            result.error500("请输入正确的手机号");
            return result;
        }
        if (oConvertUtils.isEmpty(smsCode) || !smsCode.matches("^\\d{6}$")) {
            result.error500("请输入6位短信验证码");
            return result;
        }

        String lockToken = registrationSmsState.tryAcquireRegistrationLock(phone);
        if (lockToken == null) {
            result.error500("绑定请求正在处理中，请勿重复提交");
            return result;
        }
        try {
            return bindWithVerifiedCode(userId, phone, smsCode);
        } finally {
            registrationSmsState.releaseRegistrationLock(phone, lockToken);
        }
    }

    private Result<JSONObject> bindWithVerifiedCode(String userId, String phone, String smsCode) {
        Result<JSONObject> result = new Result<>();

        RegistrationSmsState.Session session = registrationSmsState.getSession(phone);
        if (session == null) {
            result.error500("手机验证码失效，请重新获取");
            return result;
        }

        if (smsDevMode) {
            if (!"dev".equals(session.provider()) || !SMS_DEV_CODE.equals(smsCode)) {
                result.error500("手机验证码错误");
                return result;
            }
        } else {
            if (!"dypns".equals(session.provider()) || oConvertUtils.isEmpty(session.outId())) {
                result.error500("手机验证码失效，请重新获取");
                return result;
            }
            try {
                boolean passed = smsVerificationService.checkRegistrationCode(phone, smsCode, session.outId());
                if (!passed) {
                    result.error500("手机验证码错误或已失效");
                    return result;
                }
            } catch (SmsVerificationException exception) {
                log.warn(
                        "绑定手机号验证码核验失败，providerCode={}, requestId={}",
                        exception.getProviderCode(),
                        exception.getRequestId()
                );
                result.error500("短信验证码校验服务暂不可用，请稍后重试");
                return result;
            }
        }

        // 校验通过后再查一次占用，防并发绑定同一手机号
        SysUser owner = sysUserService.getUserByPhone(phone);
        if (owner != null) {
            if (userId.equals(owner.getId())) {
                result.error500("该手机号已绑定当前账号");
            } else {
                result.error500("该手机号已注册，请使用账号密码登录");
            }
            return result;
        }

        SysUser current = sysUserService.getById(userId);
        if (current == null) {
            result.error500("用户不存在");
            return result;
        }

        try {
            current.setPhone(phone);
            sysUserService.updateById(current);
            registrationSmsState.clearSession(phone);
            JSONObject obj = new JSONObject();
            obj.put("userInfo", current);
            result.setResult(obj);
            result.setSuccess(true);
            result.setMessage("绑定成功");
        } catch (Exception exception) {
            log.warn("绑定手机号落库失败，手机号={}", maskMobile(phone));
            result.error500("绑定失败，请稍后重试");
        }
        return result;
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return "***";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
//update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】强制绑定手机号实现-----------
