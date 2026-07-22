@echo off
REM ReHealth Full Automation Script
REM This script will build APK, start emulator, install, and test

echo ================================
echo ReHealth Full Automation
echo ================================
echo.

REM Set paths
set PROJECT_DIR=D:\rehealthAI\Android-apk
set SDK_DIR=D:\Android_SDK
set OUTPUT_DIR=D:\rehealthAI\outputs
set APK_PATH=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk
set ADB=%SDK_DIR%\platform-tools\adb.exe
set EMULATOR=%SDK_DIR%\emulator\emulator.exe

echo Step 1: Building APK...
echo ================================
cd /d %PROJECT_DIR%
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo [ERROR] Build failed!
    echo Check build logs at: %PROJECT_DIR%\build.log
    pause
    exit /b 1
)

echo.
echo [SUCCESS] APK built successfully!
echo APK location: %APK_PATH%
echo.

REM Check APK exists
if not exist "%APK_PATH%" (
    echo [ERROR] APK not found at expected location!
    pause
    exit /b 1
)

echo Step 2: Checking for devices/emulators...
echo ================================
%ADB% devices > %OUTPUT_DIR%\adb_devices.txt
type %OUTPUT_DIR%\adb_devices.txt

REM Check if any device is connected
%ADB% devices | find "device" | find /v "List" > nul
if errorlevel 1 (
    echo [WARNING] No device connected. Attempting to start emulator...

    REM List available AVDs
    %EMULATOR% -list-avds > %OUTPUT_DIR%\avd_list.txt

    REM Try to start first AVD
    for /f "delims=" %%i in (%OUTPUT_DIR%\avd_list.txt) do (
        echo Starting emulator: %%i
        start "Android Emulator" %EMULATOR% -avd %%i -no-audio -no-snapshot
        goto :emulator_started
    )

    :emulator_started
    echo Waiting for emulator to boot...
    timeout /t 30 /nobreak > nul

    REM Wait for device to be ready
    :wait_device
    %ADB% devices | find "device" | find /v "List" > nul
    if errorlevel 1 (
        echo Still waiting for device...
        timeout /t 10 /nobreak > nul
        goto :wait_device
    )
)

echo.
echo [SUCCESS] Device ready!
echo.

echo Step 3: Installing APK...
echo ================================
%ADB% install -r "%APK_PATH%"
if errorlevel 1 (
    echo [ERROR] Installation failed!
    pause
    exit /b 1
)

echo.
echo [SUCCESS] APK installed successfully!
echo.

echo Step 4: Launching application...
echo ================================
%ADB% shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1
timeout /t 5 /nobreak > nul

echo.
echo Step 5: Checking application status...
echo ================================

REM Check if app is running
%ADB% shell "ps -A | grep rehealth" > %OUTPUT_DIR%\app_process.txt
type %OUTPUT_DIR%\app_process.txt

REM Check for crashes
%ADB% logcat -d | findstr "FATAL" > %OUTPUT_DIR%\crash_logs.txt
if exist %OUTPUT_DIR%\crash_logs.txt (
    for %%A in (%OUTPUT_DIR%\crash_logs.txt) do if %%~zA gtr 0 (
        echo [WARNING] Crashes detected! Check: %OUTPUT_DIR%\crash_logs.txt
    )
)

echo.
echo Step 6: Taking screenshot...
echo ================================
%ADB% shell screencap -p /sdcard/rehealth_auto_test.png
%ADB% pull /sdcard/rehealth_auto_test.png %OUTPUT_DIR%\screenshot.png
echo Screenshot saved to: %OUTPUT_DIR%\screenshot.png

echo.
echo Step 7: Collecting logs...
echo ================================
%ADB% logcat -d > %OUTPUT_DIR%\full_logcat.txt
echo Full logcat saved to: %OUTPUT_DIR%\full_logcat.txt

REM Extract ReHealth specific logs
%ADB% logcat -d | findstr "ReHealth" > %OUTPUT_DIR%\rehealth_logs.txt
echo ReHealth logs saved to: %OUTPUT_DIR%\rehealth_logs.txt

echo.
echo Step 8: Checking app components...
echo ================================

REM Check permissions
%ADB% shell dumpsys package com.rehealth.genie | findstr "permission" > %OUTPUT_DIR%\app_permissions.txt
echo Permissions saved to: %OUTPUT_DIR%\app_permissions.txt

REM Check memory usage
%ADB% shell dumpsys meminfo com.rehealth.genie > %OUTPUT_DIR%\memory_info.txt
echo Memory info saved to: %OUTPUT_DIR%\memory_info.txt

REM Dump UI hierarchy
%ADB% shell uiautomator dump
%ADB% pull /sdcard/window_dump.xml %OUTPUT_DIR%\ui_hierarchy.xml
echo UI hierarchy saved to: %OUTPUT_DIR%\ui_hierarchy.xml

echo.
echo ================================
echo AUTOMATION COMPLETE
echo ================================
echo.
echo Results saved to: %OUTPUT_DIR%
echo.
echo Files generated:
dir /b %OUTPUT_DIR%\*.txt %OUTPUT_DIR%\*.png %OUTPUT_DIR%\*.xml
echo.
echo Review the following for issues:
echo - %OUTPUT_DIR%\crash_logs.txt (should be empty)
echo - %OUTPUT_DIR%\rehealth_logs.txt (app logs)
echo - %OUTPUT_DIR%\screenshot.png (visual verification)
echo.
echo Press any key to exit...
pause
