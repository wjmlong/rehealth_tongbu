# ReHealth 当前状态

> 最后核对：2026-07-29。本文档是仓库唯一的当前状态入口；历史验收记录只保存在
> `docs/archive/acceptance/`，不得作为当前实现或发布状态的依据。

## 发布结论

当前 MVP 发布状态：**BLOCKED**。

主要阻塞项：

1. 物理 MRD/RWFit 戒指及 HBand 手表/手环与 Android 13+ 真机的扫描、重连、锁屏长时间采集、功耗和准确性 QA 尚未完成；HBand 已开始首次真机联调，完整重装后的连接验证仍待完成。
2. Android 运行时端到端证据仍需覆盖登录、采集、离线队列、遥测上传、风险评估和反馈回传。
3. 签名 Release APK 的运行时 logcat、权限、隐私和真实 HTTPS 环境仍需验收。

## 已实现能力

| 范围 | 当前实现 |
| --- | --- |
| Android | 单一有效设备 Provider 路由（Release 注册 MRD/RWFit/HBand）、真实 SDK/BLE、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列、风险/干预/反馈 UI；HBand 心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分以及血糖校准、经期设置按设备能力接入；MT116 能力判定已改为合并新版分包报告，ECG 以 2 号能力包优先，固定 SDK 对应四 ABI JNI 已打包，Room v5 保存校准 mV/导联/采样元数据并提供实时及历史单导联波形详情；体温因真机验证不通过已从 HBand 商品能力和数据页移除，其他指标及 ECG 真机准确性仍待验收 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预业务数据、风险/干预/反馈编排、software_db；模型证据继续保留版本化 JSON 快照 |
| model-service | CVD 风险评分、模型制品校验、干预生成、健康助手安全边界 |
| PIAS | 独立服务提供个体归因；Android 不执行生产归因 |
| 部署 | Gateway、MySQL、TimescaleDB、Kafka、Redis、Nacos、Prometheus、Grafana 的 Compose 拓扑 |
| 真机联调通道 | `https://rehealth.youngjimmy.store`（SSH 反向隧道 + ECS nginx，Let's Encrypt SAN 证书，2026-07-29 端到端 200；备用 `rehealth.47.80.30.228.sslip.io`），Debug/Release 均可联调；见 `tools/dev-tunnel/README.md` |

## 已验证边界

- Android 采集先写 Room，再创建上传任务；网络不可阻塞 BLE。
- 遥测上传不直接触发风险评分。
- 硬件时序数据归 Device Service/TimescaleDB，业务数据归 JeecgBoot/software_db。
- CatBoost、SHAP、LLM 和生产归因不进入 Android APK。
- 生产和 staging 不允许把 Mock 结果伪装成真实模型结果。
- Android Release 源集不包含模拟戒指实现；远程风险失败不会生成本地模拟结果。
- `productCode` 只选择一个懒加载 Provider；绑定存于加密偏好且不迁移 Room，
  未绑定地址时后台采集不会使用固定 MAC 自动连接；RWFit SDK 类型不进入 UI、
  ViewModel 或 Room Entity；HBand SDK 类型同样被限制在 Gateway 文件内，未支持的指标不生成占位记录，
  ECG 波形只写本地 Room 且不进入云端上传；HBand SDK 疾病风险不作为诊断展示，
  新记录保存校准 mV 和结构化导联/采样元数据，旧整数记录保留并仅按相对幅值展示。
- Debug 设备页可在明确确认后暂停采集、断开旧 Provider 并切换本地 `productCode`；
  Release 隐藏该入口，切换不会删除历史 `ring_*` 数据。
- 三个真实 Provider 的后台同步只重连已绑定地址且不做环境扫描；前后台操作共享
  路由互斥锁。HBand 恢复画像使用按用户哈希隔离的加密缓存，外部协程取消会断开 SDK。
- MySQL 8 staging 已有迁移、用户隔离、幂等和重启回读证据；生产容量与恢复仍待验证。

## 当前仓库治理决定

- `backend/jeecgboot-vue3` 保留，作为 JeecgBoot 管理前端。
- `jeecg-boot-module-airag` 保留，不在本轮清理范围。
- `jeecg-module-demo`、`jeecg-demo-cloud-start` 与 `jeecg-cloud-test` 已确认不含 ReHealth 用途并移除。
- Git 历史不重写；已删除内容仍可从历史 commit 恢复。
- 本地代理状态、构建产物、虚拟环境、APK、截图和下载工具不进入 Git。

## 下一验收顺序

1. Docker 引擎恢复后补跑 Device Service 的 TimescaleDB/Testcontainers 集成测试。
2. 在发布环境挂载已审核模型制品并复核真实模型门禁。
3. 使用包含完整 JieLi/Nordic/JNI 依赖的 APK 完成 HBand 连接与 ECG 实时/历史波形复测，再完成 MRD/RWFit/HBand 与 Android 运行时端到端 QA。
4. 完成签名 Release APK 和真实部署环境验收。

## 历史证据

- 阶段验收快照：`docs/archive/acceptance/`
- G3 静态隐私审计：`docs/archive/qa/G3_PRIVACY_AUDIT_2026-07-20.md`
- 当前接口与行为以根 `README.md` 的文档索引所列契约为准。
