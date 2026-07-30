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
}
