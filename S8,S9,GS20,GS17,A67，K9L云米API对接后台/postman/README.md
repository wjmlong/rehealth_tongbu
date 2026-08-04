# 云米 OpenAPI Postman 测试说明

本目录用于测试 ReHealth 接入云米平台时需要的外部 OpenAPI。Collection 面向“我方按需或定时请求云米，再将返回数据写入我方数据库”的模式，不代表当前 ReHealth 后端已经实现这些拉取接口。

## 文件

- `Viomi-OpenAPI-ReHealth.postman_collection.json`：可直接导入 Postman 的接口集合。
- `Viomi-OpenAPI-ReHealth.postman_environment.json`：测试环境变量模板。

## 导入与首次测试

1. 在 Postman 点击 **Import**，同时选择 Collection 和 Environment 两个 JSON 文件。
2. 在右上角选择环境 **云米 OpenAPI - 本地测试环境**。
3. 编辑环境，只填写云米分配的 `appId` 和 `appKey`，保存环境。
4. 执行 `00 - 认证与设备 / 01 获取 AccessToken（自动计算 MD5）`。
5. Token 成功后，脚本会自动保存 `accessToken`、`userId` 和 `tokenExpire`。
6. 执行 `02 获取设备列表`。如果 `imei` 为空，脚本会把第一台设备的 IMEI 保存到环境；有多台设备时请手动选择正确的 IMEI。
7. 先执行 `03 获取最新健康汇总` 和“历史健康数据”中的只读接口。
8. 只有在确认手表在线、型号命令码正确且用户已授权时，才执行“主动测量”或“立即定位”。

Token 请求会在预请求脚本中自动完成：

```text
Timestamp = 当前 UNIX 秒级时间戳
Password  = MD5(AppKey + AppId + Timestamp)，32 位小写
```

除 Token 接口外，云米要求同时传递：

```http
Authorization: {{accessToken}}
Content-Type: application/json
```

以及 JSON 请求体中的：

```json
{
  "AccessToken": "{{accessToken}}"
}
```

## 环境变量

| 变量 | 是否必填 | 说明 |
| --- | --- | --- |
| `baseUrl` | 是 | 默认 `https://openapi.miwitracker.com` |
| `appId` | 是 | 云米分配的 AppId |
| `appKey` | 是 | 云米分配的 AppKey；按 Secret 保存 |
| `accessToken` | 自动 | Token 接口执行成功后自动保存 |
| `userId` | 自动 | Token 接口执行成功后自动保存 |
| `imei` | 是 | 目标手表 IMEI；设备列表可自动填入第一台设备 |
| `beginTime` | 否 | UTC 开始时间，格式建议 `yyyy-MM-dd HH:mm:ss`；留空自动取最近 7 天 |
| `endTime` | 否 | UTC 结束时间；留空自动取当前时间 |
| `mapType` | 否 | `Baidu` 或 `Google`，默认 `Baidu` |
| `groupId` | 否 | 设备分组，默认 `0` |
| `stepType` | 否 | 当前在线文档包含该字段，默认 `0`；清零语义需向云米确认 |
| `cmdHeartRate` | 否 | 默认 `9012` |
| `cmdBloodPressure` | 否 | 默认 `9510`，执行前按型号确认 |
| `cmdBloodOxygen` | 否 | 默认 `9511`，执行前按型号确认 |
| `cmdTemperature` | 否 | 默认 `9111` |
| `cmdLocation` | 否 | 默认 `0039` |

## 已收录接口

| 分组 | 接口 | 主要返回字段 |
| --- | --- | --- |
| 认证 | `POST /api/token/get_token` | `Result.AccessToken/UserId/Expire` |
| 设备 | `POST /api/devicelist/get_devicelist` | 设备 IMEI、型号、状态、电量 |
| 最新健康 | `POST /api/healthinfo/get_latest_healthinfo` | 步数、心率、血压、睡眠、血氧、体温 |
| 心率 | `POST /api/heartrate/get_heartrate_bytime` | `HeartRate/HrTime` |
| 血压 | `POST /api/bloodpressure/get_bloodpressure_bytime` | `Systolic/Diastolic/BpTime` |
| 血氧 | `POST /api/bloodoxygen/get_bloodoxygen_bytime` | `BloodOxygen/BloodOxygenTime` |
| 体温 | `POST /api/Temperature/get_temperature_bytime` | `Temperature/TemperatureTime` |
| 步数 | `POST /api/steps/get_steps_bytime` | 时间段步数、距离、热量 |
| 每日步数 | `POST /api/steps/get_steps_forday` | 每日明细和合计 |
| 睡眠 | `POST /api/sleep/get_sleep_bytime` | 总睡眠、浅睡、深睡、清醒、眼动 |
| 呼吸率 | `POST /api/RespiRate/GetRespiRate` | `RespiratoryRate/UtcTime` |
| 指令 | `POST /api/command/sendcommand` | 指令受理状态，不直接返回测量值 |
| 当前位置 | `POST /api/location/get_location_info` | 经纬度、定位类型、设备状态、电量 |
| 历史轨迹 | `POST /api/track/get_track_info` | 时间、经纬度、速度、方向 |

## 主动测量的正确测试方法

`sendcommand` 的成功响应只说明云米平台已经接受或下发指令，不代表已经取得测量值。

1. 记录发送指令前的最新测量时间。
2. 执行对应主动测量请求。
3. 若返回 `Code=0`、`Code=1` 或 `Code=1803`，等待手表完成测量和上传。
4. 间隔约 5、15、30、60 秒查询最新健康或相应历史接口。
5. 只有出现晚于指令发送时间的新记录，才能判定测量完成。
6. 超过业务允许时间仍没有新记录，应判定超时，不能生成假数据。

V1.6.7 指令表对血氧存在冲突：`9511` 被描述为“测量血氧”，`9726` 在不同位置又被描述为血氧测量或上传间隔。本 Collection 默认使用 `9511`，不会自动发送 `9726`，以免误改设备配置。S8、S9、GS20、GS17、A67、K9L 的实际命令支持范围必须由云米按型号和固件确认。

## 状态码注意事项

云米官方在线帮助页目前的响应示例通常使用 `Code=1` 表示成功；项目收到的 V1.6.7 文档中，部分接口章节和旧 Postman 截图使用 `Code=0` 表示成功。Collection 的测试脚本暂时兼容 `0` 和 `1`；指令接口额外兼容 `1803`（指令已下发）。正式后端实现时，应按云米书面确认的端点级规则处理，不能假设所有接口共用一个成功码。

常见指令结果：

| Code | 文档含义 |
| --- | --- |
| `0` / `1` | 不同版本文档中的成功值 |
| `1800` | 设备不在线 |
| `1801` | 发送指令超时 |
| `1802` | 下发失败 |
| `1803` | 指令已下发 |

## 安全与隐私

- 不要把真实 AppKey、AccessToken、IMEI、健康数据或经纬度提交到 Git。
- 不要把含真实 Token 的 Postman 截图发到群聊或工单；原始案例中的 Token 已经过期，也不应复制使用。
- 测试数据仅用于健康参考，不得将消费级手表结果描述为医疗诊断。
- 若 Token 失败，重新执行 Token 请求；不要在日志中打印 AppKey、Password 或完整 Token。

## 文档依据

- 本地供应商资料：`OpenAPI接口说明_V1.6.7(3).docx`、`主动请求案例 格式.docx`。
- 云米在线帮助：<https://openapi.miwitracker.com/Help>。

在线帮助与本地文档不一致时，应向云米取得对应 AppId、设备型号和固件版本的书面接口确认，再调整命令码或成功码。
