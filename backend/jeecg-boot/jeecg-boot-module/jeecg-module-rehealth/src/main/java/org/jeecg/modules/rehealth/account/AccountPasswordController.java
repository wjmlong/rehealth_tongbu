package org.jeecg.modules.rehealth.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.insurance.InsuranceApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "ReHealth Account Password API")
@RestController
@RequestMapping("/rehealth/account/password")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class AccountPasswordController {
    private final AccountPasswordService service;

    public AccountPasswordController(AccountPasswordService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @Operation(summary = "Check whether the current account must change its password")
    public ResponseEntity<Result<AccountPasswordResponse.Status>> status() {
        return respond(() -> service.status(currentUser().getId()));
    }

    @PutMapping
    @Operation(summary = "Change the current account password")
    public ResponseEntity<Result<AccountPasswordResponse.Change>> change(
            @RequestBody AccountPasswordRequest.Change request) {
        return respond(() -> service.changeOwnPassword(currentUser().getId(), request));
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw InsuranceApiException.forbidden("authenticated account is required");
    }

    private <T> ResponseEntity<Result<T>> respond(Supplier<T> action) {
        try {
            return ResponseEntity.ok(Result.OK(action.get()));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }
}
