# ReHealth AI Android

睿禾精灵 Android 端工程，当前重点是健康数据展示、智能戒指绑定、MRD 戒指 SDK 接入、真实设备数据采集和本地数据保存。

## 当前能力

- 启动页、登录页、健康初识采访页、设备绑定页、主页和数据页基础流程。
- 数据页展示健康指数、戒指连接状态、生命体征、睡眠与活动数据。
- 已接入 MRD 智能戒指 SDK `sdk_mrd2026_1.3.0.aar`。
- 已在真机上验证心率、血氧、血压手动测量。
- 已开启体温定时采集，并可读取体温历史数据。
- 进入主页后会自动进行低频数据采集，减少用户手动点击。
- 数据默认保存到本机 Room 数据库，当前没有接入云端服务器。

## 主要目录

- `app/src/main/java/com/rehealth/genie/ui/`：Compose UI 页面与交互。
- `app/src/main/java/com/rehealth/genie/ring/`：戒指领域模型、ViewModel、仓库接口。
- `app/src/main/java/com/rehealth/genie/ring/mrd/`：MRD 戒指 BLE 与 SDK 协议适配。
- `app/src/main/java/com/rehealth/genie/ring/data/`：Room 数据库实体与 DAO。
- `app/libs/sdk_mrd2026_1.3.0.aar`：MRD 戒指厂商 SDK。
- `rehealth-*.png/xml/txt`：开发过程中的真机 UI、日志和验证资料。

## 构建

需要 Android Studio / JDK 17 环境。

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 当前限制

- 当前自动采集是 APP 打开或前台运行时的自动循环。
- 锁屏或退到后台后的长期采集，需要补充 Android 前台服务和系统常驻通知。
- 后端服务器、用户账号体系、云端上传、模型推理接口尚未正式接入。
- 登录和健康采访仍以本地演示流程为主。
