# Wearable SDK baseline

> The original build baseline was checked on 2026-07-27 at Git commit
> `49aa569d8db45dbb70550a5a2e9d45a0537f0599`. SDK inventory and Provider scope
> were updated on 2026-07-27 as the staged integration progressed.

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
| HBand / Veepoo | `app/libs/vpbluetooth-1.20.aar` | `1.20` | 383,711 bytes | `26D7037238D18A28AC373A511B7A2ABDFAC2A405E01564F90A89C926B5B48BD8` | Present and included by `app/build.gradle.kts` |
| HBand / Veepoo | `app/libs/vpprotocol-2.3.73.15.aar` | `2.3.73.15` | 5,631,585 bytes | `A2B3B2BA6460FC69808A210867039181238A9CC08E2980B0A3AF9DFE85B5BED1` | Present and included by `app/build.gradle.kts` |
| HBand / ECG JNI | `app/src/main/jniLibs/arm64-v8a/libnative-lib.so` | pinned with `2.3.73.15` | 2,011,512 bytes | `8E084901E2F911DA5BFECC64CD73248A81C3CC463ABB2B9B726C9B9123399273` | Required by `startDetectECG`; packaged for physical Android devices |
| HBand / ECG JNI | `app/src/main/jniLibs/armeabi-v7a/libnative-lib.so` | pinned with `2.3.73.15` | 1,224,620 bytes | `F1C3EEAF8CBB87E005038C7A981CD16EB9B5E5E50D6E3B06EA8EF93A1624C52A` | Required by `startDetectECG`; packaged for 32-bit ARM devices |
| HBand / ECG JNI | `app/src/main/jniLibs/x86/libnative-lib.so` | pinned with `2.3.73.15` | 1,642,544 bytes | `1640BDA208DFD452541B3E348404CC48425B4E2456C6A7E02ADE4831BF2D532A` | Required by `startDetectECG`; packaged for x86 test environments |
| HBand / ECG JNI | `app/src/main/jniLibs/x86_64/libnative-lib.so` | pinned with `2.3.73.15` | 1,889,552 bytes | `EAC89514653A8134A1A7BA976E6F029B66735054CA70D1EF348E5B3846464816` | Required by `startDetectECG`; packaged for x86_64 test environments |
| HBand / JieLi support | `app/libs/jl_bt_ota_V1.10.0_10931-release.aar` | `1.10.0_10931` | 264,341 bytes | `61764E43650862637C90FE7AD603A4FE948A0724BE5508C301123D0013AB8AA8` | Required by core SDK class signatures; OTA is not exposed or invoked |
| HBand / JieLi support | `app/libs/jl_rcsp_V0.7.2_527-release.aar` | `0.7.2_527` | 584,916 bytes | `0CBB1D46BCFDA8F6D2B7A68D805C88DC4F543A4890FC2537E9AA76D0F93857B2` | Required by core SDK authentication signatures; dial APIs are not exposed or invoked |
| HBand / JieLi support | `app/libs/JL_Watch_V1.13.1_11214-release.aar` | `1.13.1_11214` | 1,189,801 bytes | `7B63DE70139AE92AF67E74FEFFEDC044A383CDDA31914344EB3318BF41D5DE6E` | Supplies `WatchOpImpl`, which `vpbluetooth` loads while initializing `JLOTAManager`; watch/dial APIs are not exposed |
| HBand / Nordic support | `no.nordicsemi.android:mcumgr-core` | `2.7.4` | 298,136 bytes | `F5289D3E95391A0F4BAA63B668C9CBD71290E9AD024D22C0033534ADBC70C97E` | Official required dependency; packaged because the connection callback initializes MCU Manager |
| HBand / Nordic support | `no.nordicsemi.android:mcumgr-ble` | `2.7.4` | 43,217 bytes | `6D1D7DF7FDA871021A6678963C7819143D4FEE232DC20FB5C237B61D233F70C9` | Supplies `McuMgrBleTransport`; ReHealth exposes no OTA entry point |
| HBand / Nordic support | `no.nordicsemi.android.support.v18:scanner` | `1.4.2` | 64,155 bytes | `9D25340AB32E2E89ECE25C7472C6F2E6DED95B942B836B0B10652129BF30B178` | Official required scanner compatibility dependency |

The RWFit AAR was retrieved from the official `RWFitSDK/RW_Android_SDK`
repository at tag `RW_SDK_V2.0.0_20260724` (commit
`5f066e65af1aca630a30ea091909d7259e0b14da`). Its MIT license is retained at
`app/libs/RWFIT_SDK_LICENSE.txt`.

The HBand/Veepoo core AARs were retrieved from the user-selected official
`HBandSDK/Android_Ble_SDK` repository at commit
`f572723a3e9476179344fee86d0d99f7ad0e6d07`. The repository license is retained
at `app/libs/HBAND_SDK_LICENSE.txt`. The app reuses Gson 2.11.0 and adds the
core protocol, Bluetooth, LocalBroadcastManager, and mandatory Nordic runtime
dependencies. Release R8
identified `jl_bt_ota` and `jl_rcsp` class-signature dependencies; the first
physical-device run then proved that `vpbluetooth` also instantiates
`JLOTAManager`, which hard-loads `WatchOpImpl`. The matching `JL_Watch` artifact
is therefore retained while its watch/dial APIs remain unexposed. The next
physical-device connection proved that `VPOperateManager` eagerly initializes
`McuMgrOtaManager`, so MCU Manager and scanner dependencies are packaged even
though ReHealth exposes no OTA workflow. The same pinned commit supplies the four
`libnative-lib.so` ABI variants. The
`vpprotocol` AAR does not embed them, while `startDetectECG` loads the JNI library;
they are therefore version-locked to the AAR and must be replaced together on any SDK upgrade.
Image conversion, Goodix, contacts,
and audio components remain excluded from the health-data integration. The upstream
README also describes use as limited to cooperative customers, so commercial
authorization remains a release gate even though the public repository contains
an Apache-2.0 license file.

