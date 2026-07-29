package org.jeecg.modules.rehealth.miwi.pull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed {@link S8SyncCursorRepository} on the software datasource.
 */
@Repository
public class JdbcS8SyncCursorRepository implements S8SyncCursorRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcS8SyncCursorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<S8SyncCursor> ROW_MAPPER = (rs, rowNum) -> new S8SyncCursor(
            rs.getString("device_id"),
            rs.getString("metric_type"),
            rs.getLong("cursor_utc"),
            rs.getLong("last_success_at"),
            rs.getInt("failure_count"),
            rs.getString("last_error_code")
    );

    @Override
    public Optional<S8SyncCursor> findByDeviceAndMetric(String deviceId, String metricType) {
        List<S8SyncCursor> rows = jdbcTemplate.query(
                "SELECT device_id, metric_type, cursor_utc, last_success_at, failure_count, last_error_code "
                        + "FROM rehealth_s8_sync_cursor WHERE device_id = ? AND metric_type = ?",
                ROW_MAPPER, deviceId, metricType
        );
        return rows.stream().findFirst();
    }

    @Override
    public void save(S8SyncCursor cursor) {
        jdbcTemplate.update(
                "INSERT INTO rehealth_s8_sync_cursor "
                        + "(device_id, metric_type, cursor_utc, last_success_at, failure_count, last_error_code, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NOW()) "
                        + "ON DUPLICATE KEY UPDATE cursor_utc = VALUES(cursor_utc), "
                        + "last_success_at = VALUES(last_success_at), failure_count = VALUES(failure_count), "
                        + "last_error_code = VALUES(last_error_code), updated_at = NOW()",
                cursor.deviceId, cursor.metricType, cursor.cursorUtcMillis, cursor.lastSuccessAtMillis,
                cursor.failureCount, cursor.lastError
        );
    }
}
