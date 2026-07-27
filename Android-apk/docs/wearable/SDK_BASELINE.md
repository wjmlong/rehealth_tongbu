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
| RWFit | `app/libs/blesdk-rwfit-release_v2_260724.aar` | `RW_SDK_V2.0.0_20260724` | 346,194 bytes | `C9C2CC91C5D8D7E1274B122D83546DB1362DA7CAD582F77F39D89B4D052D5333` | Present and included by `app/build.gradle.kts` |
| HBand / Veepoo | None | Not provided | - | - | Formal authorized package blocks HBand SDK/provider work |

The RWFit AAR was retrieved from the official `RWFitSDK/RW_Android_SDK`
repository at tag `RW_SDK_V2.0.0_20260724` (commit
`5f066e65af1aca630a30ea091909d7259e0b14da`). Its MIT license is retained at
`app/libs/RWFIT_SDK_LICENSE.txt`. No HBand AAR, JAR, ZIP, vendor demo project,
or authorization package was found.

## Device and capability evidence

| Item | Confirmed evidence | Missing evidence |
| --- | --- | --- |
| MRD model | Existing code and QA documents refer to `MR11`; the current BLE implementation requests heart rate, HRV, blood oxygen, blood pressure, temperature, stress, steps, sleep, RRI/PPG-related packets. | Procurement record, exact production model/SKU, firmware matrix, and physical-device verification of each metric |
| RWFit model | The official SDK exposes step/activity, sleep, heart rate, blood oxygen, and HRV capability flags and history callbacks. | Exact purchased model, firmware, per-model capability table, metric-unit confirmation, and physical-device connection/sync evidence |
| HBand / Veepoo model | None | Exact purchased model, firmware, formal capability table, supported metrics, and demo connection result |
| HBand password | None | Whether the purchased model requires a password and the vendor-approved default/setup flow |
| Vendor demos | None in repository | Independent scan/connect/sync evidence for each purchased physical device |

The MRD metric list above describes what the current implementation attempts;
it is not a clinical or hardware capability certification. Unsupported or
unverified metrics must remain absent rather than being emitted as zero or
simulated values.

## Gate for later phases

The single-active-device routing layer may be implemented without new SDKs.
The pinned RWFit dependency permits an isolated provider implementation against
the documented API and exact AAR signatures. Physical-device acceptance still
requires the purchased model, firmware, capability output, unit confirmation,
and scan/connect/sync evidence. HBand dependency/provider work remains blocked
until its formal SDK and device evidence are available. Vendor Bean fields and
metric mappings must not be inferred from samples or guessed.
