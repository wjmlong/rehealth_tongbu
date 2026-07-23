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
are configured. Todo 7 replaces the unavailable telemetry port with the
TimescaleDB implementation; this module does not copy Jeecg repositories or
model responsibilities.

Readiness is `OUT_OF_SERVICE` until both identity authorization and telemetry
storage report ready. The HTTP identity adapter requires
`REHEALTH_IDENTITY_BASE_URL`, `REHEALTH_IDENTITY_READINESS_URL`, an internal
service credential, and `REHEALTH_IDENTITY_ENABLED=true`. Kafka is deliberately
not a readiness dependency.

Run:

```bash
mvn -f backend/device-service/pom.xml test
```
