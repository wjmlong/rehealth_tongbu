# RWFit physical-device QA

Status: provider implemented; physical acceptance pending.

## Build and install

From `Android-apk` build the debug APK with RWFit as the forced single active
product. This property is debug-only; Release continues to obtain the active
product from the normal binding state and defaults to MRD when no selection is
present.

```powershell
.\gradlew.bat "-Prehealth.debug.wearable.product.code=RH-RW-P01" testDebugUnitTest assembleDebug
```

For Android Studio Run, add the following untracked local setting to
`local.properties`, then rebuild and reinstall the app:

```properties
rehealth.debug.wearable.product.code=RH-RW-P01
```

The command-line `-P` value takes precedence over `local.properties`. If neither
is present, Debug intentionally defaults to MRD.

Install `app/build/outputs/apk/debug/app-debug.apk`. If another application with
the same package/signature is installed, uninstall it or clear its data before
the first run. Grant Bluetooth scan/connect and notification permissions.

## Required evidence

Record all of the following without placing a raw BLE address or health value in
Git or production logs:

- phone model, Android version, app commit and APK SHA-256;
- purchased RWFit device model, firmware shown after connection, and SDK tag
  `RW_SDK_V2.0.0_20260724`;
- scan result count, first-connect result, restart/background reconnect result;
- capability flags for steps, sleep, heart rate, blood oxygen, and HRV;
- Room row counts and timestamps before/after each supported sync;
- unsupported capability result showing no corresponding Room row;
- lock-screen/background duration, disconnect/reconnect behavior, battery and
  thermal observations.
- whether the app sandbox contains `logger/devices/` files after use. The vendor
  guide mentions `XLogUtils.setLogEnable(false)`, but that public class is absent
  from the pinned AAR; any persisted packet/health log blocks Release until the
  vendor supplies a supported disable API or a no-log build.

## Scenarios

1. With Bluetooth off and with permission denied, verify scan stops safely and
   no Room row is created.
2. Enable Bluetooth, scan, select the target device, and wait until the function
   menu/capability callback completes. A transport-level connection alone is not
   considered ready.
   - Do not select a device from a noisy generic BLE list. That normally means
     the test APK was built in MRD mode; verify the RWFit setting above first.
   - Keep the target next to the phone and compare the list before and after
     putting the wearable into its charging case or powering it off. Select only
     the entry that disappears and reappears consistently. RSSI is supporting
     evidence, not identity proof, and a full BLE address must not be recorded.
3. Restart the app and verify the encrypted RWFit binding is reused without an
   automatic scan or fixed test address.
4. Run full sync. Only capabilities reported by the device are requested. Verify
   local writes occur before any upload queue item.
5. Run manual HR, SpO2, and HRV only when advertised. An absent or invalid value
   must produce no row, never a zero or simulated row.
6. Repeat full sync and confirm deterministic entity IDs prevent duplicate rows.
7. Disable the network and repeat sync/background collection; BLE and Room must
   continue, while upload retries later.
8. Inspect app-private files and filtered logcat for vendor packet/health logs.
   Do not copy raw logs into Git; record only whether such logs exist.

## Mapping notes and acceptance limits

- Heart rate uses `bpm`; blood oxygen uses `%`.
- Step distance is documented in meters. SDK calories are documented as `cal`
  and are converted to the existing Room `kcal` field by dividing by 1000.
- RWFit sleep exposes awake/light/deep stages, not REM. The existing non-null
  Room REM field is zero for this vendor record and is not treated as an
  independently measured REM metric.
- The official SDK does not document the HRV unit. Values remain `rwfit_raw` and
  must not be interpreted as milliseconds until vendor/model evidence confirms
  the unit.
- Blood pressure, temperature, stress, blood sugar, PPG, and other SDK metrics
  are outside this Provider phase and must have no generated placeholder rows.
