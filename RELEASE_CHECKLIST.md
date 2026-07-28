# ReHealth MVP Release Checklist

发布负责人必须使用本清单和 `STATUS.md` 作出 go/no-go 决定。历史验收快照不能替代
当前构建、运行时和物理设备证据。

## Android 构建

- [ ] `testDebugUnitTest` 通过。
- [ ] `assembleDebug` 通过并记录 APK SHA-256。
- [ ] 签名 `assembleRelease` 通过。
- [ ] Release 使用真实 HTTPS API 地址，禁止 cleartext 和调试配置。
- [ ] Release APK 不包含 Provider secret、内部 token、数据库凭据或本地配置文件。
- [ ] Release APK 的 `JEECG_SIGN_SECRET` 与 `SMS_TEST_CODE` 为空；生产 `/sys/sms` 使用已评审的移动端安全签名/证明方案和真实短信 Provider，不接受固定测试码或 APK 内共享密钥。
- [ ] Release 不会静默使用 Mock 戒指、Mock 风险或 Mock 归因。

## Android 运行时

- [ ] 登录、退出、401 重新登录和队列恢复通过。
- [ ] Debug 注册 `/sys/sms` 签名通过；仅在 `JEECG_SMS_DEV_MODE=true` 时请求成功后自动填入测试码 `123456`。
- [ ] 数据页固定展示心率、血氧、血压、HRV、体温、ECG、睡眠、步数和活动；无数据为 `--`，“查看全部”不可点击。
- [ ] MRD 扫描、绑定、重连和解绑通过。
- [ ] RWFit 扫描、绑定、重连、能力读取和首批指标同步通过。
- [ ] HBand 扫描、Notify、密码验证、真实画像同步、能力读取和首批指标同步通过。
- [ ] 当前 `productCode` 只激活一个 Provider；Release 未包含 Mock。
- [ ] Debug 套餐切换按“暂停采集→断开旧 Provider→更新唯一绑定→恢复采集”执行；Release 不显示客户端切换入口。
- [ ] 首次绑定前后台采集不使用固定地址、不自动扫描，也不写入 0 或模拟指标。
- [ ] 心率、血氧、血压、体温、睡眠和活动记录先写入 Room。
- [ ] 断网不阻塞 BLE，待上传数据保留在 durable queue。
- [ ] 恢复网络后遥测、特征和反馈按幂等语义同步。
- [ ] 风险、干预、反馈和趋势页面显示真实来源与模型版本。
- [ ] 权限拒绝、蓝牙关闭、设备离线和后端失败均有可恢复提示。
- [ ] 进程恢复只重连当前加密绑定；HBand 画像缓存按用户哈希隔离，登出停止采集并断开设备。

## 物理 MRD QA

- [ ] Android 13+ 真机完成扫描与首次绑定。
- [ ] 锁屏和退后台长时间采集通过。
- [ ] 重连、进程恢复和重复采集保护通过。
- [ ] 电量消耗、温升和测量准确性满足试点标准。
- [ ] 原始信号上传保持关闭，或已有批准的同意、加密和保留策略。

## 物理 RWFit QA

- [ ] 使用 `RH-RW-P01` Debug APK 在 Android 13+ 完成扫描、首次绑定和重启重连。
- [ ] 记录采购型号、固件版本和 SDK 返回的能力集合。
- [ ] 验证步数/活动、睡眠、心率、血氧及设备声明支持的 HRV；不支持项无 0/模拟行。
- [ ] 核对 HRV 厂商单位后再允许临床/特征解释；当前 `rwfit_raw` 只保留原值。
- [ ] 锁屏后台、断网、本地先写、重复同步幂等、功耗和温升通过。
- [ ] 确认 RWFit SDK 未在 app sandbox/logcat 持久化原始健康/蓝牙数据；若存在则阻断发布。

## 物理 HBand QA

- [ ] 已取得采购型号、固件、厂商合作授权及正式能力表。
- [ ] HBand/JieLi/Nordic 固定版本依赖完整，完整安装后无 `WatchOpImpl`、`OnWatchCallback`、`McuMgrBleTransport` 或 Nordic scanner 类加载错误。
- [ ] 使用 `RH-HB-E01` Debug APK 在 Android 13+ 完成扫描、Notify、密码验证和重启重连。
- [ ] 验证个人资料页可选择性别并校验年龄/身高/体重范围；缺少真实画像时连接安全失败，完整画像可成功同步。
- [ ] 验证心率、步数/活动和睡眠；设备声明支持时再验证血压和 ECG，所有不支持项无 0/模拟行。
- [ ] 验证 HBand ECG 波形仅保存在本地 Room，不进入遥测上传或生产日志；界面不把 SDK 结果解释为诊断。
- [ ] 蓝牙关闭、离开范围、后台/锁屏、重复同步幂等、功耗和温升通过。
- [ ] 按 `Android-apk/docs/wearable/HBAND_DEVICE_QA.md` 留存型号、固件、能力、准确性和后台运行证据；未完成前均为阻塞项。

## Backend 与数据

- [ ] telemetry contracts、Device Service 和 Jeecg ReHealth 测试通过。
- [ ] OpenAPI/DTO characterization 门禁通过且检查数大于零。
- [ ] software_db 与 TimescaleDB migrations 在目标版本数据库验证。
- [ ] 用户/租户/设备所有权检查和重复批次幂等通过。
- [ ] TimescaleDB durable write 成功后才返回上传完成。
- [ ] Outbox/Kafka 投递、重试、DLQ 和消费者幂等通过。
- [ ] 备份、恢复、容量和故障切换方案经过发布负责人确认。

## Model Service 与 PIAS

- [ ] `/health`、`/ready`、模型注册表和 Prometheus 指标通过。
- [ ] 真实模型制品哈希、schema、特征顺序和版本已验证。
- [ ] production/staging 不允许 Mock 或无制品状态返回真实标记。
- [ ] 推理超时、断路器和 Provider 失败路径通过。
- [ ] PIAS 归因数据量不足时返回保守状态，不生成虚假因果结论。
- [ ] 医疗免责声明和高风险升级提示通过审核。

## 安全与部署

- [ ] Compose topology gate 对 staging、production 通过。
- [ ] 仅 Gateway 暴露公网端口。
- [ ] secret 使用外部文件或受控 secret 管理，不写入 Git 和镜像。
- [ ] Android logcat、Java/Python 日志和指标不包含 PII、token 或原始健康值。
- [ ] 依赖漏洞、镜像来源、证书、TLS 和访问控制完成复核。

## 发布决定

- [ ] `STATUS.md` 的所有 release blocker 已关闭或由发布负责人书面接受。
- [ ] 自动化结果、物理设备证据、签名 APK 和部署配置属于同一待发布 commit。
- [ ] 发布说明明确已知限制、回滚方式和医疗安全边界。
