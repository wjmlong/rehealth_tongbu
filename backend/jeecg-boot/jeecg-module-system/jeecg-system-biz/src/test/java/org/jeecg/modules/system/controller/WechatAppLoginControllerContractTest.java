package org.jeecg.modules.system.controller;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.system.service.IWechatAppLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatAppLoginControllerContractTest {

    @Test
    void mapsAppLoginRouteUnderWechatBasePath() throws NoSuchMethodException {
        RequestMapping base = WechatAppLoginController.class.getAnnotation(RequestMapping.class);
        assertNotNull(base);
        assertEquals("/rehealth/mobile/wechat", base.value()[0]);

        Method method = WechatAppLoginController.class.getMethod("appLogin", JSONObject.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        assertNotNull(post);
        assertEquals("/app-login", post.value()[0]);
    }

    @Test
    void appLoginIsPublic() throws NoSuchMethodException {
        Method method = WechatAppLoginController.class.getMethod("appLogin", JSONObject.class);
        assertTrue(method.isAnnotationPresent(IgnoreAuth.class),
                "微信 app-login 必须为 @IgnoreAuth 公开接口（Shiro anon 已放行 /rehealth/mobile/wechat/**）");
    }

    @Test
    void wrapsServiceFailureIntoResultError() {
        IWechatAppLoginService service = mock(IWechatAppLoginService.class);
        JSONObject failure = new JSONObject();
        failure.put("success", false);
        failure.put("message", "微信登录未配置");
        when(service.login(anyString())).thenReturn(failure);

        WechatAppLoginController controller = new WechatAppLoginController();
        ReflectionTestUtils.setField(controller, "wechatAppLoginService", service);

        JSONObject body = new JSONObject();
        body.put("code", "code-1");
        Result<JSONObject> result = controller.appLogin(body);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("微信登录未配置", result.getMessage());
        verify(service).login("code-1");
    }

    @Test
    void wrapsServiceSuccessIntoResultEnvelope() {
        IWechatAppLoginService service = mock(IWechatAppLoginService.class);
        JSONObject success = new JSONObject();
        success.put("success", true);
        success.put("token", "token-1");
        success.put("userInfo", new JSONObject());
        when(service.login(anyString())).thenReturn(success);

        WechatAppLoginController controller = new WechatAppLoginController();
        ReflectionTestUtils.setField(controller, "wechatAppLoginService", service);

        JSONObject body = new JSONObject();
        body.put("code", "code-1");
        Result<JSONObject> result = controller.appLogin(body);

        assertTrue(result.isSuccess());
        assertEquals("token-1", result.getResult().getString("token"));
    }
}