When the Provider references `VPOperateManager`, Release R8 also sees method
signatures for optional JieLi bitmap/FAT dial helpers. The absent bitmap helper
is suppressed in `proguard-rules.pro`; its library remains excluded and no
ReHealth code invokes that feature API. Nordic MCU Manager is not suppressed
because the vendor connection callback loads it at runtime.

## Device and capability evidence

| Item | Confirmed evidence | Missing evidence |
| --- | --- | --- |
| MRD model | Existing code and QA documents refer to `MR11`; the current BLE implementation requests heart rate, HRV, blood oxygen, blood pressure, temperature, stress, steps, sleep, RRI/PPG-related packets. | Procurement record, exact production model/SKU, firmware matrix, and physical-device verification of each metric |
| RWFit model | The official SDK exposes step/activity, sleep, heart rate, blood oxygen, and HRV capability flags and history callbacks. | Exact purchased model, firmware, per-model capability table, metric-unit confirmation, and physical-device connection/sync evidence |
| HBand / Veepoo model | Official SDK API and AAR signatures only | Exact purchased model, firmware, formal capability table, supported metrics, and demo connection result |
| HBand password | Official SDK demo calls `confirmDevicePwd(..., "0000", true)` | Whether the purchased model accepts that default and the vendor-approved setup/reset flow |
| Vendor demos | None in repository | Independent scan/connect/sync evidence for each purchased physical device |

The MRD metric list above describes what the current implementation attempts;
it is not a clinical or hardware capability certification. Unsupported or
unverified metrics must remain absent rather than being emitted as zero or
simulated values.

## Gate for later phases

The single-active-device routing layer may be implemented without new SDKs.
The pinned RWFit and HBand dependencies permit isolated Provider implementations
against documented APIs and exact AAR signatures. Physical-device acceptance
still requires each purchased model, firmware, capability output, unit
confirmation, and scan/connect/sync evidence. HBand has entered initial physical-
device integration, but real-device acceptance and release remain blocked until
the complete matrix passes. Vendor Bean fields and metric mappings must not be
inferred or guessed beyond the pinned SDK contract.

RWFit Provider scope is limited to capabilities reported by the connected device:
step/activity, sleep, heart rate, blood oxygen, and HRV. The SDK documentation does
not state the HRV unit, so Android persists the real integer with unit
`rwfit_raw`; it does not claim milliseconds. Blood pressure, temperature, stress,
blood sugar, PPG, and other SDK callbacks are not requested or persisted in this
phase.

The HBand Provider follows `connectDevice -> Notify -> confirmDevicePwd ->
settled aggregate/DeviceFunctionPackage capability merge -> syncPersonInfo -> READY`.
The SDK marks `onFunctionSupportDataChange` deprecated and documents that it may fire
multiple times before all fields are initialized, so numbered packages override the
latest aggregate value for their fields. In particular, MT116 ECG and app-HRV are read
from `DeviceFunctionPackage2`; the aggregate callback remains a compatibility fallback.
Its product intersection
allows heart rate, daily steps/activity, sleep, blood oxygen, app HRV, blood pressure,
blood glucose, stress, MET, ECG, blood component, and body composition
operations. Blood-glucose calibration
and menstrual-cycle settings use separate feature operations. The settled runtime capability
reports remain authoritative: unsupported actions remain disabled and are not
requested or persisted. ECG is a product requirement for `RH-HB-E01`; a device
that does not report ECG support is rejected instead of silently degrading. The user profile comes from ReHealth profile
data; the SDK demo's fixed sex/age/height/weight values are not used. Blood
pressure history and manual detection persist systolic/diastolic mmHg values.
Manual ECG persists the SDK waveform as a local Room signal chunk and an average
heart-rate summary; normal ECG completion is finalized from `EcgDetectState` because
the diagnosis-result callback is conditional. Historical ECG and body-composition callbacks are also normalized
into the same Room entities. The waveform is explicitly excluded from cloud upload and
is not interpreted as a diagnosis. Blood oxygen and HRV manual results use `%` and
`ms`. Blood components are stored as five independent measurements; uric-acid and
blood-fat units are read from `CustomSettingData` before measurement. Body composition
is stored as 14 independent measurements with the SDK-documented units. Capability-
gated manual-history reads preserve the device blood-glucose unit, store stress as a
`0..100` score, and store metabolic equivalent
as `MET`; direct measurement uses the corresponding pinned SDK operations. Blood-glucose
calibration and menstrual-cycle settings do not create measurement rows. Pregnancy/
preparation/mother modes, TCM, OTA, dials, and messaging remain outside `RH-HB-E01`.
Temperature also remains outside `RH-HB-E01` after the current purchased device failed
physical measurement verification. Sleep and origin history are serialized as dedicated
`readSleepData` then `readOriginData` commands because the purchased device returned origin
records but omitted sleep from `readAllHealthData`; five-minute step records are aggregated per local day.
`SportUtil.getDistance()` divides metre-scale step distance by 1000, so the
Provider converts the SDK kilometre value back to Room `distanceMeters`.
