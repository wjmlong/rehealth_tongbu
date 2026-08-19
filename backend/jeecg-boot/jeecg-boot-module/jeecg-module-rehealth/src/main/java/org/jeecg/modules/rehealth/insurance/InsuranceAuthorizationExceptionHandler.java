package org.jeecg.modules.rehealth.insurance;

import org.apache.shiro.authz.AuthorizationException;
import org.jeecg.common.api.vo.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        InsuranceRiskController.class,
        InsuranceImportController.class,
        InsuranceStudyController.class,
        InsuranceMobilePlanController.class,
        InsuranceInterventionWorkbenchController.class,
        InsuranceCarePlanController.class,
        DisabledInsuranceRiskController.class
})
public class InsuranceAuthorizationExceptionHandler {
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Result<Void>> forbidden(AuthorizationException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(HttpStatus.FORBIDDEN.value(), "insurance permission is required"));
    }
}
