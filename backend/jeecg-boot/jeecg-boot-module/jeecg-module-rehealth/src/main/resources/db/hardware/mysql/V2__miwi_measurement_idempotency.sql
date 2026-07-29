-- Per-measurement idempotency for vendor pull connectors (S8 / future L16).
-- The writer now sends `INSERT ... ON DUPLICATE KEY UPDATE id = id`, so a re-pull of
-- an overlapping window (the intentional backfill) never creates duplicate rows.
-- client_record_id is deterministic for S8 pulls (s8-<sha256(...)>); for BLE it is the
-- client's own random id, which is already unique. NULLs are permitted and stay distinct.

ALTER TABLE hardware_measurement
    ADD UNIQUE KEY uk_hardware_measurement_dedupe (client_record_id);
