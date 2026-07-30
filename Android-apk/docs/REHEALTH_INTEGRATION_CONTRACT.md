# ReHealth Android / Backend MVP Integration Contract

Status: canonical Android contract, updated 2026-07-30.

## Runtime Boundary

```text
productCode -> single active RingRepository Provider -> Android BLE
-> Room -> durable upload queue -> JeecgBoot
JeecgBoot -> software_db / hardware_db -> LangChain4j health chat or model-service scoring
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

`RH-HB-E01` does not advertise `TEMPERATURE` after the current physical-device
measurement failed acceptance. The domain/telemetry string remains backward-compatible
for other Providers and existing rows; this changes no endpoint or DTO schema.

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
| Profile | `GET/PUT /rehealth/mobile/profile` | Authenticated, user-scoped typed profile. Preserve returned `version` on PUT; stale edits return `409`; BMI is server-derived. |
| Interview | `POST /rehealth/mobile/interviews`, `GET /rehealth/mobile/interviews/latest` | Store in the Room durable queue before leaving the result screen, retry through WorkManager, and reload the latest typed record after login/profile entry. The optional `profile` object carries parsed age/height/weight and is merged into the typed profile in the same software-db transaction. |
| Device binding | `POST /rehealth/mobile/devices/bind` | Send a stable device ID and SHA-256 address hash; never send the raw BLE MAC. |
| Telemetry | `POST /rehealth/mobile/measurements/batch` | Upload normalized Room records with a stable `batchId`; exclude raw PPG/RRI bytes. |
| Risk | `POST /rehealth/mobile/features/evaluate`, `GET /risk/latest` | Use the authenticated client and persisted server result. |
| Intervention | `POST /interventions/generate`, `GET /interventions/today` | Generate only when today's persisted plan is absent. |
| Feedback | `POST /interventions/{id}/feedback` | Mark local feedback complete only when `persisted=true`. |
| Attribution | `POST /rehealth/mobile/attribution/events` | Authenticated individual attribution only. |
| Health assistant | `POST /rehealth/mobile/agent/messages`, `GET /rehealth/mobile/agent/conversations/latest` | Persist the user message in Room before sending. `conversationId`, `clientMessageId`, and `requestId` make retries stable; restore the latest user/tenant-scoped server conversation after login. Provider credentials remain server-only. |

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
`BLOOD_GLUCOSE`, `TEMPERATURE`, `STRESS`, `MET`, `URIC_ACID`,
`TOTAL_CHOLESTEROL`, `TRIGLYCERIDES`, `HDL_CHOLESTEROL`,
`LDL_CHOLESTEROL`, `BMI`, `BODY_FAT_PERCENT`, `FAT_MASS`, `FAT_FREE_MASS`,
`MUSCLE_PERCENT`, `MUSCLE_MASS`, `SUBCUTANEOUS_FAT_PERCENT`,
`BODY_WATER_PERCENT`, `WATER_MASS`, `SKELETAL_MUSCLE_PERCENT`, `BONE_MASS`,
`PROTEIN_PERCENT`, `PROTEIN_MASS`, and `BASAL_METABOLIC_RATE`. Blood-glucose
calibration and menstrual-cycle configuration are device settings and never enter
the telemetry batch.

Android Room schema v5 extends local `ring_signal_chunks` for ECG with nullable
draw frequency, duration, lead type, vendor ECG type, calibration type, average
heart rate, and contact quality. Newly calibrated HBand curves use `FLOAT32_LE`
millivolts with `calibration_type=HBAND_ECG_UTIL_MV_V1`; v4 `INT32_LE` rows migrate
without deletion and remain relative-amplitude data. These fields and waveform bytes
are local UI/history data only and do not change the public telemetry DTO.

Android Room schema v6 adds `health_chat_messages`. It stores each authenticated
user's current conversation separately, writes the user message before network I/O,
and marks failed delivery without synthesizing an AI answer. MySQL migration
`software-V20260730.1` adds `rehealth_ai_conversation` and `rehealth_ai_message`;
MySQL is the authoritative complete history while the model prompt uses only a
bounded recent window plus freshly assembled server-authorized health context.

Feedback and device binding completion require `persisted == true`.

## Data and Privacy Rules

- Device identity is `<vendor>-<first 24 SHA-256 hex characters>` plus
  `hardwareAddressHash`; raw MAC addresses are not uploaded. Currently `<vendor>`
  is `mrd`, `rwfit`, or `hband`.
- `source=mrd_room`, `source=rwfit_room`, and `source=hband_room` identify the real Room collection
  path for the active Provider. The upload snapshot filters out rows from other
  vendors before creating a batch.
  Synthetic software QA must use `source=synthetic_qa`.
- `rawPayload`, PPG/RRI/ECG waveform bytes, access tokens, phone numbers, and direct
  identifiers must not be included in upload payloads or production logs.
- Telemetry ingest does not trigger model scoring. Risk evaluation is a separate
  canonical request after local feature extraction.
- CatBoost, SHAP, LLM, and causal attribution remain outside the Android APK.
- A failed health-assistant request remains visible as a failed local user message;
  Android must not synthesize a provider answer or claim to use cloud records offline.

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

Risk/model calls still require `rehealth.model-service.base-url`. Health chat uses
`rehealth.health-agent.engine=model-service|langchain4j`; the default remains
`model-service` for rollback safety. Enabling `langchain4j` additionally requires
`REHEALTH_LLM_BASE_URL`, `REHEALTH_LLM_MODEL`, and a provider credential supplied
through `REHEALTH_LLM_API_KEY_FILE`. Provider and internal credentials belong only
in backend runtime secrets.

## QA Status

Software-only contract, serialization, queue, repository, and APK build checks can
run without a ring. Real BLE scanning, binding, measurement accuracy, background
collection reliability, reconnect behavior, and battery impact are
`HARDWARE_QA_PENDING` until the applicable MRD/RWFit ring or HBand watch/band and
Android 13+ device have been validated. An Android test phone is available, but
the HBand wearable capability and accuracy matrix remains pending.
