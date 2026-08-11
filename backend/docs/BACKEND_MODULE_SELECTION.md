# 后端模块选择

最后复核：2026-07-27

JeecgBoot 提供账号、租户、权限、管理和 ReHealth 业务编排。Python `model-service` 仍是 CatBoost、SHAP、健康助手生成和归因的权威服务。

## 当前模块

| 模块 | 决定 | 职责 |
| --- | --- | --- |
| `jeecg-boot-base-core` | 保留 | 共享认证、Web、校验、MyBatis、数据源和框架支持。 |
| `jeecg-module-system/jeecg-system-api` | 保留 | 本地和云端系统契约。 |
| `jeecg-module-system/jeecg-system-biz` | 保留 | 用户、租户、权限、菜单、字典和平台管理。 |
| `jeecg-module-system/jeecg-system-start` | 保留 | 用于本地开发和 MVP 验证的单体启动器。 |
| `jeecg-boot-module/jeecg-module-rehealth` | 保留 | ReHealth 移动 API、持久化、模型客户端、干预、反馈和运营功能。 |
| `jeecg-boot-module/jeecg-boot-module-airag` | 保留 | `jeecg-system-biz` 所需的 Jeecg AI/RAG 平台能力；它不是 ReHealth 模型权威服务。 |
| `jeecg-server-cloud/jeecg-cloud-gateway` | 保留 | 可选的云端网关和路由聚合。 |
| `jeecg-server-cloud/jeecg-cloud-nacos` | 保留 | 可选的服务发现和配置服务。 |
| `jeecg-server-cloud/jeecg-system-cloud-start` | 保留 | 可选的云模式系统/ReHealth 启动器。 |
| `jeecg-server-cloud/jeecg-visual` | 保留 | 可选的监控、Sentinel 和 XXLJob 基础设施。 |
| `backend/jeecgboot-vue3` | 保留 | JeecgBoot 管理前端。 |

## 已移除模块

- `jeecg-module-demo`：上游示例控制器、Mock 端点、示例实体、静态大屏资源和 Demo 测试数据。检查未发现 ReHealth 代码或产品依赖。
- `jeecg-demo-cloud-start`：仅用于暴露 `jeecg-module-demo` 的启动器。
- `jeecg-server-cloud/jeecg-visual/jeecg-cloud-test`：上游 Feign、消息、Seata 和 ShardingSphere 示例，与 ReHealth 产品无依赖关系。

系统启动器不再依赖或排除已移除的制品。其代码生成器默认目标已从本机特定的 Demo 路径改为 `jeecg-module-rehealth`。现有数据库仍可能包含上游 Demo 表或菜单行；本次源码清理不执行破坏性生产数据库迁移。

## 服务与数据边界

```text
Android
  -> Gateway（可选）
  -> JeecgBoot ReHealth API
       -> software_db：账号和业务记录
       -> model-service：风险、干预和助手

Android 遥测
  -> Device Service
       -> TimescaleDB 持久化写入 + Outbox
       -> Kafka 持久化/质量事件
```

- JeecgBoot 不得直接写入 Device Service 所有的 TimescaleDB 表。
- 模型推理和归因必须位于 Java 客户端抽象之后。
- 用户和租户身份来自已认证的服务端上下文，绝不来自请求体中的归属字段。
- 只有完成持久化和幂等校验后才能返回遥测成功。

## 构建验证

从仓库根目录运行：

```powershell
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am test
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
mvn -f backend/jeecg-boot/jeecg-server-cloud/pom.xml -pl jeecg-system-cloud-start -am package -DskipTests
```

今后新增或移除任何模块时，都必须更新根 `README.md`、本文档和部署拓扑；发布范围变化时还必须更新 `STATUS.md`。
