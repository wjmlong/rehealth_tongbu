# ReHealth MVP Integration Contract

## Android to Backend

Base URL for emulator debug builds:

```text
http://10.0.2.2:8080/jeecg-boot
```

Current ring endpoint:

```http
POST /rehealth/mobile/ring/snapshots
```

Authenticated individual attribution uses:

```http
POST /rehealth/mobile/attribution/events
X-Access-Token: <Jeecg mobile token>
```

The app supplies confirmed local Room risk history as `risk_history` until backend
attribution persistence is implemented. Release builds do not call PIAS directly and
do not permit cleartext transport; local debug builds may use emulator HTTP for Jeecg.

The Android app sends the latest Room snapshot after a successful ring sync. Signal payload bytes are not uploaded in the MVP; only metadata is sent.

Main payload fields:

- `collectedAt`: client collection timestamp in milliseconds.
- `trigger`: `manual_sync` or `auto_collection`.
- `device`: ring address/name/RSSI.
- `measurements`: heart rate, HRV, SpO2, blood pressure, temperature, steps, stress.
- `sleep`: latest sleep session.
- `activity`: latest activity summary.
- `signals`: RRI/PPG metadata only.

Patient MVP endpoints:

```http
GET  /rehealth/mobile/patient/mvp
GET  /rehealth/mobile/patient/profile
POST /rehealth/mobile/patient/profile
GET  /rehealth/mobile/patient/risk-score
GET  /rehealth/mobile/patient/intervention-plan
POST /rehealth/mobile/patient/checkins
```

`/patient/mvp` is the preferred Android entry point. It returns one BFF payload with:

- `profile`: patient health profile and risk factors.
- `risk`: latest algorithm/mock risk result.
- `interventionPlan`: active patient actions.
- `recentCheckins`: feedback/check-in history.

Android should call independent endpoints only when editing profile or submitting check-ins.

## Backend to Algorithm

Backend can run in two modes:

- `mock`: no algorithm service configured; returns a local risk summary for app development.
- `http`: set `rehealth.algorithm.base-url` to a running PIAS/FastAPI service, then backend calls `/api/pias/predict`.

The PIAS model still requires clinical/profile fields such as glucose and lipids. Ring data should not be forced into those fields. For MVP, backend uses ring data as context, health profile values where available, and placeholders for lab fields until report/profile ingestion is built.

## Near-Term TODO

- Persist snapshots to MySQL instead of in-memory storage.
- Add authenticated `/api/mobile/**` path with JWT.
- Add report upload/OCR/profile fields for PIAS required inputs.
- Add Android retry queue with upload status per snapshot.
- Add production HTTPS base URL and remove cleartext traffic.
