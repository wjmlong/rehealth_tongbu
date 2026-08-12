package org.jeecg.modules.rehealth.website;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "ReHealth Website BFF API")
@RestController
@RequestMapping("/rehealth/website/v1")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class WebsiteRecordController {
    private static final List<String> RESOURCES = List.of("patients", "screening", "attributions", "settlements");
    private final WebsiteRecordRepository repository;

    public WebsiteRecordController(WebsiteRecordRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/{resource}")
    @Operation(summary = "Create or replace one tenant-scoped website record")
    public ResponseEntity<Result<Map<String, Object>>> save(
            @PathVariable String resource,
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody JsonNode payload
    ) {
        return respond(resource, tenantId, () -> {
            LoginUser user = currentUser();
            WebsiteRecord record = repository.save(user, resolveTenant(user, tenantId), resource, payload);
            return recordMap(record);
        }, HttpStatus.CREATED);
    }

    @GetMapping("/{resource}")
    @Operation(summary = "List tenant-scoped website records")
    public ResponseEntity<Result<Map<String, Object>>> list(
            @PathVariable String resource,
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return respond(resource, tenantId, () -> {
            LoginUser user = currentUser();
            String tenant = resolveTenant(user, tenantId);
            int boundedSize = Math.min(Math.max(pageSize, 1), 100);
            int boundedPage = Math.max(pageNo, 1);
            List<Map<String, Object>> records = repository.list(tenant, resource, boundedPage, boundedSize, keyword, status).stream().map(this::recordMap).toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", records);
            result.put("total", repository.count(tenant, resource, keyword, status));
            result.put("current", boundedPage);
            result.put("size", boundedSize);
            return result;
        }, HttpStatus.OK);
    }

    @GetMapping("/{resource}/{id}")
    public ResponseEntity<Result<Map<String, Object>>> get(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(resource, tenantId, () -> repository.find(resolveTenant(currentUser(), tenantId), resource, id).map(this::recordMap).orElseThrow(() -> new WebsiteApiException(HttpStatus.NOT_FOUND, "website record was not found")), HttpStatus.OK);
    }

    @DeleteMapping("/{resource}/{id}")
    public ResponseEntity<Result<Map<String, Object>>> delete(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(resource, tenantId, () -> {
            boolean deleted = repository.delete(resolveTenant(currentUser(), tenantId), resource, id);
            if (!deleted) throw new WebsiteApiException(HttpStatus.NOT_FOUND, "website record was not found");
            return Map.of("status", "deleted", "id", id);
        }, HttpStatus.OK);
    }

    private <T> ResponseEntity<Result<T>> respond(String resource, String tenantId, SupplierWithException<T> action, HttpStatus success) {
        if (!RESOURCES.contains(resource)) return ResponseEntity.badRequest().body(Result.error(400, "unsupported website resource"));
        try { return ResponseEntity.status(success).body(Result.OK(action.get())); }
        catch (WebsiteApiException e) { return ResponseEntity.status(e.status).body(Result.error(e.status.value(), e.getMessage())); }
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser user && user.getId() != null && !user.getId().isBlank()) return user;
        throw new WebsiteApiException(HttpStatus.UNAUTHORIZED, "authenticated Jeecg user is required");
    }

    private String resolveTenant(LoginUser user, String requested) {
        String tenant = requested == null || requested.isBlank()
                ? singleTenant(user.getRelTenantIds())
                : requested.trim();
        if (tenant == null || tenant.isBlank()) throw new WebsiteApiException(HttpStatus.BAD_REQUEST, "X-Tenant-Id is required");
        if (user.getRelTenantIds() != null && Arrays.stream(user.getRelTenantIds().split(","))
                .map(String::trim).noneMatch(tenant::equals) && !"admin".equalsIgnoreCase(user.getUsername())) {
            throw new WebsiteApiException(HttpStatus.FORBIDDEN, "requested tenant is not assigned to the authenticated user");
        }
        return tenant;
    }

    private String singleTenant(String memberships) {
        if (memberships == null || memberships.isBlank()) return null;
        String[] values = memberships.split(",");
        return values.length == 1 ? values[0].trim() : null;
    }

    private Map<String, Object> recordMap(WebsiteRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (record.payload() != null && record.payload().isObject()) record.payload().fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue()));
        map.putIfAbsent("id", record.id());
        map.put("status", record.status());
        map.put("created_at", record.createdAt());
        map.put("updated_at", record.updatedAt());
        return map;
    }

    @FunctionalInterface private interface SupplierWithException<T> { T get(); }
    private static final class WebsiteApiException extends RuntimeException {
        private final HttpStatus status;
        private WebsiteApiException(HttpStatus status, String message) { super(message); this.status = status; }
    }
}
