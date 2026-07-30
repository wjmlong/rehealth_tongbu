# ReHealth MVP 工程总纲

本文档只定义长期稳定的工程原则、服务边界和交付纪律。当前完成度、发布阻塞项与
最近验证结果统一记录在 `STATUS.md`。

## 1. MVP 目标

```text
登录与健康访谈
  -> 绑定 MRD 戒指
  -> 前后台采集真实生命体征
  -> Room 本地持久化
  -> CVD 16 维特征
  -> 离线上传队列
  -> 云端风险评分和干预
  -> 用户反馈与趋势
```

MVP 优先保证真实采集、本地可靠性、离线可用、云端评分和反馈闭环。不在当前阶段
引入端侧大模型、端侧 SHAP、联邦学习、保险结算或完整医生工作台。

## 2. 服务边界

| 组件 | 负责 | 不负责 |
| --- | --- | --- |
| Android | BLE/厂商 SDK、Room、轻量特征、上传队列、用户交互 | CatBoost、SHAP、LLM、生产归因 |
| Gateway | 公网入口、路由、安全头、限流边界 | 业务持久化、模型推理 |
| Device Service | 遥测校验、TimescaleDB、Outbox、Kafka | 用户业务档案、模型推理 |
| JeecgBoot | 账号、权限、绑定、业务编排、LangChain4j 健康问答、software_db、管理后台 | 硬件时序库所有权、CatBoost/SHAP/归因模型执行 |
| model-service | 风险评分、模型治理、干预；保留健康问答旧接口用于灰度回退 | 用户认证、设备接入、业务主数据、权威聊天历史 |
| PIAS | 个体归因服务 | Android 端归因、静默 Mock |
| rehealth-algorithms | 训练、仿真、算法研究和 PIAS 实现 | 患者移动端入口 |

## 3. 数据不变量

1. 健康数据必须先持久化，再异步上传。
2. BLE 采集不得等待网络或后端响应。
3. 上传成功只表示权威服务完成约定的 durable write。
4. 队列必须支持幂等、退避重试、401 暂停和重新登录后恢复。
5. 原始 PPG/RRI 默认不上传；启用前必须完成同意、加密和保留策略评审。
6. 客户端不得通过请求体声明数据所有者；用户和租户来自可信认证上下文。
7. Kafka 事件只携带最小引用与状态，不携带原始健康值。
8. 健康问答完整历史归 `software_db`，模型上下文只使用有界消息窗口和每轮重新查询的服务端授权画像；Android 必须先写 Room 再发起网络请求。

## 4. Android 规则

- Kotlin 优先，Compose 只负责 UI 状态呈现。
- ViewModel 编排用例，不直接拥有低层 Bluetooth 操作。
- BLE 和厂商协议位于 repository/adapter 层。
- Room 写入必须显式、可迁移、可恢复。
- 长时间采集使用 Foreground Service，恢复任务使用 WorkManager。
- Mock 只能存在于明确的 debug/test 边界，Release 不得静默回退。
- 保持 minSdk、targetSdk 和 Compose 兼容，除非任务明确要求升级。

## 5. Backend 与模型规则

- ReHealth 移动 API 隔离在 Jeecg `rehealth` 模块/package。
- Device Service 独占硬件遥测写入和查询路径。
- JeecgBoot 通过 client abstraction 调用 model-service 和 PIAS。
- model-service 使用 FastAPI 和类型化 schema，每个评分响应包含模型版本。
- 模型制品通过只读挂载、哈希和环境门禁加载，不提交到 Git。
- 健康建议必须保守，不声称诊断、处方或替代医生。

### RHI v2 演进边界

- `cvd-16-v1` 临床风险与 RHI 动态健康指数采用独立 schema、版本和持久化记录。
- RHI 不把横断面疾病分类分数表达成 10 年事件概率。
- 长期临床概率必须由审核后的人群适用模型提供；RHI 不自行补造概率。
- 32 维日特征由未来云端 Feature Pipeline 版本化生成，不能阻塞 Android BLE
  采集，也不能把 CatBoost、SHAP 或归因放入 APK。
- `research_preview_not_clinically_validated` 只允许内部验证和影子双跑；
  Android 生产 UI 只能接受未来审核后的 `validated_production` 状态。
- 正式接入前必须完成共享 OpenAPI、日快照/质量/安全事件存储、迁移、
  200–300 人 90 天动态验证和设备公平性门禁。

## 6. 安全与隐私

- 生产日志禁止记录原始健康值、token、手机号、BLE MAC 和直接标识符。
- Android 只上传稳定设备 ID 和允许的地址摘要。
- Provider、服务间凭据和数据库 secret 只存在于运行时 secret 文件或受控环境。
- 生产/staging 必须失败关闭，不允许缺少真实模型或 secret 时伪装就绪。

## 7. 变更纪律

每项任务必须：

1. 检查相关源码、构建文件、契约和当前 Git 状态。
2. 描述现状、最小实现方案、风险和验证命令。
3. 保留用户已有的无关修改。
4. 新 API 同步 OpenAPI、DTO、测试和集成契约。
5. 新 Schema 提供迁移策略。
6. 用户可见行为同步 QA 和发布检查表。
7. 运行适用的 Android、Maven、Python、契约或部署门禁。
8. 分批提交，避免把清理、业务修改和历史操作混在一个 commit。

## 8. Definition of Done

- 代码可编译，或失败原因和阻塞条件已明确记录。
- 自动化测试已运行，或无法运行的原因已说明。
- 新行为有基本自动化或人工验证步骤。
- API、Schema、部署和用户行为文档保持同步。
- `git diff --check` 通过。
- `git status` 无意外修改。
- Mock、隐私和医疗安全边界未被削弱。
