-- Narrow cleanup for LOCAL_MEDICAL_TEST_SEED TimescaleDB rows.
-- ON DELETE CASCADE removes child telemetry owned by these synthetic batches.
\set ON_ERROR_STOP on
BEGIN;
DELETE FROM hardware_upload_batch
WHERE source = 'LOCAL_MEDICAL_TEST_SEED'
  AND tenant_id IN ('9261', '9262');
COMMIT;

SELECT 'remaining_hardware_batches' AS entity, COUNT(*) AS row_count
FROM hardware_upload_batch WHERE source='LOCAL_MEDICAL_TEST_SEED';
