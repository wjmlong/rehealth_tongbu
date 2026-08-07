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

For Release acceptance, open “我的 → 设备绑定” and confirm the page exposes exactly
“HBand（MT116 蓝牙）” and “云米（IMEI 云端）”. A fresh install must select HBand by
default; upgrading a build that stored MRD/RWFit must migrate to an unbound HBand selection.

## Evidence to record before testing

- purchased product name, SKU, exact model and firmware;
- Android model/version and app commit;
- vendor cooperation/commercial authorization;
- vendor-approved password setup/reset flow (the pinned official demo uses
  `0000`, but this has not been confirmed on a purchased device);
- complete numbered `DeviceFunctionPackage1..5` reports without logging raw health
  values or the BLE MAC in release logs. The deprecated `FunctionDeviceSupportData`
  callback may fire repeatedly with partially initialized fields and is diagnostic only.
- for MT116, confirm `DeviceFunctionPackage2.getEcgFunction()` reports support. The
  latest deprecated aggregate value is used only when package 2 is absent;
  `RH-HB-E01` must reject the device when neither source reports ECG support.
- if the settled result still reports no ECG, verify the purchased MT116 SKU has
  physical ECG electrodes and record its firmware; do not bypass the gate for a
  non-ECG hardware variant.

## Connection sequence

1. Grant Bluetooth scan/connect and notification permissions. Before Bluetooth
   permission is granted, the HBand search button must remain disabled and the
   vendor SDK must not receive a scan request.
2. Open the personal-profile editor, select sex, and enter an age in `1..120`,
   height in `50..250 cm`, and weight in `10..300 kg`; save and refresh the profile.
3. Scan and confirm only SDK-recognized VeePoo/HBand candidates are shown.
4. Record the advertised device name separately from screenshots containing a
   full MAC; redact addresses before sharing evidence.
5. Connect and verify the sequence is BLE connection, Notify success, password
   confirmation, a settled merge of the deprecated aggregate capability callback
   and numbered function-package callbacks, real ReHealth profile synchronization,
   then `CONNECTED`/READY. MT116 should no longer be rejected from an early,
   partially initialized aggregate callback when package 2 reports ECG support.
   Confirm logcat contains no `NoClassDefFoundError` for `WatchOpImpl`,
   `McuMgrBleTransport`, or Nordic scanner classes and no `UnsatisfiedLinkError`
   for `libnative-lib.so`, and that a full APK install,
   rather than Apply Changes, was used after SDK dependency changes.
   For a minified Release build, also switch HBand -> Viomi -> HBand and scan
   again. The app process must remain alive; Release mapping must keep
   `com.veepoo`, `com.inuker.bluetooth.library`, `com.jieli`, and the ReHealth
   HBand adapter classes from being renamed or removed.
6. Test wrong password/confirmation timeout and verify the app reports an error,
   writes no telemetry, and can recover after disconnect/retry.
7. Remove one required profile field (sex, age, height, or weight) and verify
   connection is blocked before the SDK receives Demo demographic values.
8. Restart the app and verify it reconnects only the encrypted bound address.

## Data acceptance

For `RH-HB-E01`, validate only device-advertised capabilities:

- record `isSupportHRV`, `isSupportHrvAppDetect`, `isSupportMet`, and
  `isSupportMetAppDetect` separately; do not infer App measurement support from a
  base capability alone;

- heart-rate history and manual heart-rate measurement (`bpm`);
- daily steps/activity, including confirmation that SDK distance in kilometres
  is converted to Room metres and calories remain kcal. Confirm live daily sport
  and the per-day sum of five-minute origin records agree with the vendor app;
- sleep start/end, deep/light duration, cross-midnight handling, and the
  documented absence of a separate REM field in the selected SDK callback. Confirm
  the dedicated sleep read completes before the origin-history command starts. For
  every returned night, compare the displayed duration directly with the watch/vendor
  app `allSleepTime`; it must not equal the longer `sleepDown`→`sleepUp` span merely by
  coincidence and must not include synthesized awake minutes. For total-only sleep,
  verify duration is displayed while deep/light/REM remain unknown. If Room contains
  several increasing callbacks for one wake-up day, verify Today displays the largest
  final total and 7/30-day views count that day once;
- manual blood oxygen only when `getSpo2H()` is true; verify a real `%` value and
  wear-off/failure behavior;
- retain the four HRV/MET capability flags and the pinned `startDetectHrv`/`startDetectMet`
  APIs as lower-level compatibility evidence, but do not expose them as the normal
  `RH-HB-E01` product route. The purchased MT116 declares the dedicated capabilities while
  all three dedicated HRV/stress/MET commands return all-zero `unknown action`;
