# ReHealth Android / Backend MVP Integration Contract

Status: canonical Android contract, updated 2026-07-27.

## Runtime Boundary

```text
productCode -> single active RingRepository Provider -> Android BLE
-> Room -> durable upload queue -> JeecgBoot
JeecgBoot -> software_db / hardware_db -> model-service
```

BLE collection is independent from network availability. Android must persist a
health record in Room before it creates an upload item. Backend or model-service
failure must not stop BLE collection.

Android keeps exactly one active wearable binding in encrypted preferences. The
Release registry contains MRD, RWFit, and HBand. HBand activation is selected by
`RH-HB-E01`; its physical-device acceptance remains pending. Switching a product
disconnects the old Provider and does not delete historical `ring_*` rows. Vendor SDK objects do not cross the
`RingRepository` boundary into UI/ViewModel/Room entities.

HBand's four required `syncPersonInfo` fields (sex, age, height, weight) are
cached in encrypted preferences under a SHA-256-derived user key so process
recovery does not depend on network availability. They are not Room telemetry,
are not logged, and do not change backend DTOs or PIAS.

This local routing change does not change endpoint paths, authentication, DTOs,
durable acknowledgement, or backend PIAS behavior.

Debug emulator base URL:

```text
http://10.0.2.2:8080/jeecg-boot/
```

Release builds require an HTTPS `REHEALTH_API_BASE_URL`. Authenticated endpoints
use the Jeecg mobile-login token in `X-Access-Token`. A `401` pauses the durable
queue until the user logs in again; the app does not invent a refresh-token flow.

## Canonical Android Endpoints

| Function | Method and path | Android behavior |
| --- | --- | --- |
| Mobile login | `POST /sys/mLogin` | Save the Jeecg token in encrypted session storage. |
| Health/config | `GET /rehealth/mobile/health`, `GET /rehealth/mobile/config` | Environment and contract diagnostics. |
| Profile | `GET/PUT /rehealth/mobile/profile` | Authenticated, user-scoped health profile. |
| Interview | `POST /rehealth/mobile/interviews`, `GET /interviews/latest` | Store locally first, then retry through WorkManager. |
| Device binding | `POST /rehealth/mobile/devices/bind` | Send a stable device ID and SHA-256 address hash; never send the raw BLE MAC. |
| Telemetry | `POST /rehealth/mobile/measurements/batch` | Upload normalized Room records with a stable `batchId`; exclude raw PPG/RRI bytes. |
| Risk | `POST /rehealth/mobile/features/evaluate`, `GET /risk/latest` | Use the authenticated client and persisted server result. |
| Intervention | `POST /interventions/generate`, `GET /interventions/today` | Generate only when today's persisted plan is absent. |
| Feedback | `POST /interventions/{id}/feedback` | Mark local feedback complete only when `persisted=true`. |
| Attribution | `POST /rehealth/mobile/attribution/events` | Authenticated individual attribution only. |
| Health assistant | `POST /rehealth/mobile/agent/messages` | Backend-proxied model access; no provider credential in the APK. |

Every durable business endpoint returns a retryable `503` envelope when the
required database is disabled or unavailable. Android must not interpret an
HTTP/Jeecg success envelope without a durable acknowledgement as completed.

Telemetry completion requires all of:

```text
accepted == true
persisted == true
status starts with "ACCEPTED_"
```

Feedback and device binding completion require `persisted == true`.

## Data and Privacy Rules

- Device identity is `<vendor>-<first 24 SHA-256 hex characters>` plus
  `hardwareAddressHash`; raw MAC addresses are not uploaded. Currently `<vendor>`
  is `mrd`, `rwfit`, or `hband`.
- `source=mrd_room`, `source=rwfit_room`, and `source=hband_room` identify the real Room collection
  path for the active Provider. The upload snapshot filters out rows from other
  vendors before creating a batch.
  Synthetic software QA must use `source=synthetic_qa`.
- `rawPayload`, PPG/RRI payload bytes, access tokens, phone numbers, and direct
  identifiers must not be included in upload payloads or production logs.
- Telemetry ingest does not trigger model scoring. Risk evaluation is a separate
  canonical request after local feature extraction.
- CatBoost, SHAP, LLM, and causal attribution remain outside the Android APK.
- Offline health-assistant fallback must be labelled as generic and must not claim
  to use the user's cloud record.

## Retired Android Paths

These prototype routes are forbidden in active Android code:

```text
POST /rehealth/mobile/ring/snapshots
GET  /rehealth/mobile/patient/mvp
GET  /rehealth/mobile/patient/profile
GET  /rehealth/mobile/patient/risk-score
GET  /rehealth/mobile/patient/intervention-plan
POST /rehealth/mobile/patient/checkins
```

## Required Backend Configuration

Apply the additive software and hardware MySQL migrations, then explicitly enable:

```yaml
rehealth:
  software-db:
    enabled: true
  hardware-db:
    enabled: true
  mobile:
    time-zone: Asia/Shanghai
```

Model and health-agent calls require `rehealth.model-service.base-url`. Provider
credentials and internal service credentials belong only in backend/model-service
runtime secrets.

## QA Status

Software-only contract, serialization, queue, repository, and APK build checks can
run without a ring. Real BLE scanning, binding, measurement accuracy, background
collection reliability, reconnect behavior, and battery impact are
`HARDWARE_QA_PENDING` until the applicable MRD/RWFit ring or HBand watch/band and
Android 13+ device have been validated. No HBand physical device is currently available.
