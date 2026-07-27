# Wearable SDK baseline

> Checked on 2026-07-27 at Git commit
> `49aa569d8db45dbb70550a5a2e9d45a0537f0599` before provider-routing changes.

This baseline records only material that is present in the repository or has
been verified by a build. It must not be treated as proof that a physical
device or an unprovided vendor SDK works.

## Build baseline

Environment:

- Windows 11 amd64
- Gradle 8.9
- JDK 17.0.19
- Android `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`

Commands run from `Android-apk`:

| Command | Result | Duration | Notes |
| --- | --- | --- | --- |
| `.\gradlew.bat testDebugUnitTest` | PASS | 1m 02s | Existing unit-test suite passed; existing Android/API deprecation warnings remain. |
| `.\gradlew.bat assembleDebug` | PASS | 19s | Debug APK assembled successfully. |

## SDK material inventory

| Vendor | Repository material | Version | Size | SHA-256 | Status |
| --- | --- | --- | ---: | --- | --- |
| MRD | `app/libs/sdk_mrd2026_1.3.0.aar` | `1.3.0` from the file name | 1,012,537 bytes | `0A4D5F171C18AB0CFBCE0965704F571A1A8B33F0E87006E52B33BB5568D4D576` | Present and included by `app/build.gradle.kts` |
| RWFit | None | Not provided | - | - | Blocks RWFit SDK/provider work |
| HBand / Veepoo | None | Not provided | - | - | Formal authorized package blocks HBand SDK/provider work |

Only the MRD AAR was found outside generated build directories. No RWFit or
HBand AAR, JAR, ZIP, vendor demo project, or authorization package was found.

## Device and capability evidence

| Item | Confirmed evidence | Missing evidence |
| --- | --- | --- |
| MRD model | Existing code and QA documents refer to `MR11`; the current BLE implementation requests heart rate, HRV, blood oxygen, blood pressure, temperature, stress, steps, sleep, RRI/PPG-related packets. | Procurement record, exact production model/SKU, firmware matrix, and physical-device verification of each metric |
| RWFit model | None | Exact purchased model, firmware, formal capability table, supported metrics, and demo connection result |
| HBand / Veepoo model | None | Exact purchased model, firmware, formal capability table, supported metrics, and demo connection result |
| HBand password | None | Whether the purchased model requires a password and the vendor-approved default/setup flow |
| Vendor demos | None in repository | Independent scan/connect/sync evidence for each purchased physical device |

The MRD metric list above describes what the current implementation attempts;
it is not a clinical or hardware capability certification. Unsupported or
unverified metrics must remain absent rather than being emitted as zero or
simulated values.

## Gate for later phases

The single-active-device routing layer may be implemented without new SDKs.
RWFit/HBand dependency or provider phases must not start until their formal SDK
packages, purchased model identifiers, capability tables, required credentials,
and vendor-demo real-device connection evidence are available. Vendor Bean
fields and metric mappings must not be inferred from samples or guessed.
