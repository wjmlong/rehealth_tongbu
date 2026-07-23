package com.rehealth.device.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
public final class JdbcOutboxStore implements OutboxStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public JdbcOutboxStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public Optional<OutboxEvent> claimNext() {
        Optional<OutboxEvent> claimed = transactions.execute(status -> {
            List<OutboxEvent> rows = jdbc.query("""
                    SELECT id, event_type, event_version, event_metadata::text,
                           attempt_count, available_at
                    FROM hardware_outbox
                    WHERE (status IN ('PENDING', 'FAILED', 'PUBLISHING'))
                      AND available_at <= now()
                    ORDER BY created_at, id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                    """, (row, index) -> new OutboxEvent(
                    row.getObject("id", UUID.class),
                    row.getString("event_type"),
                    row.getInt("event_version"),
                    row.getString("event_metadata"),
                    row.getInt("attempt_count"),
                    row.getTimestamp("available_at").toInstant()));
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            OutboxEvent event = rows.get(0);
            jdbc.update("""
                    UPDATE hardware_outbox
                    SET status = 'PUBLISHING', attempt_count = attempt_count + 1,
                        available_at = now() + interval '30 seconds', updated_at = now()
                    WHERE id = ?
                    """, event.eventId());
            return Optional.of(event);
        });
        return claimed == null ? Optional.empty() : claimed;
    }

    @Override
    public void markPublished(UUID eventId) {
        transactions.executeWithoutResult(status -> {
            jdbc.update("""
                    UPDATE hardware_outbox
                    SET status = 'PUBLISHED', published_at = now(),
                        last_error_code = NULL, updated_at = now()
                    WHERE id = ? AND status = 'PUBLISHING'
                    """, eventId);
            jdbc.update("""
                    UPDATE hardware_reconciliation reconciliation
                    SET state = 'EVENT_PUBLISHED', last_error_code = NULL, updated_at = now()
                    FROM hardware_outbox acknowledged
                    WHERE acknowledged.id = ?
                      AND reconciliation.upload_batch_id = acknowledged.upload_batch_id
                      AND NOT EXISTS (
                        SELECT 1 FROM hardware_outbox pending
                        WHERE pending.upload_batch_id = acknowledged.upload_batch_id
                          AND pending.status <> 'PUBLISHED')
                    """, eventId);
            jdbc.update("""
                    UPDATE hardware_upload_batch batch
                    SET status = 'EVENT_PUBLISHED'
                    FROM hardware_outbox acknowledged
                    WHERE acknowledged.id = ?
                      AND batch.id = acknowledged.upload_batch_id
                      AND NOT EXISTS (
                        SELECT 1 FROM hardware_outbox pending
                        WHERE pending.upload_batch_id = acknowledged.upload_batch_id
                          AND pending.status <> 'PUBLISHED')
                    """, eventId);
        });
    }

    @Override
    public void markRetryable(UUID eventId, String errorCode, Instant availableAt) {
        jdbc.update("""
                UPDATE hardware_outbox
                SET status = 'FAILED', last_error_code = ?, available_at = ?, updated_at = now()
                WHERE id = ? AND status = 'PUBLISHING'
                """, errorCode, Timestamp.from(availableAt), eventId);
    }

    @Override
    public void quarantine(UUID eventId, String errorCode) {
        transactions.executeWithoutResult(status -> {
            jdbc.update("""
                    UPDATE hardware_outbox
                    SET status = 'DLQ_REVIEW', last_error_code = ?, updated_at = now()
                    WHERE id = ?
                    """, errorCode, eventId);
            jdbc.update("""
                    UPDATE hardware_reconciliation reconciliation
                    SET state = 'DLQ_REVIEW', last_error_code = ?, updated_at = now()
                    FROM hardware_outbox poisoned
                    WHERE poisoned.id = ?
                      AND reconciliation.upload_batch_id = poisoned.upload_batch_id
                    """, errorCode, eventId);
            jdbc.update("""
                    UPDATE hardware_upload_batch batch
                    SET status = 'DLQ_REVIEW'
                    FROM hardware_outbox poisoned
                    WHERE poisoned.id = ? AND batch.id = poisoned.upload_batch_id
                    """, eventId);
        });
    }
}
