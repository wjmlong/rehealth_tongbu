package org.jeecg.modules.rehealth.miwi.pull;

import java.util.Optional;

/**
 * Per-(device, metric) pull cursor. A single global "last sync" time is explicitly
 * avoided: heart rate may succeed while sleep fails, so each metric advances
 * independently and a failed metric is retried without blocking the others.
 */
public interface S8SyncCursorRepository {

    Optional<S8SyncCursor> findByDeviceAndMetric(String deviceId, String metricType);

    void save(S8SyncCursor cursor);

    /** Final result of one pull attempt for a (device, metric). */
    final class S8SyncCursor {
        public final String deviceId;
        public final String metricType;
        public final long cursorUtcMillis;
        public final long lastSuccessAtMillis;
        public final int failureCount;
        public final String lastError;

        public S8SyncCursor(
                String deviceId,
                String metricType,
                long cursorUtcMillis,
                long lastSuccessAtMillis,
                int failureCount,
                String lastError
        ) {
            this.deviceId = deviceId;
            this.metricType = metricType;
            this.cursorUtcMillis = cursorUtcMillis;
            this.lastSuccessAtMillis = lastSuccessAtMillis;
            this.failureCount = failureCount;
            this.lastError = lastError;
        }

        public static S8SyncCursor initial(String deviceId, String metricType) {
            long now = System.currentTimeMillis();
            return new S8SyncCursor(deviceId, metricType, now, 0L, 0, null);
        }

        public S8SyncCursor withCursor(long newCursorUtcMillis) {
            return new S8SyncCursor(deviceId, metricType, newCursorUtcMillis, lastSuccessAtMillis, failureCount, lastError);
        }
    }
}
