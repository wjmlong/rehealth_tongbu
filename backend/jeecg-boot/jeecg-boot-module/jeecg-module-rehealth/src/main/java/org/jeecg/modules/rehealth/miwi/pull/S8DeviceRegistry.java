package org.jeecg.modules.rehealth.miwi.pull;

import java.util.List;
import java.util.Optional;

/**
 * Registry of S8 (云米) watches known to our vendor account.
 *
 * This is the operator-imported list (user + IMEI + model + role) that the pull
 * connector iterates over. It is intentionally separate from {@code rehealth_device_binding}
 * (the app-side user binding): a device only produces telemetry once it is BOTH in this
 * registry AND bound to a user via the normal app bind flow (which writes
 * {@code rehealth_device_binding} with the same {@code miwi4g-<sha256(imei)[:24]>} deviceId).
 */
public interface S8DeviceRegistry {

    List<S8Device> findActiveDevices();

    void upsert(S8Device device);

    Optional<S8Device> findByDeviceId(String deviceId);

    /** A single S8 watch registered against our vendor account. */
    final class S8Device {
        public final String deviceId;
        public final String imei;
        public final String model;
        public final String role;
        public final boolean active;

        public S8Device(String deviceId, String imei, String model, String role, boolean active) {
            this.deviceId = deviceId;
            this.imei = imei;
            this.model = model;
            this.role = role;
            this.active = active;
        }
    }
}
