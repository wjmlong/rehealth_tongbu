package org.jeecg.modules.rehealth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.rehealth.service.RehealthUserHealthService;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员端点：列出注册用户及其基础信息 + 软件提取的健康数据。
 * 需要登录 token（X-Access-Token），由 JeecgBoot 全局 AuthInterceptor 校验。
 *
 * GET /rehealth/admin/v1/users
 */
@Slf4j
@Tag(name = "ReHealth 用户健康聚合")
@RestController
@RequestMapping("/rehealth/admin/v1/users")
public class RehealthUserHealthAdminController {

    @Autowired
    private RehealthUserHealthService service;

    @GetMapping
    public Result<List<RehealthUserHealthVO>> list() {
        return Result.ok(service.listUsersWithHealth());
    }
}
