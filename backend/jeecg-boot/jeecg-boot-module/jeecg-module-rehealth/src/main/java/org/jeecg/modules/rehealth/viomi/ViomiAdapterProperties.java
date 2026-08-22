package org.jeecg.modules.rehealth.viomi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Viomi (云米 / miwitracker) active-report adapter.
 *
 * <p>The Viomi platform pushes wearable telemetry to this backend through an HTTP callback.
 * Credentials (app-id / app-key) are issued by Viomi during onboarding and must be configured
 * in the deployment environment before enabling authentication.</p>
 */
@Component
public class ViomiAdapterProperties {

    @Value("${rehealth.viomi.enabled:true}")
    private boolean enabled;

    @Value("${rehealth.viomi.app-id:}")
    private String appId;

    @Value("${rehealth.viomi.app-key:}")
    private String appKey;

    @Value("${rehealth.viomi.require-auth:true}")
    private boolean requireAuth;

    @Value("${rehealth.viomi.user-id:viomi-gateway}")
    private String userId;

    @Value("${rehealth.viomi.source:viomi}")
    private String source;

    @Value("${rehealth.viomi.base-url:https://openapi.miwitracker.com}")
    private String baseUrl;

    @Value("${rehealth.viomi.connect-timeout-seconds:10}")
    private int connectTimeoutSeconds;

    @Value("${rehealth.viomi.request-timeout-seconds:30}")
    private int requestTimeoutSeconds;

    @Value("${rehealth.viomi.command.heart-rate:0}")
    private int heartRateCommand;

    @Value("${rehealth.viomi.command.blood-pressure:0}")
    private int bloodPressureCommand;

    @Value("${rehealth.viomi.command.blood-oxygen:0}")
    private int bloodOxygenCommand;

    @Value("${rehealth.viomi.plan-encryption-secret:}")
    private String planEncryptionSecret;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public boolean isRequireAuth() {
        return requireAuth;
    }

    public String getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public int commandFor(String metric) {
        return switch (metric) {
            case "HEART_RATE" -> heartRateCommand;
            case "BLOOD_PRESSURE" -> bloodPressureCommand;
            case "BLOOD_OXYGEN" -> bloodOxygenCommand;
            default -> 0;
        };
    }

    public String getPlanEncryptionSecret() {
        return planEncryptionSecret;
    }
}
