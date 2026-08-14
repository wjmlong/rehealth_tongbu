package org.jeecg.modules.system.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.SysThirdAccount;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.model.ThirdLoginModel;
import org.jeecg.modules.system.service.ISysThirdAccountService;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.system.service.IWechatMiniLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序登录实现
 *
 * 链路：
 *   code -> 微信 jscode2session -> openid
 *   openid -> 查 sys_third_account(thirdType="wechat_mini")
 *     - 已有 sys_user_id -> 直接登录
 *     - 无 -> 复用第三方账号体系自动创建 SysUser 并绑定
 *   -> 签发 APP 客户端 token
 *
 * 复用 JeecgBoot 成熟能力：
 *   ISysThirdAccountService.saveThirdUser / createUser
 *   JwtUtil.sign(username, password, CLIENT_TYPE_APP)
 *   Redis PREFIX_USER_TOKEN + token
 */
@Slf4j
@Service
public class WechatMiniLoginServiceImpl implements IWechatMiniLoginService {

    private static final String THIRD_TYPE_WECHAT_MINI = "wechat_mini";
    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private ISysThirdAccountService sysThirdAccountService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${rehealth.wechat.mini.appid:}")
    private String wechatAppId;

    @Value("${rehealth.wechat.mini.secret:}")
    private String wechatSecret;

    @Override
    public JSONObject login(String code) {
        JSONObject result = new JSONObject();
        if (oConvertUtils.isEmpty(code)) {
            result.put("success", false);
            result.put("message", "微信登录凭证 code 不能为空");
            return result;
        }
        if (oConvertUtils.isEmpty(wechatAppId) || oConvertUtils.isEmpty(wechatSecret)) {
            result.put("success", false);
            result.put("message", "微信小程序 appid/secret 未配置");
            return result;
        }

        // 1. code -> openid
        String openid = resolveOpenId(code);
        if (openid == null) {
            result.put("success", false);
            result.put("message", "微信登录凭证无效或已过期");
            return result;
        }

        // 2. 查/建第三方账号
        SysUser sysUser = resolveOrCreateUser(openid);
        if (sysUser == null || oConvertUtils.isEmpty(sysUser.getId())) {
            result.put("success", false);
            result.put("message", "微信登录失败：无法创建或获取用户");
            return result;
        }

        // 3. 签发 APP token（与 mLogin 一致）
        String token = JwtUtil.sign(sysUser.getUsername(), sysUser.getPassword(), CommonConstant.CLIENT_TYPE_APP);
        stringRedisTemplate.opsForValue().set(
                CommonConstant.PREFIX_USER_TOKEN + token,
                token,
                JwtUtil.APP_EXPIRE_TIME * 2 / 1000,
                TimeUnit.SECONDS
        );

        result.put("success", true);
        result.put("token", token);
        result.put("userInfo", sysUser);
        return result;
    }

    /**
     * 用 code 调微信 jscode2session 换取 openid
     */
    private String resolveOpenId(String code) {
        try {
            String url = JSCODE2SESSION_URL
                    + "?appid=" + wechatAppId
                    + "&secret=" + wechatSecret
                    + "&js_code=" + code
                    + "&grant_type=authorization_code";
            String body = HttpUtil.get(url, 5000);
            JSONObject resp = JSON.parseObject(body);
            if (resp == null) {
                return null;
            }
            if (resp.getInteger("errcode") != null && resp.getInteger("errcode") != 0) {
                log.warn("jscode2session error: errcode={}, errmsg={}",
                        resp.getInteger("errcode"), resp.getString("errmsg"));
                return null;
            }
            return resp.getString("openid");
        } catch (Exception e) {
            log.error("jscode2session 调用失败", e);
            return null;
        }
    }

    /**
     * 根据 openid 查找或创建系统用户
     */
    private SysUser resolveOrCreateUser(String openid) {
        // 查第三方账号表
        List<SysThirdAccount> accounts = sysThirdAccountService.list(
                new LambdaQueryWrapper<SysThirdAccount>()
                        .eq(SysThirdAccount::getThirdType, THIRD_TYPE_WECHAT_MINI)
                        .eq(SysThirdAccount::getThirdUserUuid, openid)
                        .eq(SysThirdAccount::getTenantId, CommonConstant.TENANT_ID_DEFAULT_VALUE)
        );

        SysThirdAccount account = accounts.isEmpty() ? null : accounts.get(0);

        if (account == null) {
            // 不存在第三方账号：先创建第三方账号记录，再创建系统用户
            ThirdLoginModel tlm = new ThirdLoginModel(THIRD_TYPE_WECHAT_MINI, openid, "微信用户", "");
            account = sysThirdAccountService.saveThirdUser(tlm, CommonConstant.TENANT_ID_DEFAULT_VALUE);
        }

        if (account == null) {
            return null;
        }

        // 已绑定系统用户，直接返回
        if (oConvertUtils.isNotEmpty(account.getSysUserId())) {
            SysUser existing = sysUserService.getById(account.getSysUserId());
            if (existing != null) {
                return existing;
            }
        }

        // 未绑定：创建系统用户（用户名=openid，密码默认 123456，与第三方登录一致）
        SysUser created = sysThirdAccountService.createUser(null, openid, CommonConstant.TENANT_ID_DEFAULT_VALUE);
        return created;
    }
}
