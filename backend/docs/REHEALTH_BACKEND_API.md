# ReHealth 后端 API E1

状态：E1 实现契约。
模块：`jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`。

## 模块边界

ReHealth 生产后端代码位于 `jeecg-module-rehealth`。

在确认所有生产 ReHealth API 均位于专用模块后，已移除过时的 Demo 模块及其早期原型路由位置。

## 端点列表

单体模式下的基础路径：

```text
/jeecg-boot/rehealth/mobile
```

已实现的 E1 端点：

```text
GET  /rehealth/mobile/health
GET  /rehealth/mobile/config
POST /rehealth/mobile/devices/bind
POST /rehealth/mobile/measurements/batch
POST /rehealth/mobile/features/evaluate
GET  /rehealth/mobile/risk/latest
POST /rehealth/mobile/interventions/generate
GET  /rehealth/mobile/interventions/today
POST /rehealth/mobile/interventions/{id}/feedback
POST /rehealth/mobile/attribution/events
```

只有 `/health` 标记了 `@IgnoreAuth`。其他移动端点应使用 JeecgBoot 的常规认证与授权机制。

## Model Service 契约

`ModelServiceClient` 是 Java 调用模型服务的唯一边界。

配置属性：

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

调用：

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /v1/cvd/attribution/individual`

Java 后端不实现 CatBoost、SHAP、CVD 评分或归因逻辑。移动端干预端点是为 LangChain4j 明确保留的例外：Jeecg 组装持久化的权威上下文并生成结构化、保守的健康行动，但不进行诊断、调整用药或推断因果治疗效果。

每次调用 `POST /rehealth/mobile/interventions/generate` 前，Jeecg 都会从 `software_db` 重新加载当前认证用户的档案、最新访谈和风险，并携带租户、时区和内部凭据调用 Device Service：
`GET /rehealth/internal/v1/operations/users/{userId}/intervention-context`。
Device Service 从 TimescaleDB 读取今日活动、睡眠、测量、饮食及有界的近期变化。客户端提供的档案或风险上下文会被忽略。

## 数据库拆分状态

E1 定义了软件库和硬件库边界，但未实现数据库持久化。

`software_db` 边界：

- `ReHealthBusinessRepository`
- 当前实现：`E1PendingSoftwareDbReHealthBusinessRepository`
- 状态：接口已就绪，数据表和 Mapper 待完成

`hardware_db` 边界：

- `HardwareIngestionPort`
- 当前实现：`E2PendingHardwareIngestionPort`
- 状态：接入端口已就绪，消息队列和 `hardware_db` 写入待 E2 完成

遥测上传通过 `HardwareIngestionPort` 路由，不直接写入普通业务表。

## D1 集成说明

后端配置指向正在运行的 model-service 后，Android D1 即可使用 `/features/evaluate`。当前向 `/measurements/batch` 上传遥测会返回明确的 E2 待完成响应，不得将其视为持久化同步完成。
