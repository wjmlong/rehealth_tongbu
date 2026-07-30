package org.jeecg.modules.rehealth.viomi;

/**
 * Result of verifying the Viomi access token (JWT HS256 signed with the shared AppKey).
 */
public class ViomiAuthResult {

    public final boolean valid;
    public final String appId;
    public final String imei;

    private ViomiAuthResult(boolean valid, String appId, String imei) {
        this.valid = valid;
        this.appId = appId;
        this.imei = imei;
    }

    public static ViomiAuthResult success(String appId, String imei) {
        return new ViomiAuthResult(true, appId, imei);
    }

    public static ViomiAuthResult failed() {
        return new ViomiAuthResult(false, null, null);
    }
}