- verify HRV/stress use package-4 `miniCheckup` or real history and MET uses real device
  history only. The product page must never issue a real-time MET command.
  Show no-result on failure and confirm no “This feature is not supported” SDK toast appears.
  Debug evidence must use the `HBandMetricFlow` tag and must not include device identifiers
  or raw health values;
- blood-pressure history and manual measurement only when `getBp()` is true;
  verify systolic/diastolic order, `mmHg` units, wear-off/charging/low-battery
  failures, and compare repeated readings with a validated cuff without making
  diagnostic claims;
- manual ECG only when package 2 `getEcgFunction()` (or the legacy fallback
  `getEcg()` when package 2 is absent) reports support; verify contact/wear guidance, start/
  stop/cancel behavior, SDK sample/draw frequency, local waveform persistence, and the
  average-heart-rate summary for both normal completion and abnormal-result callbacks.
  Before the SDK start command, verify both the data-card entry and detail-page button show
  instructions requiring continuous opposite-hand contact with the metal electrode and a
  stable posture. Cancelling the dialog must not start measurement.
  Open the single-lead detail page before measurement, confirm ADC callbacks update the
  live chart and progress, and verify every valid ADC point is paired with the callback's
  corresponding gain before `EcgUtil.convertToMvWithValue(..., ecgType, false, gain)`.
  After completion, inspect the Room v5 row: new HBand records use `FLOAT32_LE`, identify
  `HBAND_ECG_UTIL_MV_V1`, retain duration/ECG type/contact quality, and label I or V1 only
  when `EcgDiagnosis.leadOffType` explicitly supplies it. A normal result without that field
  must remain “导联待设备确认”. Upgrade from v4 and confirm old `INT32_LE` rows remain visible
  as relative amplitude rather than being deleted or mislabeled mV. Confirm the latest ten
  local records can be selected and replayed without UI stalls.
  A summary may exist without a curve when the device returns only average heart rate.
  Confirm raw ECG waveform bytes never enter a
  telemetry upload payload or production log. The page must say this is portable single-lead
  ECG rather than a clinical 12-lead examination, must not list SDK disease-risk output as a
  diagnosis, and must show “仅供健康参考，不能替代医疗诊断”.
- blood components only when `getBloodComponent()` is true; verify uric acid,
  TCHO, TAG, HDL, and LDL are five distinct Room records and that displayed units
  match the current device `CustomSettingData` units;
- body composition only when `getBodyComponent()` is true; verify all 14 fields,
  units, lead-off handling, local persistence, and non-diagnostic UI text. Before the SDK
  command, verify the instructions require a complete electrode circuit, separated relaxed
  arms, and still posture; cancelling the dialog must not start measurement;
- direct blood glucose only when `getBloodGlucose()` is true; verify the device unit,
  manual-history sync, failure behavior, and non-diagnostic/estimated-value wording;
- HRV and stress measurement use package-4 `miniCheckup` first when available, then device
  history. Dedicated-command failure, `BUSY`, `LOW_POWER`, `WEAR_OFF`, or write failure must
  end without persistence;
- HRV persists and displays only positive values. Verify every real stress result stays in
  `1..100 score`; zero is treated as no result and is not
  interpreted as a mental-health diagnosis;
- metabolic equivalent reads only the latest real device history. It persists and displays
  only positive values; the card has no real-time measurement button and is hidden when the
  device has no valid history. Compare activity-time values with the vendor app;
- before any valid HRV/stress/MET row exists, verify all three cards are absent rather than
  displayed as `--`. A positive real history row may reveal its card; a history-only HRV/stress
  card has no action. `ring_sim`, `synthetic_qa`, mock, zero, non-finite, and out-of-range values
  must not reveal a card;
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
Do not enable HBand temperature for `RH-HB-E01`; it failed the current physical-device
test and has been removed from the product capability and data page.

## Failure and background matrix

- Android 8/9, 12, and 14/15 where devices are available;
- permissions denied, Bluetooth off, device out of range, and password timeout;
- user-initiated disconnect and out-of-range disconnect after a successful connection;
  verify the app remains alive and logcat contains no `NoClassDefFoundError` from
  `releaseJLSDK`, `JLWatchFaceManager`, or `BmpConvert`;
- app foreground/background, screen locked, process killed, and phone reboot;
- foreground manual sync overlapping scheduled collection (commands must remain
  serialized);
- offline collection followed by network recovery;
- 10 consecutive syncs and a 24-hour power/temperature run;
- device clock/time-zone error and a sleep session spanning midnight.

Do not mark HBand accepted until every applicable item has device-backed logs,
screenshots, Room row inspection, model/firmware details, and comparison results.
