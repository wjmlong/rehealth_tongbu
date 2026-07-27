# ReHealth 当前状态

> 最后核对：2026-07-27。本文档是仓库唯一的当前状态入口；历史验收记录只保存在
> `docs/archive/acceptance/`，不得作为当前实现或发布状态的依据。

## 发布结论

当前 MVP 发布状态：**BLOCKED**。

主要阻塞项：

1. 物理 MRD 戒指与 Android 13+ 真机的扫描、重连、锁屏长时间采集、功耗和准确性 QA 尚未完成。
2. Android 运行时端到端证据仍需覆盖登录、采集、离线队列、遥测上传、风险评估和反馈回传。
3. 签名 Release APK 的运行时 logcat、权限、隐私和真实 HTTPS 环境仍需验收。

## 已实现能力

| 范围 | 当前实现 |
| --- | --- |
| Android | MRD BLE、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列、风险/干预/反馈 UI |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布 |
| JeecgBoot | 登录与权限、用户/设备绑定、业务数据、风险/干预/反馈编排、software_db |
| model-service | CVD 风险评分、模型制品校验、干预生成、健康助手安全边界 |
| PIAS | 独立服务提供个体归因；Android 不执行生产归因 |
| 部署 | Gateway、MySQL、TimescaleDB、Kafka、Redis、Nacos、Prometheus、Grafana 的 Compose 拓扑 |

## 已验证边界

- Android 采集先写 Room，再创建上传任务；网络不可阻塞 BLE。
- 遥测上传不直接触发风险评分。
- 硬件时序数据归 Device Service/TimescaleDB，业务数据归 JeecgBoot/software_db。
- CatBoost、SHAP、LLM 和生产归因不进入 Android APK。
- 生产和 staging 不允许把 Mock 结果伪装成真实模型结果。
- Android Release 源集不包含模拟戒指实现；远程风险失败不会生成本地模拟结果。
- MySQL 8 staging 已有迁移、用户隔离、幂等和重启回读证据；生产容量与恢复仍待验证。

## 当前仓库治理决定

- `backend/jeecgboot-vue3` 保留，作为 JeecgBoot 管理前端。
- `jeecg-boot-module-airag` 保留，不在本轮清理范围。
- `jeecg-module-demo` 需在依赖、配置和构建检查确认无 ReHealth 用途后移除。
- Git 历史不重写；已删除内容仍可从历史 commit 恢复。
- 本地代理状态、构建产物、虚拟环境、APK、截图和下载工具不进入 Git。

## 下一验收顺序

1. 完成仓库清理后的 Android、Java、Python 和部署拓扑回归。
2. 完成物理 MRD 与 Android 运行时端到端 QA。
3. 完成签名 Release APK 和真实部署环境验收。

## 历史证据

- 阶段验收快照：`docs/archive/acceptance/`
- G3 静态隐私审计：`docs/archive/qa/G3_PRIVACY_AUDIT_2026-07-20.md`
- 当前接口与行为以根 `README.md` 的文档索引所列契约为准。
