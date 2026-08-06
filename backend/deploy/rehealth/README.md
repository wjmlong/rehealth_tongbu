# ReHealth deployment topology

This topology is the deployment contract for development, staging and
production. It keeps every stateful or application service on internal Docker
networks; only `edge` publishes a host port. Android continues to use the same
`/jeecg-boot/rehealth/**` API surface.

## Prerequisites

1. Build the Jeecg cloud JARs, Device Service JAR and Jeecg Vue `dist` directory.
2. Copy `.env.example` to `.env` and select an explicit runtime mode.
3. Materialize every file in `secrets/` from the deployment secret manager.
4. Mount an approved, signed model artifact bundle read-only at `artifacts/`.
5. Import `gateway/rehealth-routes.json` into the selected Nacos namespace.

External images are digest-pinned in `images.lock`. ReHealth application images
are built from the checked-out release and use `pull_policy: never`; the release
pipeline must record their resulting content digests before promotion.

## Validation

```powershell
python backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production --report topology.json
```

The topology gate is static. Runtime readiness requires the application JARs,
the hardened PIAS entrypoint, Device Service and real secret/artifact bundles.
Do not interpret a static pass as a deployed-service health result.

The `topology-failures` gate is an executable bounded dependency-transition
test: it starts temporary TCP dependencies, proves each is reachable, stops the
selected dependency, and probes the resulting ingest/publisher/model state.
Its `runtime_verified` field applies only to that temporary failure harness.

Device Service readiness requires TimescaleDB and Jeecg identity resolution.
Kafka is intentionally not a readiness dependency: an outage degrades the
publisher and leaves committed Outbox rows pending while ingestion stays ready.

## Telemetry authority cutover

`backend/qa/rehealth_stack_gate.py cutover` is the only supported route switch.
The checked-in approval descriptor is bound to the approved Todo 10 staging
volume clone by exact reconciliation, signature and cosign public-key hashes,
the pre-cutover Git SHA, both database fingerprints and both schema versions.
The gate descriptor-reads the exact files, rejects symlinks and files changed
during a read, verifies the non-expired reconciliation and cosign signature,
then atomically replaces the route seed and deployment audit record.

```powershell
$env:REHEALTH_CUTOVER_VERIFY_KEY = (
  Resolve-Path backend/deploy/rehealth/gateway/cutover-verification.pub
).Path

python `
  backend/qa/rehealth_stack_gate.py cutover `
  --reconciliation <approved-bundle>\reconciliation.json `
  --signature <approved-bundle>\reconciliation.sig `
  --verify-key-env REHEALTH_CUTOVER_VERIFY_KEY
```

The exact public paths remain
`/jeecg-boot/rehealth/mobile/measurements/batch` and
`/jeecg-boot/rehealth/mobile/measurements/recent`. Their order `-100` route is
owned only by `rehealth-device-service`; the Jeecg business wildcard stays at
`-90`, so the legacy MySQL telemetry writer is not externally reachable. The
route preserves `X-Access-Token`, `X-Tenant-Id`, and the device selector used
for binding authorization while removing client-supplied ReHealth user and
tenant identity headers. Device Service returns the existing
`success/message/code/result/timestamp` Result-compatible envelope.

Failure cases are executable and must leave the route and audit bytes unchanged:

```powershell
python `
  backend/qa/rehealth_stack_gate.py cutover `
  --reconciliation <approved-bundle>\reconciliation.json `
  --signature <approved-bundle>\reconciliation.sig `
  --verify-key-env REHEALTH_CUTOVER_VERIFY_KEY `
  --cases bad_signature,expired_report,dirty_reconciliation,stale_git_sha,dsn_mismatch,route_collision
```

Before authority is established, `--action rollback` retains the Jeecg/MySQL
route. After a successful cutover, the same application rollback retains the
Device Service/Timescale route. Routing data back to MySQL is a separate data
authority reversal and is forbidden without a new, separately approved and
signed Timescale-to-MySQL reconciliation.

Production publishes only the `edge` port. Gateway, Jeecg, Device Service,
TimescaleDB and all other dependencies remain on internal Compose networks.

## Local application development

Run only stateful infrastructure in Docker and run the Java/Python application
services directly on Windows:

```powershell
docker compose `
  --env-file backend/deploy/rehealth/.env `
  -f backend/deploy/rehealth/docker-compose.yml `
  -f backend/deploy/rehealth/docker-compose.local-infra.yml `
  --profile development up -d `
  software-db hardware-db kafka kafka-init redis nacos prometheus grafana
```

