package org.jeecg.modules.rehealth.miwi.pull;

import org.jeecg.modules.rehealth.miwi.MiwiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed {@link S8DeviceRegistry} using the software (Jeecg) datasource.
 * Mirrors the datasource used by {@code JdbcSoftwareDbReHealthBusinessRepository}.
 *
 * IMEI is stored verbatim because the vendor OpenAPI requires the raw IMEI to query;
 * in production this column should be encrypted at rest (app-level, e.g. Jasypt) and
 * never appear in logs. The deviceId is the stable hash {@code miwi4g-<sha256(imei)[:24]>}.
 */
@Repository
public class JdbcS8DeviceRegistry implements S8DeviceRegistry {

    private final JdbcTemplate jdbcTemplate;
    private final MiwiProperties properties;

    @Autowired
    public JdbcS8DeviceRegistry(JdbcTemplate jdbcTemplate, MiwiProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    private static final RowMapper<S8Device> ROW_MAPPER = (rs, rowNum) -> new S8Device(
            rs.getString("device_id"),
            rs.getString("imei"),
            rs.getString("model"),
            rs.getString("role"),
            rs.getInt("is_active") != 0
    );

    @Override
    public List<S8Device> findActiveDevices() {
        return jdbcTemplate.query(
                "SELECT device_id, imei, model, role, is_active FROM rehealth_s8_device WHERE is_active = 1",
                ROW_MAPPER
        );
    }

    @Override
    public void upsert(S8Device device) {
        jdbcTemplate.update(
                "INSERT INTO rehealth_s8_device (device_id, imei, model, role, is_active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, 1, NOW(), NOW()) "
                        + "ON DUPLICATE KEY UPDATE imei = VALUES(imei), model = VALUES(model), "
                        + "role = VALUES(role), is_active = 1, updated_at = NOW()",
                device.deviceId, device.imei, device.model, device.role
        );
    }

    @Override
    public Optional<S8Device> findByDeviceId(String deviceId) {
        List<S8Device> rows = jdbcTemplate.query(
                "SELECT device_id, imei, model, role, is_active FROM rehealth_s8_device WHERE device_id = ?",
                ROW_MAPPER, deviceId
        );
        return rows.stream().findFirst();
    }

    /** Computes the stable deviceId from an IMEI using the same rule as the app/push path. */
    public String deviceIdForImei(String imei) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imei.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return properties.getDeviceIdPrefix() + HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (Exception e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }
}
