# HBand physical-device QA

Status: **HARDWARE_QA_PENDING**. A physical device has entered initial integration,
but the complete connection, capability, data, and background QA matrix is not accepted.
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
2. Open the personal-profile editor, select sex, and enter an age in `1..120`,
   height in `50..250 cm`, and weight in `10..300 kg`; save and refresh the profile.
3. Scan and confirm only SDK-recognized VeePoo/HBand candidates are shown.
4. Record the advertised device name separately from screenshots containing a
   full MAC; redact addresses before sharing evidence.
5. Connect and verify the sequence is BLE connection, Notify success, password
   confirmation, capability callback, real ReHealth profile synchronization,
   then `CONNECTED`/READY.
   Confirm logcat contains no `NoClassDefFoundError` for `WatchOpImpl`,
   `McuMgrBleTransport`, or Nordic scanner classes, and that a full APK install,
   rather than Apply Changes, was used after SDK dependency changes.
6. Test wrong password/confirmation timeout and verify the app reports an error,
   writes no telemetry, and can recover after disconnect/retry.
7. Remove one required profile field (sex, age, height, or weight) and verify
   connection is blocked before the SDK receives Demo demographic values.
8. Restart the app and verify it reconnects only the encrypted bound address.

## Data acceptance

For `RH-HB-E01`, validate only device-advertised capabilities:

- heart-rate history and manual heart-rate measurement (`bpm`);
- daily steps/activity, including confirmation that SDK distance in kilometres
  is converted to Room metres and calories remain kcal;
- sleep start/end, deep/light duration, cross-midnight handling, and the
  documented absence of a separate REM field in the selected SDK callback;
- manual blood oxygen only when `getSpo2H()` is true; verify a real `%` value and
  wear-off/failure behavior;
- manual HRV only when `getHrvAppDetectFunction()` is true; verify the SDK integer
  is persisted as `ms` and compare repeated values with the vendor app;
- blood-pressure history and manual measurement only when `getBp()` is true;
  verify systolic/diastolic order, `mmHg` units, wear-off/charging/low-battery
  failures, and compare repeated readings with a validated cuff without making
  diagnostic claims;
- manual ECG only when `getEcg()` is true; verify contact/wear guidance, start/
  stop/cancel behavior, SDK sample rate, local waveform persistence, and the
  average-heart-rate summary. Confirm raw ECG waveform bytes never enter a
  telemetry upload payload or production log, and do not expose SDK diagnosis
  output as medical advice.
- blood components only when `getBloodComponent()` is true; verify uric acid,
  TCHO, TAG, HDL, and LDL are five distinct Room records and that displayed units
  match the current device `CustomSettingData` units;
- body composition only when `getBodyComponent()` is true; verify all 14 fields,
  units, lead-off handling, local persistence, and non-diagnostic UI text;
- body temperature only when `getTemperatureFunction()` is true; verify direct
  measurement and history, Fahrenheit-device conversion to stored `°C`, wear-off
  behavior, and comparison with the vendor app/reference thermometer;
- direct blood glucose only when `getBloodGlucose()` is true; verify the device unit,
  manual-history sync, failure behavior, and non-diagnostic/estimated-value wording;
- stress only when `getStress()` is true; verify direct measurement and manual-history
  sync stay in `0..100 score` and are not interpreted as a mental-health diagnosis;
- metabolic equivalent only when `getMet()` is true; verify direct measurement and
  manual-history sync use `MET` and compare activity-time values with the vendor app;
- blood-glucose calibration only when the adjusting capability is true. Use a
  same-time external meter reference value, verify the setting callback, and do
  not treat calibration as a measurement or medical result;
- menstrual-cycle setting only when `getWomen()` is true. Verify 4–28 day period
  validation, cycle/date mapping, explicit user confirmation, and no production log.

Compare ten repeated syncs against the vendor app. Verify deterministic IDs
prevent duplicate Room rows. Unsupported, zero, invalid, or absent readings must
not produce measurements. A visible disabled card is not evidence that the device
supports the operation. Do not enable pregnancy/preparation/mother modes, TCM, OTA, dials, messages,
contacts, music, or audio.

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
