# ADR-002: TimescaleDB, Outbox, and Kafka ownership

Status: Accepted, version 1, 2026-07-23.

Device Service exclusively owns normalized hardware telemetry in TimescaleDB. Jeecg owns business records in `software_db` and does not read Timescale tables directly. The historical MySQL `hardware_db` remains a migration source until signed reconciliation and cutover; it is not a second writer after cutover.

Upload success requires durable write before success: normalized telemetry and its Outbox row commit in the same Timescale transaction. Kafka availability does not gate persisted upload success. The publisher retries pending Outbox rows and marks publication state without deleting audit history.

Topics are `rehealth.telemetry.persisted.v1`, `rehealth.telemetry.quality.v1`, and `rehealth.telemetry.dlq.v1`. Events contain opaque identifiers, time bounds, counts, quality status, and persistence/publication status only. They contain no metric values, raw PPG/RRI or other raw signals, tokens, prompts, or direct identifiers. The partition key is opaque `device_ref`, preserving per-device order. Delivery is at-least-once; every consumer is an idempotent consumer keyed by `event_id`, and poison events are quarantined to the DLQ with bounded metadata only.