The local override binds dependency ports to `127.0.0.1` only. Kafka keeps its
internal `kafka:9092` listener for Compose jobs and adds
`127.0.0.1:29092` for locally running services. Do not use this override for
staging or production.

Application services then use these local endpoints:

| Service | Local endpoint |
|---|---|
| JeecgBoot | `http://127.0.0.1:8080/jeecg-boot` |
| Device Service | `http://127.0.0.1:8091` |
| model-service | `http://127.0.0.1:8000` |
| PIAS | `http://127.0.0.1:8010` |
| Kafka | `127.0.0.1:29092` |

Keep passwords and internal service credentials in the ignored
`backend/deploy/rehealth/secrets/` files. Load them into the local process
environment at startup; never copy them into tracked YAML or source files.

Health chat now supports two server-side engines behind the unchanged mobile API:

- `REHEALTH_HEALTH_AGENT_ENGINE=langchain4j` is the default and runs prompt assembly, bounded
  conversation memory, the authenticated current-profile tool and the OpenAI-compatible provider
  call inside JeecgBoot.
- `REHEALTH_HEALTH_AGENT_ENGINE=model-service` keeps the Python provider path only for an explicit rollback.

Before enabling LangChain4j, apply
`V20260730_1__add_health_agent_conversations.sql`, keep
`REHEALTH_SOFTWARE_DB_ENABLED=true`, and place the provider key in the ignored
`secrets/provider_credential` file. The container mounts it into JeecgBoot as
`REHEALTH_LLM_API_KEY_FILE`; the key must not be placed in tracked YAML or Android.
Provider URL/model are selected with `REHEALTH_LLM_BASE_URL` and
`REHEALTH_LLM_MODEL`.

Personalized intervention generation always uses the same server-only
LangChain4j provider configuration, independently of
`REHEALTH_HEALTH_AGENT_ENGINE`. Jeecg must also have
`REHEALTH_DEVICE_SERVICE_ENABLED=true`,
`REHEALTH_DEVICE_SERVICE_BASE_URL=http://device-service:8091`, and the shared
`REHEALTH_INTERNAL_SERVICE_CREDENTIAL_FILE`. Optional intervention-specific
limits are `REHEALTH_INTERVENTION_LANGCHAIN4J_TIMEOUT_SECONDS` and
`REHEALTH_INTERVENTION_LANGCHAIN4J_MAX_TOKENS`. If provider credentials,
Device Service context, or software persistence are unavailable, generation
fails closed; no mock plan is persisted.

For local personalized-intervention QA, seed the active `admin` account with an
explicitly marked test profile, interview, mock risk context, and rolling
TimescaleDB activity/sleep/measurement/diet history:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-admin-intervention-test-data.ps1
```

The script resolves the current `admin` user ID and login tenant from
`software_db`, refreshes only records carrying its stable local-seed IDs, and
prints row-count verification. It requires the local `software-db` and
`hardware-db` containers plus ignored database password files. All seeded data
is labelled `LOCAL_TEST_SEED`; the persisted risk has `is_mock=true` and is not
valid for clinical or production decisions. Start JeecgBoot and Device Service,
authenticate as `admin`, retain its login tenant header, and call
`POST /rehealth/mobile/interventions/generate` with a stable `request_id` to
exercise the real LangChain4j generation and persistence path.

To keep using the legacy model-service health Q&A locally with the YAML-first path:

1. Copy `model-service/config/ai-chat.example.yml` to the ignored
   `model-service/config/ai-chat.local.yml` file.
2. Fill `health-agent.provider.api-key`. The default provider is
   `https://api.deepseek.com` with model `deepseek-v4-flash`; a non-empty key
   automatically enables it.
3. Keep `REHEALTH_HEALTH_AGENT_ENGINE=model-service` and restart the local applications. `start-local-apps.ps1` detects this file and
   lets model-service load it directly. The API key is read only by `model-service`;
   it must never be copied into the Android project, JeecgBoot YAML, or Git.

Java LangChain4j is selected when the ignored local `.env` omits the engine setting. Use
`secrets/provider_credential`; the startup script passes only the secret-file path to JeecgBoot.

Home camera food/OCR analysis is a separate server-side vision path. Before enabling it, apply
`V20260731_1__add_behavior_records.sql`, keep `REHEALTH_SOFTWARE_DB_ENABLED=true`, and place the
vision provider key in the ignored `secrets/vision_provider_credential` file. Configure
`REHEALTH_VISION_ENABLED=true`, `REHEALTH_VISION_BASE_URL`, `REHEALTH_VISION_MODEL`, and optionally
`REHEALTH_VISION_TIMEOUT_SECONDS` (default `75`) and `REHEALTH_VISION_MAX_TOKENS` (default `1200`);
the startup script forwards those ignored local limits and supplies only
`REHEALTH_VISION_API_KEY_FILE` to JeecgBoot. Each analysis attempt makes one provider call without
automatic model retry. Raw camera bytes are forwarded for one analysis request and are neither
stored in MySQL nor written to application logs.

