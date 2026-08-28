package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.modules.system.entity.SysThirdAccount;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysThirdAccountService;
import org.jeecg.modules.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatAppLoginServiceTest {

    private ISysThirdAccountService thirdAccountService;
    private ISysUserService sysUserService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private WechatAppLoginServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        thirdAccountService = mock(ISysThirdAccountService.class);
        sysUserService = mock(ISysUserService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new WechatAppLoginServiceImpl() {
            @Override
            protected JSONObject resolveOpenIdentity(String code) {
                JSONObject identity = new JSONObject();
                identity.put("openid", "openid-" + code);
                identity.put("unionid", "unionid-" + code);
                return identity;
            }
        };
        ReflectionTestUtils.setField(service, "sysThirdAccountService", thirdAccountService);
        ReflectionTestUtils.setField(service, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "wechatAppId", "wx-test-appid");
        ReflectionTestUtils.setField(service, "wechatSecret", "test-secret");
    }

    @Test
    void rejectsBlankCode() {
        JSONObject result = service.login("");
        assertFalse(result.getBoolean("success"));
        assertEquals("微信登录凭证 code 不能为空", result.getString("message"));
    }

    @Test
    void failsClosedWhenWechatConfigMissing() {
        ReflectionTestUtils.setField(service, "wechatAppId", "");
        JSONObject result = service.login("code-1");
        assertFalse(result.getBoolean("success"));
        assertEquals("微信登录未配置", result.getString("message"));
    }

    @Test
    void rejectsWhenIdentityResolutionFails() {
        ReflectionTestUtils.setField(service, "wechatAppId", "wx-test-appid");
        ReflectionTestUtils.setField(service, "wechatSecret", "test-secret");
        WechatAppLoginServiceImpl failing = new WechatAppLoginServiceImpl() {
            @Override
            protected JSONObject resolveOpenIdentity(String code) {
                return null;
            }
        };
        ReflectionTestUtils.setField(failing, "sysThirdAccountService", thirdAccountService);
        ReflectionTestUtils.setField(failing, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(failing, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(failing, "wechatAppId", "wx-test-appid");
        ReflectionTestUtils.setField(failing, "wechatSecret", "test-secret");

        JSONObject result = failing.login("code-1");
        assertFalse(result.getBoolean("success"));
        assertEquals("微信登录凭证无效或已过期", result.getString("message"));
    }

    @Test
    void logsInExistingBoundUserAndStoresUnionId() {
        SysUser existing = new SysUser();
        existing.setId("user-1");
        existing.setUsername("wechat-user");
        existing.setPassword("encrypted");

        SysThirdAccount bound = new SysThirdAccount();
        bound.setId("account-1");
        bound.setThirdType("wechat_app");
        bound.setThirdUserUuid("openid-code-1");
        bound.setThirdUserId("openid-code-1");
        bound.setSysUserId("user-1");

        when(thirdAccountService.list(any(Wrapper.class)))
                .thenReturn(List.of(bound));
        when(sysUserService.getById("user-1")).thenReturn(existing);

        JSONObject result = service.login("code-1");

        assertTrue(result.getBoolean("success"));
        assertEquals("user-1", result.getJSONObject("userInfo").getString("id"));
        verify(thirdAccountService).updateById(any(SysThirdAccount.class));
        verify(thirdAccountService, never()).saveThirdUser(any(), any());
        // token 写入 Redis（与 mLogin 相同的 PREFIX_USER_TOKEN 约定）
        verify(valueOperations).set(anyString(), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
    }

    @Test
    void autoRegistersNewUserAndIssuesToken() {
        when(thirdAccountService.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        SysThirdAccount createdAccount = new SysThirdAccount();
        createdAccount.setId("account-2");
        createdAccount.setThirdType("wechat_app");
        createdAccount.setThirdUserUuid("openid-code-2");
        when(thirdAccountService.saveThirdUser(any(), any())).thenReturn(createdAccount);

        SysUser createdUser = new SysUser();
        createdUser.setId("user-2");
        createdUser.setUsername("openid-code-2");
        createdUser.setPassword("encrypted");
        when(thirdAccountService.createUser(eq((String) null), eq("openid-code-2"), any()))
                .thenReturn(createdUser);

        JSONObject result = service.login("code-2");

        assertTrue(result.getBoolean("success"));
        assertNotNull(result.getString("token"));
        assertEquals("user-2", result.getJSONObject("userInfo").getString("id"));
        verify(valueOperations).set(anyString(), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
    }
}
