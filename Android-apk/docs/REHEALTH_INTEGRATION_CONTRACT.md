# ReHealth Android / Backend MVP Integration Contract

Status: canonical Android contract, updated 2026-07-28.

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

Debug default base URL (committed `gradle.properties` for internal testing):

```text
https://rehealth.youngjimmy.store/jeecg-boot/
```

Local emulator dev (no tunnel) override:

```text
http://10.0.2.2:8080/jeecg-boot/
```

Release builds require an HTTPS `REHEALTH_API_BASE_URL`. Authenticated endpoints
use the Jeecg mobile-login token in `X-Access-Token`. A `401` pauses the durable
queue until the user logs in again; the app does not invent a refresh-token flow.

## Canonical Android Endpoints

| Function | Method and path | Android behavior |
| --- | --- | --- |
| Registration SMS | `POST /sys/sms` | Pre-auth request with `X-Sign` and `X-Timestamp`. Local `JEECG_SMS_DEV_MODE=true` stores fixed code `123456` without calling the SMS Provider. |
| Account registration | `POST /sys/user/register` | Submit phone, six-digit SMS code and password, then perform mobile login on success. |
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

The fixed registration code and Jeecg development signature default are Debug/local
behavior only. Release does not contain either value. Production keeps random codes
and the real SMS Provider, and must use a reviewed mobile-safe signing/attestation
strategy rather than embedding a production shared secret in the APK.

Telemetry completion requires all of:

```text
accepted == true
persisted == true
status starts with "ACCEPTED_"
```

HBand advanced-health measurements use independent normalized `metricType` values:
`URIC_ACID`, `TOTAL_CHOLESTEROL`, `TRIGLYCERIDES`, `HDL_CHOLESTEROL`,
`LDL_CHOLESTEROL`, `BMI`, `BODY_FAT_PERCENT`, `FAT_MASS`, `FAT_FREE_MASS`,
`MUSCLE_PERCENT`, `MUSCLE_MASS`, `SUBCUTANEOUS_FAT_PERCENT`,
`BODY_WATER_PERCENT`, `WATER_MASS`, `SKELETAL_MUSCLE_PERCENT`, `BONE_MASS`,
`PROTEIN_PERCENT`, `PROTEIN_MASS`, and `BASAL_METABOLIC_RATE`. Blood-glucose
calibration and menstrual-cycle configuration are device settings and never enter
the telemetry batch.

Feedback and device binding completion require `persisted == true`.

## Miwi 4G Cloud Watch (S8)

The Miwi/云米 4G watch family (S8/S9/GS20/GS17/A67/K9L) does not use phone BLE.
It is a **second transport (vendor-cloud pull / push), not a second business
system**: all S8 data enters the *same* `HardwareIngestionPort` pipeline as the
mobile BLE batch, distinguished only by `source`/`deviceId`. App vendor enum is
`MIWI4G`, product code `RH-S8-4G01`.

```text
Watch --4G--> Miwi cloud
            ├─ OpenAPI pull (PRIMARY, 8 月初): backend polls bytime endpoints on a
            │   per-(device,metric) cursor; auth via Authorization: <AccessToken>.
            └─ HTTP push (realtime supplement): POST /rehealth/miwi/push?token=<secret>
Both paths -> resolve user by deviceId -> HardwareIngestionPort (same pipeline)
```

Contract points:

- Binding uses the standard `POST /rehealth/mobile/devices/bind` with
  `deviceId = "miwi4g-" + first 24 SHA-256 hex chars of the IMEI`; the raw IMEI
  itself is never uploaded (address hash rule identical to BLE vendors). The pull
  path recomputes the same deviceId from the IMEI used to query the vendor API.
- Pull (`rehealth.miwi.pull.enabled=true`) queries
  `/api/heartrate|bloodpressure|bloodoxygen|temperature/get_*_bytime` and
  `/api/steps/get_steps_bytime` on `https://openapi.miwitracker.com`, token from
  `/api/token/get_token` (MD5). Mapped metric types: `HEART_RATE`, `BLOOD_PRESSURE`
  (systolic=primary, diastolic=secondary), `BLOOD_OXYGEN`, `BODY_TEMPERATURE`,
  `STEPS`, with `source=S8_CLOUD_PULL` (transport=VENDOR_CLOUD_PULL) and UTC epoch
  millis timestamps. Each (device, metric) owns an independent sync cursor; a
  deterministic `client_record_id` makes overlapping-window re-pulls idempotent.
- Push body is `{"DataType":"Health","ResultData":"<escaped JSON>"}`;
  `ResultData` is parsed as a second JSON document, `source=MIWI_4G_CLOUD`. The
  vendor protocol has no signature; the callback therefore requires the
  pre-shared `?token=` matching `rehealth.miwi.callback-token` and returns 401
  otherwise. Unbound-device pushes are acked (`code=1`) and skipped.
- ECG waveform, blood glucose, and raw-PPG interfaces are not provided by the
  current vendor API version (V1.6.x). L16 direct-TCP is a phase-2 gateway, not
  wired into JeecgBoot. See `Android-apk/docs/wearable/MIWI_4G_WATCH.md` for the
  vendor confirmation list and the S8/L16 decision.

## Data and Privacy Rules

- Device identity is `<vendor>-<first 24 SHA-256 hex characters>` plus
  `hardwareAddressHash`; raw MAC addresses are not uploaded. Currently `<vendor>`
  is `mrd`, `rwfit`, `hband`, or `miwi4g` (for `miwi4g` the hashed identifier is
  the watch IMEI instead of a BLE MAC).
- `source=mrd_room`, `source=rwfit_room`, and `source=hband_room` identify the real Room collection
  path for the active Provider. The upload snapshot filters out rows from other
  vendors before creating a batch.
  Synthetic software QA must use `source=synthetic_qa`.
- `rawPayload`, PPG/RRI/ECG waveform bytes, access tokens, phone numbers, and direct
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
  miwi:                      # only when onboarding Miwi 4G cloud watches (S8 etc.)
    enabled: true
    app-id: <vendor-assigned>
    app-key: <vendor-assigned>
    api-base-url: <vendor-assigned>
    callback-token: <random secret shared with vendor callback URL>
```

Model and health-agent calls require `rehealth.model-service.base-url`. Provider
credentials and internal service credentials belong only in backend/model-service
runtime secrets.

## QA Status

Software-only contract, serialization, queue, repository, and APK build checks can
run without a ring. Real BLE scanning, binding, measurement accuracy, background
collection reliability, reconnect behavior, and battery impact are
`HARDWARE_QA_PENDING` until the applicable MRD/RWFit ring or HBand watch/band and
Android 13+ device have been validated. An Android test phone is available, but
the HBand wearable capability and accuracy matrix remains pending.
