# ReHealth Device Service

Independent Spring Boot 3.5.5 / Java 17 boundary for authenticated wearable
telemetry. It reuses the versioned sources from `backend/contracts/telemetry`
and exposes:

- `POST /rehealth/mobile/measurements/batch`
- `GET /rehealth/mobile/measurements/recent`
- `GET /rehealth/internal/v1/operations/status`
- `/actuator/health` and `/actuator/health/readiness`
- `/v3/api-docs`

The scaffold fails closed for identity and persistence until concrete adapters
are configured. Todo 7 provides the PostgreSQL 17 / TimescaleDB schema and
Flyway lifecycle. Todo 8 adds the telemetry-port adapter: each accepted batch,
normalized telemetry, quality/rejection records, reconciliation state, and
versioned Outbox events commit in one Timescale transaction. Duplicate replays
return the original receipt without adding rows. Kafka publication remains a
separate concern. This module does not copy Jeecg repositories or model
responsibilities.

Readiness is `OUT_OF_SERVICE` until both identity authorization and telemetry
storage report ready. The HTTP identity adapter requires
`REHEALTH_IDENTITY_BASE_URL`, `REHEALTH_IDENTITY_READINESS_URL`, an internal
service credential, and `REHEALTH_IDENTITY_ENABLED=true`. Kafka is deliberately
not a readiness dependency.

Set `REHEALTH_HARDWARE_DB_ENABLED=true` with
`REHEALTH_HARDWARE_DB_URL`, `REHEALTH_HARDWARE_DB_USERNAME`, and either
`REHEALTH_HARDWARE_DB_PASSWORD` or `REHEALTH_HARDWARE_DB_PASSWORD_FILE` to run
the Timescale migrations at startup. Retention defaults are 730 days for
normalized measurement/session telemetry, 90 days for signal metadata, 1,095
days for operational history, and 30 days for published outbox rows. Failed or
unresolved outbox work is never removed by the lifecycle job.

Run:

```bash
mvn -f backend/device-service/pom.xml test
```
