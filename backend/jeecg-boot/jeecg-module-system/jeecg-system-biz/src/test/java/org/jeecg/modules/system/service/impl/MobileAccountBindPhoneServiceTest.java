package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.sms.SmsVerificationService;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.system.service.RegistrationSmsState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileAccountBindPhoneServiceTest {

    private static final String USER_ID = "user-1";
    private static final String PHONE = "13800138000";

    private ISysUserService sysUserService;
    private RegistrationSmsState registrationSmsState;
    private SmsVerificationService smsVerificationService;
    private MobileAccountBindPhoneServiceImpl service;

    @BeforeEach
    void setUp() {
        sysUserService = mock(ISysUserService.class);
        registrationSmsState = mock(RegistrationSmsState.class);
        smsVerificationService = mock(SmsVerificationService.class);
        service = new MobileAccountBindPhoneServiceImpl();
        ReflectionTestUtils.setField(service, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(service, "registrationSmsState", registrationSmsState);
        ReflectionTestUtils.setField(service, "smsVerificationService", smsVerificationService);
        // 测试环境默认走生产分支（JEECG_SMS_DEV_MODE 未设置）
        ReflectionTestUtils.setField(service, "smsDevMode", false);
    }

    @Test
    void sendCodeRejectsInvalidPhone() {
        Result<String> result = service.sendCode(USER_ID, "123", "1.2.3.4");
        assertFalse(result.isSuccess());
        assertEquals("请输入正确的手机号", result.getMessage());
        verify(registrationSmsState, never()).allowSend(anyString(), anyString());
    }

    @Test
    void sendCodeRejectsPhoneOwnedByAnotherAccount() {
        SysUser other = new SysUser();
        other.setId("user-2");
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(other);

        Result<String> result = service.sendCode(USER_ID, PHONE, "1.2.3.4");

        assertFalse(result.isSuccess());
        assertEquals("该手机号已注册，请使用账号密码登录", result.getMessage());
        verify(registrationSmsState, never()).allowSend(anyString(), anyString());
    }

    @Test
    void sendCodeRejectsPhoneAlreadyBoundToCurrentAccount() {
        SysUser self = new SysUser();
        self.setId(USER_ID);
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(self);

        Result<String> result = service.sendCode(USER_ID, PHONE, "1.2.3.4");

        assertFalse(result.isSuccess());
        assertEquals("该手机号已绑定当前账号", result.getMessage());
    }

    @Test
    void sendCodeRejectsWhenRateLimitDenied() {
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(null);
        when(registrationSmsState.allowSend(PHONE, "1.2.3.4")).thenReturn(false);

        Result<String> result = service.sendCode(USER_ID, PHONE, "1.2.3.4");

        assertFalse(result.isSuccess());
        assertEquals("短信接口请求太多，请稍后再试！", result.getMessage());
    }

    @Test
    void sendCodeUsesDypnsAndRecordsSession() {
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(null);
        when(registrationSmsState.allowSend(PHONE, "1.2.3.4")).thenReturn(true);
        when(smsVerificationService.sendRegistrationCode(eq(PHONE), anyString()))
                .thenReturn(new SmsVerificationService.SendReceipt("out-1", "req-1", "biz-1", 300, 60));

        Result<String> result = service.sendCode(USER_ID, PHONE, "1.2.3.4");

        assertTrue(result.isSuccess());
        verify(registrationSmsState).markSent(PHONE, "dypns", "out-1", 300, 60);
    }

    @Test
    void bindRejectsInvalidCodeFormat() {
        Result<JSONObject> result = service.bind(USER_ID, PHONE, "12ab");
        assertFalse(result.isSuccess());
        assertEquals("请输入6位短信验证码", result.getMessage());
        verify(registrationSmsState, never()).tryAcquireRegistrationLock(anyString());
    }

    @Test
    void bindRejectsWhenNoSessionExists() {
        when(registrationSmsState.tryAcquireRegistrationLock(PHONE)).thenReturn("lock-1");
        when(registrationSmsState.getSession(PHONE)).thenReturn(null);

        Result<JSONObject> result = service.bind(USER_ID, PHONE, "123456");

        assertFalse(result.isSuccess());
        assertEquals("手机验证码失效，请重新获取", result.getMessage());
        verify(registrationSmsState).releaseRegistrationLock(PHONE, "lock-1");
    }

    @Test
    void bindRejectsWrongVerificationCode() {
        when(registrationSmsState.tryAcquireRegistrationLock(PHONE)).thenReturn("lock-1");
        when(registrationSmsState.getSession(PHONE))
                .thenReturn(new RegistrationSmsState.Session("dypns", "out-1"));
        when(smsVerificationService.checkRegistrationCode(PHONE, "654321", "out-1")).thenReturn(false);

        Result<JSONObject> result = service.bind(USER_ID, PHONE, "654321");

        assertFalse(result.isSuccess());
        assertEquals("手机验证码错误或已失效", result.getMessage());
    }

    @Test
    void bindRejectsWhenPhoneTakenDuringRace() {
        when(registrationSmsState.tryAcquireRegistrationLock(PHONE)).thenReturn("lock-1");
        when(registrationSmsState.getSession(PHONE))
                .thenReturn(new RegistrationSmsState.Session("dypns", "out-1"));
        when(smsVerificationService.checkRegistrationCode(PHONE, "123456", "out-1")).thenReturn(true);

        SysUser other = new SysUser();
        other.setId("user-2");
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(other);

        Result<JSONObject> result = service.bind(USER_ID, PHONE, "123456");

        assertFalse(result.isSuccess());
        assertEquals("该手机号已注册，请使用账号密码登录", result.getMessage());
        verify(sysUserService, never()).updateById(any());
    }

    @Test
    void bindWritesPhoneAndClearsSessionOnPass() {
        when(registrationSmsState.tryAcquireRegistrationLock(PHONE)).thenReturn("lock-1");
        when(registrationSmsState.getSession(PHONE))
                .thenReturn(new RegistrationSmsState.Session("dypns", "out-1"));
        when(smsVerificationService.checkRegistrationCode(PHONE, "123456", "out-1")).thenReturn(true);
        when(sysUserService.getUserByPhone(PHONE)).thenReturn(null);

        SysUser current = new SysUser();
        current.setId(USER_ID);
        current.setUsername("openid-user");
        when(sysUserService.getById(USER_ID)).thenReturn(current);

        Result<JSONObject> result = service.bind(USER_ID, PHONE, "123456");

        assertTrue(result.isSuccess());
        assertEquals(PHONE, current.getPhone());
        verify(sysUserService).updateById(current);
        verify(registrationSmsState).clearSession(PHONE);
        assertEquals(USER_ID, result.getResult().getJSONObject("userInfo").getString("id"));
        verify(registrationSmsState).releaseRegistrationLock(PHONE, "lock-1");
    }
}