When a Windows development machine requires a local proxy to reach an HTTPS
provider, set `REHEALTH_JEECG_HTTPS_PROXY_ENABLED=true` together with
`REHEALTH_JEECG_HTTPS_PROXY_HOST` and `REHEALTH_JEECG_HTTPS_PROXY_PORT` in the
ignored local `.env`. The launcher adds the proxy only to the JeecgBoot JVM and
keeps `localhost`, `127.*`, and `[::1]` on direct connections. Specify only a
host and port; never put proxy credentials in tracked configuration. A loopback
proxy address is local-development configuration and must not be copied into a
container, staging, or production deployment.

To proxy only the photo-analysis provider instead of all JeecgBoot HTTPS calls, set the ignored
`REHEALTH_VISION_PROXY_HOST` and `REHEALTH_VISION_PROXY_PORT`. The proxy is injected only into the
vision client's JDK `HttpClient`; Device Service and other loopback service calls remain direct.

The secret-file plus ignored `.env` path remains supported when
`ai-chat.local.yml` is absent and is still the required pattern for staging and
production.

After the current JARs and Python virtual environment have been built, start
or stop all application processes with:

```powershell
powershell -ExecutionPolicy Bypass -File backend/deploy/rehealth/start-local-apps.ps1
powershell -ExecutionPolicy Bypass -File backend/deploy/rehealth/stop-local-apps.ps1
```

Before the first local Jeecg run against a new `software-db` volume, import the
base Jeecg schema and apply the add-only ReHealth migrations. The local Debug
launcher maps its legacy-compatible `hardware` datasource to
`rehealth_software`, so the telemetry V1 migration must be applied to that
database before exercising the Android upload queue:

```powershell
$migration = "backend\jeecg-boot\jeecg-boot-module\jeecg-module-rehealth\src\main\resources\db\hardware\mysql\V1__create_hardware_telemetry_tables.sql"
Get-Content -Raw $migration | docker exec -i rehealth-software-db-1 `
  sh -c 'MYSQL_PWD=$(cat /run/secrets/software_db_password) mysql -u rehealth_software rehealth_software'
```

This migration is intentionally explicit because JeecgBoot disables Flyway
auto-configuration. Apply it once to a fresh volume; do not replay it after the
tables exist. Quartz uses the official uppercase `QRTZ_*` table names. If an
older Jeecg base dump created lowercase `qrtz_*` names, normalize those names
before startup when Docker MySQL runs with case-sensitive table names; changing
only `tablePrefix` is insufficient because Quartz appends uppercase table-name
suffixes internally.

`start-local-apps.ps1` 为本地 JeecgBoot 设置 `JEECG_SMS_DEV_MODE=true`。此模式下
`POST /jeecg-boot/sys/sms` 仍要求正常请求签名，但不会调用短信网关，而是在 Redis 中
保存固定测试验证码 `123456`。staging/production 必须在 `.env` 中设置
`JEECG_SMS_DEV_MODE=false`、`JEECG_SMS_ALIYUN_ENABLED=true`，填写审核通过的短信签名和
注册模板 Code，并把专用 RAM 用户的 AccessKey ID/Secret 分别写入忽略跟踪的
`secrets/aliyun_sms_access_key_id` 与 `secrets/aliyun_sms_access_key_secret`。注册模板变量必须
命名为 `${code}`。登录和修改密码模板只有启用对应短信入口时才需要填写。短信凭据不得复用
OSS AccessKey，也不得进入 `.env`、Android APK、日志或受版本控制的 YAML；任一生产必填项
缺失时发送会失败关闭，不会回退到旧默认签名或模板。

该本地启动脚本还会启用
`REHEALTH_QA_SYNTHETIC_ATTRIBUTION_HISTORY_ENABLED=true`，仅供 Debug APK 的
全链路演练把已完成真实 RDI-16 远程评估的 30 日合成历史转发给 PIAS。后端默认值仍为
`false`，staging/production 不得启用。

The scripts run applications as hidden Windows processes. PID files and
separate stdout/stderr logs are written to the ignored
`backend/deploy/rehealth/.local-runtime/` directory. The model service loads
the reviewed local model, PIAS uses its production entrypoint, and the external
health-agent provider follows the ignored local `.env` configuration.
