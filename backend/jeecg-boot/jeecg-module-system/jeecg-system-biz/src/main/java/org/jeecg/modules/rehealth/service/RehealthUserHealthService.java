package org.jeecg.modules.rehealth.service;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.RestUtil;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合「注册用户（sys_user）基础信息」与「device-service 中的健康/提取数据」。
 *
 * 设计说明（遵循 AGENTS.md：跨服务通信走 client 抽象）：
 *  - 用户档案来自 JeecgBoot 的 sys_user（MySQL）。
 *  - 健康/提取数据来自 device-service 的 TimescaleDB，通过内部运维端点拉取，
 *    复用 device-service 既有的 X-ReHealth-Service-Credential 服务间凭证。
 */
@Service
public class RehealthUserHealthService {

    @Autowired
    private ISysUserService userService;

    @Value("${rehealth.device-service.base-url:}")
    private String deviceBaseUrl;

    @Value("${rehealth.device-service.credential:}")
    private String deviceCredential;

    public List<RehealthUserHealthVO> listUsersWithHealth() {
        List<SysUser> users = userService.list();
        List<RehealthUserHealthVO> result = new ArrayList<>();
        for (SysUser u : users) {
            // 过滤已逻辑删除的账户（delFlag=1）
            if (u.getDelFlag() != null && u.getDelFlag() == 1) {
                continue;
            }
            RehealthUserHealthVO vo = new RehealthUserHealthVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealname(u.getRealname());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setSex(u.getSex());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            vo.setHealth(fetchHealth(u.getId()));
            result.add(vo);
        }
        return result;
    }

    private JSONObject fetchHealth(String userId) {
        if (deviceBaseUrl == null || deviceBaseUrl.isBlank()) {
            return null;
        }
        try {
            String base = deviceBaseUrl.endsWith("/")
                    ? deviceBaseUrl.substring(0, deviceBaseUrl.length() - 1)
                    : deviceBaseUrl;
            String url = base + "/rehealth/internal/v1/operations/users/" + userId + "/health";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-ReHealth-Service-Credential", deviceCredential);
            RestTemplate restTemplate = RestUtil.getRestTemplate();
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JSONObject json = JSONObject.parseObject(resp.getBody());
            return json == null ? null : json.getJSONObject("result");
        } catch (Exception e) {
            // 单用户健康数据拉取失败不应阻断整体列表；健康字段返回 null
            return null;
        }
    }
}
