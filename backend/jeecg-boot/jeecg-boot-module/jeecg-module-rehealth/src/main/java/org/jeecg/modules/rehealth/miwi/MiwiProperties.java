package org.jeecg.modules.rehealth.miwi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Miwi (云米/MiwiTracker) 4G watch cloud integration.
 * Watches such as S8/S9/GS20 push health data from the vendor cloud to our
 * HTTP callback; the OpenAPI client is used for token retrieval and pull-based queries.
 */
@Component
public class MiwiProperties {
    /** Master switch. When disabled the callback endpoint rejects all requests. */
    @Value("${rehealth.miwi.enabled:false}")
    private boolean enabled;

    @Value("${rehealth.miwi.app-id:}")
    private String appId;

    @Value("${rehealth.miwi.app-key:}")
    private String appKey;

    /** Vendor OpenAPI base URL, assigned by the vendor. */
    @Value("${rehealth.miwi.api-base-url:}")
    private String apiBaseUrl;

    /**
     * Shared secret required as {@code ?token=} on the callback URL.
     * The vendor push protocol has no signature ("校验规则：无"), so we enforce
     * a private callback token as the minimum protection layer.
     */
    @Value("${rehealth.miwi.callback-token:}")
    private String callbackToken;

    /**
     * Prefix used to build the ReHealth deviceId from the watch IMEI.
     * Must match the Android app rule: {@code "<vendor lowercase>-" + sha256(imei).take(24)},
     * where the app vendor enum is MIWI4G.
     */
    @Value("${rehealth.miwi.device-id-prefix:miwi4g-}")
    private String deviceIdPrefix = "miwi4g-";

    /** Access token TTL safety margin in seconds before forced refresh. */
    @Value("${rehealth.miwi.token-ttl-seconds:6000}")
    private long tokenTtlSeconds = 6000L;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getCallbackToken() {
        return callbackToken;
    }

    public String getDeviceIdPrefix() {
        return deviceIdPrefix;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }
}
