package org.jeecg.modules.rehealth.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.service.agent.HealthAgentMobileService;
import org.jeecg.modules.rehealth.service.agent.HealthAgentRequestException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth Health Agent API")
@RestController
@RequestMapping("/rehealth/mobile/agent")
public class HealthAgentController {
    private final HealthAgentMobileService service;
    private final AuthenticatedTenantResolver tenantResolver;

    public HealthAgentController(
            HealthAgentMobileService service,
            AuthenticatedTenantResolver tenantResolver
    ) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message to the authenticated user's health agent")
    public Result<HealthAgentResponseDto> message(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody HealthAgentMessageRequestDto request
    ) {
        LoginUser user = currentUser();
        String authorizedTenant = tenantResolver.resolve(user, tenantId);
        try {
            return Result.OK(service.respond(authorizedTenant, user.getId(), request));
        } catch (HealthAgentRequestException failure) {
            return Result.error(failure.statusCode(), failure.getMessage());
        } catch (RuntimeException unavailable) {
            return Result.error(503, "health-agent is temporarily unavailable; retry later");
        }
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser user && user.getId() != null && !user.getId().isBlank()) {
            return user;
        }
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }

}
