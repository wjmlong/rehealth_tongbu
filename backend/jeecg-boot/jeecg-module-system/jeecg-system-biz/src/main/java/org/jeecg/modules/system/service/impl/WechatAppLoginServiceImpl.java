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
import org.jeecg.modules.system.service.IWechatAppLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

//update-begin---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用登录实现-----------
/**
 * 微信开放平台移动应用登录实现（App 侧微信登录）
 *
 * 链路：
 *   code -> 微信 sns/oauth2/access_token -> openid（unionid 落库备查）
 *   openid -> 查 sys_third_account(thirdType="wechat_app")
 *     - 已有 sys_user_id -> 直接登录
 *     - 无 -> 复用第三方账号体系自动创建 SysUser 并绑定
 *   -> 签发 APP 客户端 token
 *
 * 与 WechatMiniLoginServiceImpl（微信小程序）的差异只在 code 兑换方式：
 * 小程序走 jscode2session，移动应用必须走 sns/oauth2/access_token。
 * AppSecret 仅存服务端，配置缺失时失败关闭。
 */
@Slf4j
@Service
public class WechatAppLoginServiceImpl implements IWechatAppLoginService {

    private static final String THIRD_TYPE_WECHAT_APP = "wechat_app";
    private static final String OAUTH2_ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token";

    @Autowired
    private ISysThirdAccountService sysThirdAccountService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${rehealth.wechat.app.appid:}")
    private String wechatAppId;

    @Value("${rehealth.wechat.app.secret:}")
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
            result.put("message", "微信登录未配置");
            return result;
        }

        // 1. code -> openid（unionid 仅落库备查，本期不参与匹配逻辑）
        JSONObject identity = resolveOpenIdentity(code);
        String openid = identity == null ? null : identity.getString("openid");
        String unionid = identity == null ? null : identity.getString("unionid");
        if (oConvertUtils.isEmpty(openid)) {
            result.put("success", false);
            result.put("message", "微信登录凭证无效或已过期");
            return result;
        }

        // 2. 查/建第三方账号
        SysUser sysUser = resolveOrCreateUser(openid, unionid);
        if (sysUser == null || oConvertUtils.isEmpty(sysUser.getId())) {
            result.put("success", false);
            result.put("message", "微信登录失败：无法创建或获取用户");
            return result;
        }

        // 3. 签发 APP token（与 mLogin / 小程序登录一致）
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
     * 用 code 调微信 sns/oauth2/access_token 换取 openid/unionid。
     * 声明为 protected 便于测试子类覆写，不真实请求微信。
     */
    protected JSONObject resolveOpenIdentity(String code) {
        try {
            String url = OAUTH2_ACCESS_TOKEN_URL
                    + "?appid=" + wechatAppId
                    + "&secret=" + wechatSecret
                    + "&code=" + code
                    + "&grant_type=authorization_code";
            String body = HttpUtil.get(url, 5000);
            JSONObject resp = JSON.parseObject(body);
            if (resp == null) {
                return null;
            }
            if (resp.getInteger("errcode") != null && resp.getInteger("errcode") != 0) {
                log.warn("wechat oauth2/access_token error: errcode={}, errmsg={}",
                        resp.getInteger("errcode"), resp.getString("errmsg"));
                return null;
            }
            return resp;
        } catch (Exception e) {
            log.error("wechat oauth2/access_token 调用失败", e);
            return null;
        }
    }

    /**
     * 根据 openid 查找或创建系统用户；unionid 非空时写入 thirdUserId 备查。
     */
    private SysUser resolveOrCreateUser(String openid, String unionid) {
        List<SysThirdAccount> accounts = sysThirdAccountService.list(
                new LambdaQueryWrapper<SysThirdAccount>()
                        .eq(SysThirdAccount::getThirdType, THIRD_TYPE_WECHAT_APP)
                        .eq(SysThirdAccount::getThirdUserUuid, openid)
                        .eq(SysThirdAccount::getTenantId, CommonConstant.TENANT_ID_DEFAULT_VALUE)
        );

        SysThirdAccount account = accounts.isEmpty() ? null : accounts.get(0);

        if (account == null) {
            // 不存在第三方账号：先创建第三方账号记录，再创建系统用户
            ThirdLoginModel tlm = new ThirdLoginModel(THIRD_TYPE_WECHAT_APP, openid, "微信用户", "");
            account = sysThirdAccountService.saveThirdUser(tlm, CommonConstant.TENANT_ID_DEFAULT_VALUE);
        }

        if (account == null) {
            return null;
        }

        // unionid 落库备查（本期不参与账号匹配；saveThirdUser 默认把 thirdUserId 写成 openid）
        if (oConvertUtils.isNotEmpty(unionid) && !unionid.equals(account.getThirdUserId())) {
            account.setThirdUserId(unionid);
            sysThirdAccountService.updateById(account);
        }

        // 已绑定系统用户，直接返回
        if (oConvertUtils.isNotEmpty(account.getSysUserId())) {
            SysUser existing = sysUserService.getById(account.getSysUserId());
            if (existing != null) {
                return existing;
            }
        }

        // 未绑定：创建系统用户（用户名=openid，密码默认，与第三方登录一致；手机号为空，
        // 由客户端强制绑定手机号流程补齐 sys_user.phone）
        return sysThirdAccountService.createUser(null, openid, CommonConstant.TENANT_ID_DEFAULT_VALUE);
    }
}
//update-end---author:rehealth-dev ---date:2026-08-27  for：【App微信登录】微信移动应用登录实现-----------
