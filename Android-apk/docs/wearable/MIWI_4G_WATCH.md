# Miwi 4G 云平台手表（S8）接入说明

> 首次编写：2026-07-29（分支 `4Gwatch`）。适用于云米/MiwiTracker 云平台的 4G 手表
> （API 文档覆盖 S8、S9、GS20、GS17、A67、K9L；当前接入目标为 S8）。
> 契约细节见 `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` 的
> "Miwi 4G Cloud Watch (S8)" 一节。

## 1. 架构与边界

```text
S8 手表 --4G/SIM--> 云米云平台 --HTTP 回调--> JeecgBoot /rehealth/miwi/push
                                                 ↓ 按 deviceId 匹配绑定用户
                                          HardwareIngestionPort（与手机遥测同一入库管线）
App：仅负责 IMEI 绑定（/rehealth/mobile/devices/bind）与结果展示，不参与数据采集。
```

- 该设备**不走手机蓝牙**，App 端 `Miwi4gCloudRingRepository` 不做 BLE 扫描；
  `syncAll()` 不产生本地记录（数据链路在云端）。
- deviceId 规则与 BLE 设备一致：`miwi4g-` + SHA-256(IMEI) 前 24 位十六进制；
  IMEI 原文不上传、不写日志。
- 回调按"已绑定用户"过滤：未绑定 IMEI 的推送会被 ack（code=1）但不入库。

## 2. 后端组件（jeecg-module-rehealth，包 `org.jeecg.modules.rehealth.miwi`）

| 类 | 职责 |
| --- | --- |
| `MiwiProperties` | `rehealth.miwi.*` 配置（默认 `enabled=false`） |
| `MiwiCallbackController` | `POST /rehealth/miwi/push?token=...`，`@IgnoreAuth` + 预共享 token 校验 |
| `MiwiPushService` | 解包双层 JSON、IMEI→deviceId→用户、送入 `HardwareIngestionPort` |
| `MiwiHealthDataMapper` | 字段映射与 UTC 时间归一（支持秒/毫秒/本地时间字符串，本地时间按 UTC+8） |
| `MiwiOpenApiClient` | `get_token`（厂商协议要求 `MD5(AppKey+AppId+Timestamp)`）与拉取式查询 |

回调返回约定（厂商协议）：处理成功/已跳过 → `{"code":1}`；瞬时故障（如数据库不可用）
→ `{"code":0}` 提示厂商重试；token 错误 → HTTP 401。

## 3. App 端组件

- `WearableVendor.MIWI4G`、产品 `RH-S8-4G01`（`wearable_products.json`）。
- `Miwi4gCloudRingRepository`：IMEI 即"设备地址"；`connect()` 校验 10-17 位数字并
  写入 `ActiveWearableBindingStore`，随后由 `RingViewModel` 触发云端 bind。
- 设备页（`DeviceBindingScreen`）在激活 S8 产品时显示 IMEI 输入卡片，隐藏蓝牙扫描。
- 产品切换入口目前仅 Debug 构建开放（`ALLOW_WEARABLE_PRODUCT_SWITCH`），Release
  默认产品策略不变——正式发布前需确定 Release 产品选择方案。

## 4. 已知厂商 API 限制（V1.6.5 / V1.6.7）

- 无 ECG 波形/R 波/房颤/QT 等任何 ECG 接口；无血糖、血脂、尿酸接口。
- PPG 原始数据仅 ZIP 导出（约 20Hz），且未确认 S8/MT116 固件是否支持 HEALTHPPG 上报。
- 回调无签名（"校验规则：无"）；本项目以私有 callback-token + 建议 IP 白名单兜底。
- 返回码不统一（推送 code=1 成功，OpenAPI Code=0 成功）；时区/时间格式混杂，
  入库前已统一为 UTC epoch millis。
- 健康数据缺质量字段（佩戴状态、测量方式、信号质量、固件版本等）。

## 5. 待厂家书面确认清单（发给厂家）

1. S8（以及 MT116，如适用）固件是否已接入贵司云平台，并支持 Health 数据主动回调？
2. 请提供测试环境：AppId/AppKey、api-base-url、样机 IMEI、回调联调支持。
3. 回调安全：能否增加 `X-App-Id / X-Timestamp / X-Nonce / X-Signature`
   （HMAC-SHA256(AppSecret, timestamp+nonce+body)）与 IP 白名单？现阶段请在回调
   URL 中携带我方分配的 token 参数。
4. 哪些型号支持 HEALTHPPG 原始数据上报？PPG 实际采样率是否高于 CSV 中的 20Hz？
   请提供 LED 波长/通道数/ADC 位数/增益/单位/滤波说明。
5. 是否有 ECG 波形（采样率、导联、R 波时间点、报告）上传或导出的任何计划/私有接口？
6. 血糖是否仅设备本地显示？是否存在未写入本文档的血糖上报通道与校准接口？
7. 健康数据能否附带质量字段：佩戴状态、手动/自动测量、信号质量、固件版本、算法版本？
8. 推送的时间字段（timestamp/uploadTime 等）时区与单位的权威定义，逐接口列明。
9. 设备远程配置（测量频率、定时测量计划）通过哪个 OpenAPI 下发？
10. 回调失败的重试策略（次数/间隔/超时判定）与幂等键（是否有 msgId）。

## 6. QA 状态

- 软件侧：`MiwiPushServiceTest`（后端映射/绑定过滤/拒收）与
  `Miwi4gCloudRingRepositoryTest`（App 绑定/恢复/无本地记录）已通过。
- `HARDWARE_QA_PENDING`：真机 S8 + 厂商测试环境未打通前，端到端推送、
  Token 获取、时区实测、绑定归属正确性均未验收。
- 后端模块本机未运行 Maven 编译（开发机无 Maven/JDK 配置），合入主干前需
  在 CI 或后端开发机执行 `mvn -pl jeecg-boot-module/jeecg-module-rehealth -am test`。
