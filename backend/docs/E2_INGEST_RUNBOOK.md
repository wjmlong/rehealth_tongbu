# E2.1 持久化硬件接入运行手册

> 状态更新（2026-08-21）：本文记录的独立 MySQL `rehealth_hardware` 写入路径是
> 历史 E2.1 方案。硬件遥测权威现已迁移到独立 Device Service/TimescaleDB
> （TimescaleDB V1–V4，V4 增加 `hardware_diet_record`），
> `POST /rehealth/mobile/measurements/batch` 经 Gateway 路由到 Device Service；
> JeecgBoot 内保留的 MySQL 写入器仅作 legacy/本地联调兼容，不对外可达。
> 当前权威架构见 `HARDWARE_INGEST_ARCHITECTURE.md` 与
> `backend/device-service/README.md`。

日期：2026-07-13
端点：`POST /rehealth/mobile/measurements/batch`

## 前置条件

1. 创建名为 `rehealth_hardware` 的独立 MySQL Schema。
2. 应用仅新增对象的 V1 迁移：

```powershell
$migration = "backend\jeecg-boot\jeecg-boot-module\jeecg-module-rehealth\src\main\resources\db\hardware\mysql\V1__create_hardware_telemetry_tables.sql"
Get-Content -Raw $migration | & mysql -h 127.0.0.1 -u root -p rehealth_hardware
```

JeecgBoot 禁用了 Flyway 自动配置，因此该迁移是显式部署步骤。V1 SQL 成功执行前，不得启用写入器。

## 配置

```powershell
$env:REHEALTH_HARDWARE_DB_ENABLED = "true"
$env:REHEALTH_HARDWARE_DB_URL = "jdbc:mysql://127.0.0.1:3306/rehealth_hardware?characterEncoding=UTF-8&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai"
$env:REHEALTH_HARDWARE_DB_USERNAME = "root"
$env:REHEALTH_HARDWARE_DB_PASSWORD = "<本地密码>"
```

对应的配置键：

```yaml
rehealth.ingest.mode: durable-direct
rehealth.ingest.queue.type: direct-hardware-db
rehealth.hardware-db.enabled: true
rehealth.raw-signal-upload.enabled: false
spring.datasource.dynamic.datasource.hardware: <独立 hardware_db 连接>
```

当 `rehealth.hardware-db.enabled=false` 时，有效遥测不会进入备用内存队列。端点返回 `code=503`，Android 本地队列必须稍后重试。

## 启动后端

从仓库根目录运行：

```powershell
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-module-system/jeecg-system-start -am spring-boot:run -Dspring-boot.run.profiles=dev
```

使用有效的 Jeecg 令牌。只有 `/rehealth/mobile/health` 不需要认证。

## 手工 QA

使用同一令牌和 `batchId`，将以下兼容 D2 的请求体提交两次：

```json
{
  "batchId": "manual-e2-1-001",
  "userId": "client-value-is-ignored",
  "deviceId": "ring-001",
  "collectedFrom": 1720000000000,
  "collectedTo": 1720000300000,
  "source": "ANDROID_ROOM",
  "measurements": [{
    "id": "measurement-001",
    "metricType": "HEART_RATE",
    "measuredAt": 1720000010000,
    "primaryValue": 72.0,
    "unit": "bpm",
    "source": "MRD"
  }],
  "sleepSessions": [],
  "activitySessions": [],
  "signalChunks": [],
  "quality": {"schemaVersion": "d2-v1"}
}
```

预期第一次返回：`ACCEPTED_PERSISTED`、`accepted=true`、`persisted=true`、`queued=false`、`ingestStage=HARDWARE_DB_COMMITTED`。

预期重试返回：`ACCEPTED_DUPLICATE`、与第一次相同的 `receiptId`，且不新增数据行。使用以下 SQL 验证：

```sql
SELECT COUNT(*) FROM hardware_upload_batch WHERE batch_id = 'manual-e2-1-001';
SELECT COUNT(*) FROM hardware_measurement m
JOIN hardware_upload_batch b ON b.id = m.upload_batch_id
WHERE b.batch_id = 'manual-e2-1-001';
```

两个计数均应为 `1`。重启后端，再次提交同一请求体，确认仍返回 `ACCEPTED_DUPLICATE`，且两个计数仍为 `1`。

添加一个 `signalChunks` 条目，或添加嵌套的 `ppgPayload`/`rawPayload` 键，确认返回 `REJECTED_INVALID`、`accepted=false`，且数据库没有新增行。

## 自动化验证

```powershell
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am '-Dtest=TelemetryBatchValidatorTest,HardwareTelemetryIngestionServiceTest,JdbcHardwareTelemetryWriterTest' test
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am package
git diff --check
git status --short --branch
```

H2 MySQL 模式测试覆盖已提交的规范化写入、类似进程重启场景的幂等、部分行失败后的完整回滚、原始信号拒绝和 API 响应语义，但不能替代上述 MySQL 手工 QA。

## 运行风险

- 直接同步 JDBC 的吞吐量受硬件数据库连接池限制。
- 当前尚无持久化消息队列、死信队列、分区轮换或负载测试证据。
- 设备归属校验仍依赖持久化的 `software_db` 设备绑定。
- 保留期限目前仅有策略/配置文档，清理任务尚未实现。
