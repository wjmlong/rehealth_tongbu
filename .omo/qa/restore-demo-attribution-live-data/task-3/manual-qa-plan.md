# Attribution tab manual QA

Device: `emulator-5554`; package: `com.rehealth.genie`; exact APK:
`Android-apk/app/build/outputs/apk/debug/app-debug.apk`.

```powershell
$adb = 'D:\Android_SDK\platform-tools\adb.exe'
$evidence = 'D:\rehealthAI\.omo\qa\restore-demo-attribution-live-data\task-4'

& $adb -s emulator-5554 install -r 'D:\rehealthAI\Android-apk\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s emulator-5554 shell am force-stop com.rehealth.genie
& $adb -s emulator-5554 shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1

# Attribution bottom tab, then period selector / factor / plan interactions
# on the configured 1080x2400 API31 emulator.
& $adb -s emulator-5554 shell input tap 540 2180
& $adb -s emulator-5554 shell input tap 540 400
& $adb -s emulator-5554 shell input swipe 540 1850 540 500 700
& $adb -s emulator-5554 shell input tap 500 650
& $adb -s emulator-5554 shell input swipe 540 1850 540 500 700
& $adb -s emulator-5554 shell input tap 540 1800

& $adb -s emulator-5554 shell uiautomator dump --compressed /sdcard/rehealth-attribution.xml
& $adb -s emulator-5554 pull /sdcard/rehealth-attribution.xml "$evidence\attribution-final.xml"
& $adb -s emulator-5554 shell screencap -p /sdcard/rehealth-attribution.png
& $adb -s emulator-5554 pull /sdcard/rehealth-attribution.png "$evidence\attribution-final.png"

# Motion capture: scroll through all target cards while the factor bars animate.
& $adb -s emulator-5554 shell screenrecord --time-limit 20 /sdcard/rehealth-attribution.mp4
& $adb -s emulator-5554 shell input swipe 540 1850 540 500 800
& $adb -s emulator-5554 shell input swipe 540 1850 540 500 800
& $adb -s emulator-5554 pull /sdcard/rehealth-attribution.mp4 "$evidence\attribution-scroll.mp4"

# Start/mid/end stills for the animated contribution bars.
& $adb -s emulator-5554 shell screencap -p /sdcard/attribution-start.png
Start-Sleep -Milliseconds 350
& $adb -s emulator-5554 shell screencap -p /sdcard/attribution-mid.png
Start-Sleep -Milliseconds 700
& $adb -s emulator-5554 shell screencap -p /sdcard/attribution-end.png
& $adb -s emulator-5554 pull /sdcard/attribution-start.png "$evidence\attribution-animation-start.png"
& $adb -s emulator-5554 pull /sdcard/attribution-mid.png "$evidence\attribution-animation-mid.png"
& $adb -s emulator-5554 pull /sdcard/attribution-end.png "$evidence\attribution-animation-end.png"
```

Binary observables: activity stays in the foreground; hierarchy contains `健康归因`,
`7 天`, `30 天`, `90 天`, `个人风险趋势`, `今日行为记录`, `贡献因素`, and
`个性化干预计划`; screenshots are non-empty PNGs; the MP4 is non-empty and scrolls
without a crash or blank surface.
