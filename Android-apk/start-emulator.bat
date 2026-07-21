@echo off
REM Windows批处理脚本 - 启动Android模拟器并安装睿禾精灵

echo ========================================
echo 睿禾精灵 - Android模拟器启动脚本
echo ========================================
echo.

REM 设置Android SDK路径
set ANDROID_HOME=D:\Android_Studio\sdk
set EMULATOR=%ANDROID_HOME%\emulator\emulator.exe
set ADB=%ANDROID_HOME%\platform-tools\adb.exe

REM 检查模拟器是否存在
if not exist "%EMULATOR%" (
    echo [错误] Android模拟器未找到！
    echo.
    echo 请手动启动：
    echo 1. 打开Android Studio
    echo 2. Tools - Device Manager
    echo 3. 启动任意模拟器
    echo.
    pause
    exit /b 1
)

echo [1/4] 检查可用模拟器...
"%EMULATOR%" -list-avds

echo.
echo [2/4] 启动模拟器...
echo 提示：模拟器将在新窗口打开
start "" "%EMULATOR%" -avd Pixel_5_API_33

echo.
echo [3/4] 等待模拟器启动（约30秒）...
timeout /t 30 /nobreak

echo.
echo [4/4] 检查设备连接...
"%ADB%" devices

REM 安装APK
set APK=app\build\outputs\apk\debug\app-debug.apk
if exist "%APK%" (
    echo.
    echo [安装] 正在安装睿禾精灵APK...
    "%ADB%" install -r "%APK%"
    echo.
    echo ✓ 安装完成！
) else (
    echo.
    echo [警告] APK文件未找到：%APK%
    echo 请先编译项目：gradlew.bat assembleDebug
)

echo.
echo ========================================
echo 完成！现在可以在模拟器中使用睿禾精灵了
echo ========================================
echo.
echo 常用命令：
echo   adb devices          - 查看设备
echo   adb logcat           - 查看日志
echo   adb install -r xxx.apk - 安装APK
echo.
pause
