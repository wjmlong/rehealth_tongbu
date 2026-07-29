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

    /**
     * Master switch for the periodic PULL connector (backend -> vendor OpenAPI -> our DB).
     * Distinct from {@code enabled}, which gates the vendor push callback. Recommended
     * primary path for 8 月初; keep push as a supplementary realtime channel.
     */
    @Value("${rehealth.miwi.pull.enabled:false}")
    private boolean pullEnabled;

    /**
     * How the vendor bytime endpoints expect {@code startTime}/{endTime}.
     * One of {@code epoch_millis}, {@code epoch_seconds}, or a java DateTimeFormatter
     * pattern such as {@code yyyy-MM-dd HH:mm:ss}. MUST be confirmed against vendor doc V1.6.5.
     */
    @Value("${rehealth.miwi.pull.time-format:epoch_seconds}")
    private String pullTimeFormat = "epoch_seconds";

    /** Extra backfill window (minutes) subtracted from the cursor to catch late vendor uploads. */
    @Value("${rehealth.miwi.pull.backfill-minutes:10}")
    private int pullBackfillMinutes = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public String getPullTimeFormat() {
        return pullTimeFormat;
    }

    public int getPullBackfillMinutes() {
        return pullBackfillMinutes;
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
