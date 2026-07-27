# HBand physical-device QA

Status: **HARDWARE_QA_PENDING**. No HBand watch/band is currently available.
Automated tests and APK builds are not evidence of physical compatibility.

## Build the dedicated APK

From `Android-apk`:

```powershell
.\gradlew.bat "-Prehealth.debug.wearable.product.code=RH-HB-E01" testDebugUnitTest assembleDebug
```

For Android Studio Run, place this only in the untracked `local.properties`:

```properties
rehealth.debug.wearable.product.code=RH-HB-E01
```

Install `app/build/outputs/apk/debug/app-debug.apk`. Confirm the app activates
only `RH-HB-E01`/`HBAND`; it must not scan or connect MRD/RWFit concurrently.

## Evidence to record before testing

- purchased product name, SKU, exact model and firmware;
- Android model/version and app commit;
- vendor cooperation/commercial authorization;
- vendor-approved password setup/reset flow (the pinned official demo uses
  `0000`, but this has not been confirmed on a purchased device);
- complete `FunctionDeviceSupportData` result without logging raw health values
  or the BLE MAC in release logs.

## Connection sequence

1. Grant Bluetooth scan/connect and notification permissions.
2. Scan and confirm only SDK-recognized VeePoo/HBand candidates are shown.
3. Record the advertised device name separately from screenshots containing a
   full MAC; redact addresses before sharing evidence.
4. Connect and verify the sequence is BLE connection, Notify success, password
   confirmation, capability callback, real ReHealth profile synchronization,
   then `CONNECTED`/READY.
5. Test wrong password/confirmation timeout and verify the app reports an error,
   writes no telemetry, and can recover after disconnect/retry.
6. Remove one required profile field (sex, age, height, or weight) and verify
   connection is blocked before the SDK receives Demo demographic values.
7. Restart the app and verify it reconnects only the encrypted bound address.

## Data acceptance

For `RH-HB-E01`, validate only:

- heart-rate history and manual heart-rate measurement (`bpm`);
- daily steps/activity, including confirmation that SDK distance in kilometres
  is converted to Room metres and calories remain kcal;
- sleep start/end, deep/light duration, cross-midnight handling, and the
  documented absence of a separate REM field in the selected SDK callback.

Compare ten repeated syncs against the vendor app. Verify deterministic IDs
prevent duplicate Room rows. Unsupported, zero, invalid, or absent readings must
not produce measurements. Blood oxygen/HRV are excluded by the current product
profile even if the SDK reports those functions. Do not test or enable blood
pressure, temperature, stress, blood glucose, uric acid, blood lipids, ECG,
body composition, TCM, OTA, dials, messages, contacts, music, or audio.

## Failure and background matrix

- Android 8/9, 12, and 14/15 where devices are available;
- permissions denied, Bluetooth off, device out of range, and password timeout;
- app foreground/background, screen locked, process killed, and phone reboot;
- foreground manual sync overlapping scheduled collection (commands must remain
  serialized);
- offline collection followed by network recovery;
- 10 consecutive syncs and a 24-hour power/temperature run;
- device clock/time-zone error and a sleep session spanning midnight.

Do not mark HBand accepted until every applicable item has device-backed logs,
screenshots, Room row inspection, model/firmware details, and comparison results.
