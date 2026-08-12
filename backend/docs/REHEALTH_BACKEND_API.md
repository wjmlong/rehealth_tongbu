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

## Website BFF business records

The corporate website FastAPI BFF does not connect to PostgreSQL or MySQL. It
forwards the authenticated Jeecg token and tenant header to these JeecgBoot
endpoints; the records are stored in the MySQL `software_db` table
`rehealth_website_record` by the `jeecg-module-rehealth` module.

```text
POST   /rehealth/website/v1/{patients|attributions|settlements|screening}
GET    /rehealth/website/v1/{resource}
GET    /rehealth/website/v1/{resource}/{id}
DELETE /rehealth/website/v1/{resource}/{id}
```

All calls require normal Jeecg authentication and tenant scope (`X-Tenant-Id`);
the resource allow-list is enforced server-side.

只有 `/health` 标记了 `@IgnoreAuth`。其他移动端点应使用 JeecgBoot 的常规认证与授权机制。

## 公司官网本地登录

公司官网不使用 Android 的 `/sys/mLogin`，也不复用管理后台 PC 登录的单点会话槽位：

```text
POST /sys/webLogin
```

请求体沿用 `SysLoginModel` 的 `username`/`password`；`username` 可为唯一登录账号或唯一邮箱。
端点复用 Jeecg 用户有效性检查、失败次数锁定、租户选择和密码校验，以固定 `WEB` 客户端类型
签发 JWT，并额外返回当前租户范围内的 `roles` 与 `permissions`。`WEB` 使用独立的 Redis
单点登录键，不会踢出同一用户的 PC 或 APP 会话。响应中的 Jeecg Token 只允许由官网 FastAPI
BFF 在服务端持有；不得保存到浏览器、日志或官网 PostgreSQL。

本地联调阶段保留官网现有滑块交互，但它不是服务端可验证的人机证明。正式发布前必须补充
服务端验证码/风控、HTTPS Secure Cookie、生产密钥管理和完整安全验收。邮箱验证码登录和机构
自主注册当前明确不支持。

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
