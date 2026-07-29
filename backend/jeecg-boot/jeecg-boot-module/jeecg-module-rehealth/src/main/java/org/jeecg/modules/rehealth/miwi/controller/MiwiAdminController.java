package org.jeecg.modules.rehealth.miwi.controller;

import org.jeecg.modules.rehealth.miwi.pull.JdbcS8DeviceRegistry;
import org.jeecg.modules.rehealth.miwi.pull.S8DeviceRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator-facing import for the S8 (云米) vendor-account device list.
 *
 * 8 月初 flow: an operator imports each watch (IMEI + model + role) here; the watch
 * only starts producing telemetry once the same IMEI is also bound to a user through
 * the normal app bind flow (which writes {@code rehealth_device_binding} with the
 * same {@code miwi4g-<sha256(imei)[:24]>} deviceId). This keeps device onboarding and
 * user binding as separate, auditable steps. Requires an authenticated Jeecg session.
 */
@RestController
@RequestMapping("/rehealth/miwi/admin")
public class MiwiAdminController {

    private final JdbcS8DeviceRegistry registry;

    public MiwiAdminController(JdbcS8DeviceRegistry registry) {
        this.registry = registry;
    }

    public static final class S8DeviceImportRequest {
        public String imei;
        public String model;
        public String role; // e.g. SAFETY_4G, PRIMARY_ACTIVITY
    }

    @PostMapping("/s8-devices")
    public Map<String, Object> importS8Device(@RequestBody S8DeviceImportRequest request) {
        if (request.imei == null || request.imei.trim().length() < 10) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "imei must be at least 10 digits");
            return err;
        }
        String imei = request.imei.trim();
        String deviceId = registry.deviceIdForImei(imei);
        registry.upsert(new S8DeviceRegistry.S8Device(
                deviceId,
                imei,
                request.model == null ? "RH-S8-4G01" : request.model.trim(),
                request.role == null ? "SAFETY_4G" : request.role.trim(),
                true
        ));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("deviceId", deviceId);
        return result;
    }
}
