# ADR-002: TimescaleDB, Outbox, and Kafka ownership

Status: Accepted, version 1, 2026-07-23.

Device Service exclusively owns normalized hardware telemetry in TimescaleDB. Jeecg owns business records in `software_db` and does not read Timescale tables directly. The historical MySQL `hardware_db` remains a migration source until signed reconciliation and cutover; it is not a second writer after cutover.

Upload success requires durable write before success: normalized telemetry and its Outbox row commit in the same Timescale transaction. Kafka availability does not gate persisted upload success. The publisher retries pending Outbox rows and marks publication state without deleting audit history.

Topics are `rehealth.telemetry.persisted.v1`, `rehealth.telemetry.quality.v1`, and `rehealth.telemetry.dlq.v1`. Events contain opaque identifiers, time bounds, counts, quality status, and persistence/publication status only. They contain no metric values, raw PPG/RRI or other raw signals, tokens, prompts, or direct identifiers. The partition key is opaque `device_ref`, preserving per-device order. Delivery is at-least-once; every consumer is an idempotent consumer keyed by `event_id`, and poison events are quarantined to the DLQ with bounded metadata only.

## Why Kafka is behind the Outbox

Kafka is not the durable system of record and is not part of the synchronous
mobile upload success condition. TimescaleDB is authoritative. The database
transaction first commits normalized telemetry and an Outbox row; only then does
the publisher attempt to send the bounded event to Kafka.

This placement provides:

- isolation between device ingestion and slower downstream consumers;
- peak smoothing when many devices reconnect and upload together;
- per-device ordering through the opaque `device_ref` partition key;
- replayable, at-least-once delivery with consumer idempotency by `event_id`;
- independent consumers for operations, quality, feature pipelines and audit;
- continued ingestion while Kafka is unavailable, with pending Outbox rows retried later.

Kafka events intentionally carry references and bounded operational metadata,
not raw or normalized measurement values. A future feature consumer must use the
authorized batch/device reference to obtain the required Timescale data rather
than copying raw health data into the event bus.

## Alternatives considered

| Alternative | Suitable use | Reason it is not the current default |
| --- | --- | --- |
| Timescale/PostgreSQL Outbox worker without a broker | Small pilot and one downstream action | Simplest deployment, but weak fan-out, replay tooling and independent consumer scaling |
| RabbitMQ | Commands, work queues and complex routing | Strong task routing, but event retention and stream replay are weaker than Kafka for the planned telemetry pipeline |
| Redis Streams | Lightweight low-latency processing where Redis is already operated | Lower operational cost, but weaker long-term retention, capacity isolation and audit posture |
| NATS JetStream | Lightweight service messaging | Good latency and simplicity, but less aligned with the current team's data-stream tooling and planned consumers |
| Managed Pub/Sub or SQS/SNS | Deployment committed to a public cloud | Reduces broker operations but introduces provider coupling and local/staging differences |
| Pulsar | Very large multi-tenant retained streams | Capable but disproportionate operational complexity for the current MVP |
| Debezium CDC | Database-log-driven event publication | Can replace the custom publisher, but still requires a managed event platform and connector operations |
| MQTT broker | Direct IoT device connectivity | Appropriate as a device ingress protocol, not a full replacement for the internal durable event stream |

## Deployment decision

The non-negotiable boundary is **TimescaleDB plus Transactional Outbox**. Kafka
publication is optional per environment and may be disabled for a small pilot.
When disabled, ingestion and Outbox persistence remain available, but all
Kafka-dependent projections, quality workflows and future feature consumers are
paused.

Do not replace Kafka by adding synchronous calls from Device Service to Jeecg,
model-service or notification services. That would make hardware upload
availability depend on downstream business and AI services, violating the
local-first and durable-ingestion architecture.
