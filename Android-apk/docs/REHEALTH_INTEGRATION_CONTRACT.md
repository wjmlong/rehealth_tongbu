# ReHealth Android / Backend MVP Integration Contract

Status: canonical Android contract, updated 2026-07-31.

## Runtime Boundary

```text
productCode -> single active RingRepository Provider -> Android BLE
-> Room -> durable upload queue -> Gateway
Gateway -> Device Service / TimescaleDB for telemetry and diet behavior
Gateway -> JeecgBoot / software_db -> LangChain4j health chat/structured intervention
JeecgBoot -> model-service for CVD scoring; JeecgBoot -> PIAS for attribution
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
| Telemetry | `POST /rehealth/mobile/measurements/batch` | Use `telemetry-v2`; upload locally persisted normalized Room records and optional structured `dietRecords` with a stable `batchId`; exclude raw PPG/RRI and meal-image bytes. |
| Risk | `POST /rehealth/mobile/features/evaluate`, `GET /risk/latest` | Use the authenticated client and persisted server result. |
| Intervention | `POST /interventions/generate`, `GET /interventions/today` | Generate only when today's persisted plan is absent. Send only a stable `request_id`; the server reloads profile/interview/risk and tenant-scoped TimescaleDB context and returns ordered structured `items`. |
| Feedback | `POST /interventions/{id}/feedback` | Mark local feedback complete only when `persisted=true`. |
| Attribution | `POST /rehealth/mobile/attribution/events` | Authenticated individual attribution only. |
| Health assistant | `POST /rehealth/mobile/agent/messages`, `GET /rehealth/mobile/agent/conversations/latest` | Persist the user message in Room before sending. `conversationId`, `clientMessageId`, and `requestId` make retries stable; restore the latest user/tenant-scoped server conversation after login. JeecgBoot extracts only explicit self-reported name, gender, age, height and weight, merges changed values into the typed profile before assembling that turn's prompt, and appends a Chinese field-update confirmation to the persisted answer. Hypothetical or third-party values are not profile updates. Provider credentials remain server-only. |
| Photo behavior record | `POST /rehealth/mobile/behavior-records/analyze-photo`, `GET /rehealth/mobile/behavior-records/today` | Capture with the system camera into app-private cache, normalize and upload JPEG/PNG/WebP up to 4 MB with an owner-stable `requestId`, then render the persisted FOOD/OCR result on Home and Data. The upload uses a dedicated client timeout longer than the server's vision timeout; provider timeout is surfaced separately from connectivity failure. Never include a provider credential in Android. |

RHI v2 now has an authenticated, non-authoritative preview path:
`POST /rehealth/mobile/rhi/evaluate-series`. Android sends a bounded series of
versioned 32-field daily requests to JeecgBoot; JeecgBoot calls
`model-service POST /v2/rhi/evaluate` for each day and returns the ordered results.
Android must never call model-service directly. Local RHI remains the offline
default and the user must explicitly select the remote preview. This route does
not persist an authoritative cloud RHI snapshot and does not replace the future
Feature Pipeline ownership or its release gate.

Android does contain a separate local product index named RDI
(`rdi-rule-1.0.1`). It remains an additive transparent rule with Room
persistence, but it does not drive the Attribution health improvement score.
The Attribution risk value and historical line use this native 0-100 RDI index;
they do not convert or substitute CVD probability or PIAS output. Version 1.0.1
stores the verified-activity baseline in the same seven-day-minute unit used by
the current value and rejects the former clock-minute sleep-regularity anchor;
sleep regularity is compared as variability against the historical window.
Attribution also has a local RDI-native 30-day counterfactual adapter. With at
least seven valid activity, sleep, and HRV days plus an explicit supported
activity or sleep intervention, it builds separate no-action and plan inputs
from the selected 7/30/90-day personal pattern and invokes the same `RdiEngine`
for every future day. The selected-period RDI score is rendered as the horizontal
no-action reference; the native pointwise difference between both engine arms is
applied to that reference to form the plan trajectory. The D30 score difference
is the displayed expected reduction.
The `recent-personal-variability-normal-sensitivity-95-v1` interval is the
pointwise range of deterministic optimistic and conservative input-sensitivity
runs; it is a scenario range, not a disease-probability confidence interval.
Projected inputs and scores are transient and must never be persisted as Room
observations. No PIAS or CVD probability is accepted as an RDI scenario value.

That existing score and chart use the Android RHI Lite evaluator
(`rhi-deterministic-preview-2.2.0-android-lite`). It ports the governed RHI-100
preview curves, domain weights, confidence shrinkage, and display smoothing for
fields with explicit provenance from Room, a confirmed clinical report, or the
trusted user profile. Room schema 9 adds nullable sedentary time, waist,
formal VO2max, HbA1c, and eGFR inputs; schema 10 adds confirmed upper-arm cuff
seven-day BP and dated hospital-lab values. Migrations 8→9 and 9→10 are explicit
and preserve existing health records. Unsupported fields remain missing/neutral
with zero confidence; blanks are never replaced with normal values. Cuffless ring
blood pressure remains display-only. The Attribution improvement score is the
signed difference between the last and first valid RHI inside the selected
7/30/90-day window, so it represents cumulative improvement over that view rather
than a period median against one shared 90-day baseline. Its chart filters the same real RHI history
for the selected 7/30/90-day view. The current Attribution risk index uses the
native local RDI-16 0-100 score. Scenario output never replaces that current
RDI-16 value. The Model UI is unchanged.
Version 2.2.0 corrects four scoring and provenance defects and adds daily
persistence. No UI was changed.

- **Product tier gating.** `RhiProductTier` (LITE / STANDARD / CLINICAL) is
  resolved from the evidence actually extracted, regardless of whether it was
  hand-entered or synced from a device. The tier gates both which indicators
  score and which fields the confidence denominator expects. A wearable-only
  user is no longer graded against laboratory fields they were never asked to
  supply, and `total_cholesterol` is no longer counted twice, so an
  evidence-complete user can now actually reach a high confidence grade.
- **MVPA baseline scale.** The personal-improvement baseline for
  `mvpa_minutes_7d` is now a distribution of rolling seven-day totals over the
  trailing 8–28 day offsets, matching the scale of the current value. It
  previously compared a seven-day total against single-day values, which
  systematically suppressed the personal-improvement term.
- **Steps coverage.** `steps_7d_mean` always divides by seven. Unworn days count
  as zero exposure rather than being dropped from the average, so a single
  10,000-step day no longer yields a full activity score.
- **Quality warnings.** `RhiQualityWarning` surfaces deterministic
  data-integrity findings that never alter the score:
  `activity_duration_missing` (steps recorded but zero exercise minutes),
  `wear_time_incomplete`, `blood_pressure_unavailable`, and `steps_all_zero`.

Room schema 14 splits RHI daily persistence into four tables:
`rhi_daily_health_index`, `rhi_daily_domain_score`,
`rhi_daily_feature_snapshot`, and `rhi_data_quality_snapshot`. Migration 13→14
is additive; no existing table is altered or dropped. Rows are keyed by
`(user_id, scored_on)` and a recomputation replaces a day atomically rather than
appending a duplicate. An excluded domain is stored as `NULL`, never as a
neutral 50, so a gap is never recorded as a measurement. Persistence is a side
effect of scoring and never a precondition for it. `delta_7d` and `delta_28d`
are measured against a fixed lookback in the stored series, so a 7-day and a
90-day view report the same seven-day change.
Guest/local-device calculations use the `__local_device__` Room key so Debug
7/30/90-day validation has durable rows; those rows are not placed on the
authenticated upload queue.

Android RHI field provenance is fixed as follows:

| Source | RHI fields |
| --- | --- |
| Trusted profile | age, biological sex, BMI fallback, nicotine exposure, diabetes status, medication flags, family history |
| Health archive manual input | sedentary hours, waist, formal VO₂max, HbA1c, eGFR |
| Confirmed upper-arm cuff | seven-day SBP and DBP mean plus 3–7 valid-day count |
| Confirmed dated hospital report | fasting glucose, TC, LDL-C, HDL-C, TG |
| Room wearable/activity/sleep | resting HR/HRV levels and changes, steps, MVPA, activity regularity, sleep duration/regularity/efficiency, nocturnal SpO₂ burden |
| Room body composition/feedback | 28-day weight trend and composite adherence |

TC, eGFR, age, sex, diabetes, medication, and family-history fields increase
clinical-profile completeness but do not directly penalize the daily RHI.

The Attribution 16-factor card is a third, explicitly separated view. It renders
server-owned `factor_contributions` with rule version `factor16-rule-v1.0.0`;
it is not RDI16 and does not replace the CVD probability or PIAS causal output.
`feature_contributions` remains the model/SHAP field. The values shown beside the
16 rows come from the exact local vector sent in the same evaluation request.
Factor16 confirmation is keyed by its own rule version, so a deterministic
Factor16 result may be shown even when `is_mock=true` for the separate CVD scorer;
the mock CVD risk score itself remains hidden.
For the explicit Debug mock-wearable QA flow only, Android mirrors the same
transparent V1.0 Factor16 display rule after attempting the real evaluation request
when the response does not contain all 16 versioned factor contributions or the
request is unavailable. That fallback is
identified as `factor16-rule-v1.0.0-debug-mock`, is never persisted as a model
result, and has a release-source-set no-op implementation. A complete server
Factor16 response always takes precedence.
The Debug mock replay still uses the normal app inputs: the encrypted local
profile cache, Room clinical input, and Room activity data. The profile cache
retains BMI plus smoking, drinking, diabetes, hypertension, and family-history
fields so a remote profile outage does not erase the explicit mock fixture.
Upper-arm cuff seven-day means require 3–7 valid days and explicit confirmation;
cuffless wearable BP remains visible on Data but is excluded from this vector.
Dated, user-confirmed hospital labs feed the five metabolic fields. Blood-pressure
and lab cards apply the 80/20 display split; absent verified longitudinal
control-support data stays zero and is never imputed.

The Data UI follows the same separation. Its risk card is labeled RDI-16 and reuses
the existing CVD-16 feature-evaluation path with the clinical BP source gate above. It renders
a score only when the response is reachable, finite, in `[0, 1]`, and explicitly
`isMock=false`; mock or failed output remains unavailable. Its health-index ring renders Android RHI Lite:
Today/7-day selections use the current seven-day RHI, while 30/90-day selections
use the valid-day median and the same 7/14-day minimums.

The full synthetic-chain entry remains the confirmation-gated Debug action under
Profile -> Device binding. The Debug mock provider seeds 118 days (90 visible
days plus 28 warm-up days) of clearly marked synthetic records to Room
before invoking real device binding, durable telemetry upload, local/remote RHI,
90 daily local RDI-16 evaluations, and PIAS. The Release source-set factory is a no-op.
No other screen may generate this fixture. `quality.rawSignalExcluded=true`
declares that raw PPG/RRI samples are absent and is valid control metadata; actual
raw payload keys and signal chunks remain rejected while raw upload is disabled.

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

Android Room schema v7 adds `health_chat_conversations`. Migration 6→7 derives one
conversation row per existing user/conversation without deleting v6 messages, and
marks the most recently updated conversation active. Android can create, switch,
locally delete, and locally clear conversations. Local deletion retains a tombstone
so the existing `conversations/latest` refresh does not immediately restore a
deleted cache entry. It does not delete authoritative MySQL history: the public
backend contract still exposes only latest-conversation restore and has no list or
delete endpoint.

Android Room schema v8 adds `rdi_daily_snapshots` and
`rdi_contribution_records`. Migration 7→8 creates only these tables and their
indices, preserving all prior records. A snapshot stores raw/display scores,
confidence, status, date and algorithm version; each contribution stores the
source, current/baseline values, unit, raw points, confidence-adjusted points,
evidence text, source-factor ID and version. These tables are local-only and are
not part of telemetry, public mobile DTOs, PIAS, or clinical-risk persistence.

Feedback and device binding completion require `persisted == true`.

`telemetry-v2` keeps the existing measurement, sleep and activity arrays and
adds `dietRecords`. A diet record has a stable `id`, `consumedAt`,
`mealType` (`breakfast`, `lunch`, `dinner`, or `snack`), a bounded
`description`, and optional non-negative calories, protein, carbohydrate, fat,
fiber, sodium and source. The response adds `dietRecordCount`. A meal must be
saved locally before upload; raw food photos are not part of this contract.

Android Room schema v11 adds the user-scoped `diet_records` table. Migration
10→11 is explicit, preserves all existing health data, and is covered by
`DietRecordMigrationTest`. Saving a meal commits the Room row before creating
its durable `telemetry_batch` queue item; missing device identity leaves the
row locally pending instead of dropping it.

`InterventionGenerateResponseDto.items` contains 1–5 actions. Each action has
`id`, `category`, `title`, `action`, `rationale`, `target`, `timing`,
`priority`, and `evidenceRefs`. Android sorts by priority and renders these
items in the personalized-plan card while preserving `plan_id` for feedback.
The server may also return `summary`, `focus_date`, `context_version`,
`context_generated_at`, and `latest_data_at`. A failed generation is not
replaced with client-side medical or mock advice.

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

Photo analysis additionally requires `REHEALTH_VISION_ENABLED=true`,
`REHEALTH_VISION_BASE_URL`, `REHEALTH_VISION_MODEL`, and a provider credential supplied
through `REHEALTH_VISION_API_KEY_FILE`. The vision model must accept image input and
return text. `REHEALTH_VISION_TIMEOUT_SECONDS` defaults to 75 seconds; JeecgBoot makes one
provider request per analysis attempt and does not automatically repeat an expensive image
inference after timeout. The secret file is mounted only into JeecgBoot and must never be copied
to Android resources, Gradle properties, tracked YAML, or Git.

## QA Status

Software-only contract, serialization, queue, repository, and APK build checks can
run without a ring. Real BLE scanning, binding, measurement accuracy, background
collection reliability, reconnect behavior, and battery impact are
`HARDWARE_QA_PENDING` until the applicable MRD/RWFit ring or HBand watch/band and
Android 13+ device have been validated. An Android test phone is available, but
the HBand wearable capability and accuracy matrix remains pending.
