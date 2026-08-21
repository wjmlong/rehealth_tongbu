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

## Local insurer workflow

Apply the non-destructive MySQL migrations through `V20260819_2` before testing
the insurer website. They add import/job/plan-feedback tables, workflow
permissions, insurance organization settings, tenant-scoped department codes and
local-admin acceptance grants, read-only organization/member settings, insurer
intervention actions and aggregate RHI/RDI daily snapshots plus structured RDI
contributions. `V20260819_1` creates the commented, versioned institution care-plan
tables and separates plan view, draft edit and publish permissions; `V20260819_2`
adds immutable occurrence execution facts used by the App's rolling 28-day adherence.
Production must assign
`insurer_viewer`, `insurer_analyst`, `insurance_operator` or `insurer_auditor`
explicitly and must not rely on the local admin grant.

The website FastAPI BFF must receive its Jeecg base URL and tenant mapping from
server-side configuration. It must not receive a MySQL DSN. To export the RWE
Word report, set the optional variable below to an approved read-only template;
when absent, local development resolves the repository source template.

```text
REHEALTH_RWE_TEMPLATE_PATH=E:\code\rehealth_tonbu\docs\ReHealth_PSM_RWE_Report_Draft_V0.1.docx
```

The FastAPI runtime requires `python-docx==1.2.0` and `openpyxl==3.1.5` for Word
export and XLSX import. These dependencies do not access the database.

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
| Device Service | `http://127.0.0.1:8091` by default; override with `REHEALTH_DEVICE_SERVICE_PORT` |
| model-service | `http://127.0.0.1:8000` |
| PIAS | `http://127.0.0.1:8010` |
| Kafka | `127.0.0.1:29092` |

The Windows local launchers pass the development-only insurance tenant-membership
scope as explicit Jeecg command-line properties. This allows local insurer dashboard
QA without weakening staging or production defaults; the insurer read permission must
still be granted manually to the selected local role.

Keep passwords and internal service credentials in the ignored
`backend/deploy/rehealth/secrets/` files. Load them into the local process
environment at startup; never copy them into tracked YAML or source files.

The website administration read API uses the same secret-file boundary. Set
`REHEALTH_DEVICE_SERVICE_INTERNAL_TOKEN_FILE` to the mounted internal credential
file (Compose uses `/run/secrets/internal_service_credential`); do not configure
the retired plaintext `rehealth.device-service.credential` property for this API.
For systemd, put
`REHEALTH_DEVICE_SERVICE_INTERNAL_TOKEN_FILE=/run/secrets/internal_service_credential`
in the unit's root-owned `EnvironmentFile` (mode `0600`). Missing/unreadable
credentials or an unavailable Device Service fail the detail API with HTTP 503;
the response never silently reports telemetry as absent. Requests to
`/rehealth/admin/v1/patients/**` also require `X-Access-Token`, `X-Tenant-Id`,
an active membership of that tenant, and `rehealth:admin:patient:view`. The
Device Service internal health URL requires `tenantId` and scopes every query by
both tenant and user. Patient list/detail queries require an existing
`rehealth_patient_profile`, so institution employee accounts without a patient
profile are not returned as patients. Because current profile and CVD tables do not carry a tenant
column, the admin API fails closed and excludes a user who has another active
tenant membership. Detail reads are bounded to one operator-membership lookup,
one target aggregation query, and one Device Service summary request. Synthetic,
mock, demo, sample, `LOCAL_TEST_SEED`, and `ring_sim` provenance is returned as a
summary flag. A synthetic detail may include risk, RHI/RDI, structured Factor16,
and the latest intervention summary only when each returned result is explicitly
Mock; clients must label it as a test preview, exclude it from clinical aggregates,
and not present it as medical advice. Provenance/result mismatches are
suppressed;
raw telemetry rows and the internal credential are never logged or returned.
The list deliberately avoids an N+1 Device Service fan-out, so every row returns
`provenanceStatus=unknown`; website/BFF charts and counters must not include an
`unknown` row in clinical-risk statistics. A detail read changes the status to
`verified_real` only when its non-empty provenance set contains exclusively
registered real sources (`hband_wearable`, `hband_cloud_restore`, `viomi_cloud`,
`mrd_ring`, `mrd-sdk`, or `rwfit`). Empty, mixed-unregistered, or unknown sources
remain `unknown` and suppress risk and index results. Synthetic sources are
`synthetic` and follow the explicit Mock preview rule above.

Flyway migration `V3.9.2_1__rehealth_admin_patient_permission.sql` creates the
assignable `rehealth:admin:patient:view` permission idempotently and grants it to
no role. Before enabling the website, use JeecgBoot's role authorization screen
to assign “查看患者健康数据” only to an approved operations/clinical role; do not
attach it to a broad default or anonymous role.
Compose packages the migration from
`jeecg-server-cloud/jeecg-system-cloud-start/src/main/resources/flyway/sql/mysql`
into the deployed Cloud JAR and forces `SPRING_FLYWAY_ENABLED=true` with that
classpath location. Cloud startup uses a dedicated fail-closed configuration to
select only the dynamic `master` MySQL datasource, baseline an existing untracked
3.9.2 schema at `3.9.2.0`, tolerate older history entries absent from the Cloud
artifact, validate the current checksum, and execute `3.9.2.1`. Any selection,
validation, or migration failure aborts startup. The copy under the monolithic start module is retained for
non-Cloud deployments.

The current database seed omits the vendor dump's known failed
`3.9.2.0 / V3.9.2_0__all_upgrade.sql / -1769021348` history row. An
existing volume created from the older dump must be backed up before startup
and repaired once with an exact, row-count-checked delete of only that failed
row. Refuse the repair when any other failed migration exists; do not run an
unscoped automatic `repair`. Flyway 7 also probes
`performance_schema.user_variables_by_thread` on MySQL. Give the migration
principal `SELECT` on that table only (plus its required target-schema DDL), or
use a dedicated migration principal; do not grant global `PROCESS`, global
`SELECT`, or administrative privileges.
### Local insurer acceptance data

After the insurance migrations through `V20260813_4` have been applied, seed a
repeatable tenant-1000 acceptance cohort with the local MySQL container running:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-insurance-workflow-test-data.ps1
```

The script creates 12 synthetic tenant members, profiles, non-clinical risk
fixtures, active policies, consents and paid claims. Six members have an active
intervention and six are controls, so the checked-in draft PSM study has enough
candidates to freeze a snapshot and exercise the workflow. Re-running the
script updates the same deterministic records instead of duplicating them.

Every business row is marked `LOCAL_INSURANCE_QA`; seeded users have no password,
phone number or email and cannot log in. Although the risk rows use `is_mock=0`
to exercise the verified-risk and PSM code paths, their model/scorer/artifact
fields explicitly identify them as local non-clinical fixtures. Never run this
script outside local development or use its values for medical, underwriting,
claim or settlement decisions.

The same seed adds two active insurer managers (`local_insurance_manager_01` /
`local_insurance_manager_02`, password `123456`) under `健康险一部` and `健康险二部`.
Each manager is assigned six synthetic insured subjects through
`rehealth_insurance_subject_manager`; this mapping is the data-permission fixture
for the next risk-list API change. The current risk API is still tenant-scoped,
so manager login data is ready for permission testing but does not by itself
change the existing list query until the manager-scope guard is implemented.

To validate Jeecg multi-tenant insurance administration with several organizations,
seed three isolated synthetic insurers and their staff:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-multi-insurer-tenant-test-data.ps1
```

The repeatable local-only seed owns tenant IDs `9101`–`9103`, creates two business
departments per insurer, and adds an organization administrator, two department
managers, an analyst, operator, viewer, pending invitee, and one shared auditor
account. The shared auditor deliberately belongs to all three tenants so tenant-
scoped department and role joins can be tested with the same global Jeecg user.
Every organization, person, phone number, email address, and license number is
explicitly marked as synthetic `LOCAL_MULTI_INSURER_QA` data. The script refuses
to overwrite `9101`–`9103` when any ID is already owned by non-QA data. Active
test accounts use the local-only password `123456`; never run this seed outside
the local development database. The selected seed actor (`admin` by default) is
also added as an active member of all three QA tenants without changing its
default login tenant. In the Jeecg console, use the tenant selector to switch to
`9101`, `9102`, or `9103` before opening department management; system department
queries intentionally show only the currently selected tenant. The selected QA
actor is also linked to each insurer's root organization so its department is not
blank in tenant-scoped user management. The seed verifies that every seed-owned
tenant membership resolves to an active department in the same tenant.

Organization names, staff names, addresses, and other normal business display
fields intentionally use production-like wording without visible `测试`, `合成`,
or `[LOCAL QA]` suffixes. Synthetic ownership is still enforced through stable
usernames and IDs, reserved contact values, `source_system`, fixture metadata,
and the local-only seed guards; presentation wording is not a provenance check.

To extend those three organizations with complete APP-user service fixtures, run:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-test-data.ps1 `
  -AnchorDate 2026-08-14
```

By default this command first refreshes the `LOCAL_MULTI_INSURER_QA` organization
and staff baseline, then writes `LOCAL_MULTI_INSURER_APP_QA` data to MySQL and
TimescaleDB. It creates 14 global APP accounts: four home-labelled accounts for
each of tenants `9101`–`9103`, plus `local_app_shared_01` and
`local_app_shared_02`. The shared accounts and selected home-labelled accounts
receive services from multiple insurers. This yields
36 independent insurer-subject relationships and 120 staff responsibility
assignments. Each insurer keeps its original six subjects and adds six APP users
already served by another insurer, producing 12 visible subjects per insurer and
exercising the supported “one APP user, multiple service institutions” model.
APP accounts are deliberately absent from `sys_user_tenant`; insurer
service membership comes from `rehealth_insurance_subject`, while WEB staff
membership continues to use Jeecg tenant membership and tenant-scoped roles.

Each APP account has a profile, complete RHI manual input, interview, device
binding, behavior records, 30 CVD-16 risk-history fixtures, seven RHI daily
snapshots, seven explicitly Mock RDI daily snapshots with three structured
contributions each, four Factor16 explanations, PIAS attribution and an intervention plan
containing three actions. Every insurer relationship has its own policy,
coverage, consent, plan binding, intervention, three APP feedback entries, three
staff actions and a claim. Each insurer's intervention workbench contains exactly
three `pending_action`, three `pending_review`, three `in_progress`, and three
`improved` subjects; every active fixture staff account is responsible for at
least four subjects. TimescaleDB receives 118
days matching the Android Debug full-chain rehearsal shape: ten measurements,
one sleep record, one activity record and one diet record per relationship per
day. All active synthetic staff and APP accounts use password `123456`. Re-running
the script is idempotent and verifies exact counts. The data is synthetic,
non-clinical and local-only; do not use it for medical, underwriting, claim or
settlement decisions.

APP-user names, device labels, business records, plan copy, and study titles use
the same natural presentation style as ordinary records. Re-running the seed
updates existing fixture rows through its deterministic upserts. Internal
`LOCAL_MULTI_INSURER_APP_QA`, `synthetic`, `is_mock`, and
`clinicalUseAllowed=false` markers remain unchanged and continue to control QA
isolation and safety behavior.

After the APP-user seed and migrations through `V20260819_2` are available, populate the
versioned institution care-plan tables with the same 36 insurer-subject
relationships:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-versioned-care-plan-test-data.ps1 `
  -AnchorDate 2026-08-19
```

The repeatable `LOCAL_VERSIONED_CARE_PLAN_QA` seed writes 36 active plans and
published revisions, 108 patient-visible plan items, 108 scheduled occurrences
and 72 lifecycle audit events. Display titles and instructions use natural
business wording; deterministic IDs and `source_plan_id` retain local fixture
ownership. The wrapper refuses incompatible reserved-ID collisions and verifies
the exact row counts plus Chinese comments on all five tables and all 71
columns. The canonical insert SQL is colocated with the module at
`jeecg-module-rehealth/src/main/resources/db/testdata/software/mysql/`; the
deploy directory only contains the guarded runner. Never run this local-only
fixture against staging or production.

To make every workbench status and risk distribution visible, the CVD fixture
rows use `is_mock=0` together with `scorer_mode=local_qa_fixture`,
`artifact_name=LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL`, natural business display
fields, and `clinicalUseAllowed=false`. Only the three explicit improvement-cohort APP
accounts, including the shared `local_app_shared_01` account used for 何俊杰, use a
non-Mock attribution row. This exception exists solely for local
UI/permission acceptance and must never be copied to staging or production.

### Local medical workspace test data

To populate the medical workspace with two isolated institutions, four staff
logins, 24 fictional App patients, Mock risk/intervention chains, RHI/RDI
snapshots, and 30-day device histories, run with the local MySQL and
TimescaleDB containers already started:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-medical-workspace-test-data.ps1 `
  -AnchorDate 2026-08-18
```

The seed is repeatable and verifies exact software and hardware row counts.
All rows use the narrow `LOCAL_MEDICAL_TEST_SEED` marker, all model outputs are
explicitly Mock/non-clinical, and the command refuses reserved-ID or account
collisions. Remove only this cohort with the same command plus `-Cleanup`.
Accounts, data topology, known patient-list behavior, safety rules, and exact
counts are documented in [MEDICAL_TEST_DATA.md](MEDICAL_TEST_DATA.md).

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

For local RHI calculation QA, seed the active `admin` account with the same
50-year-old male synthetic profile and improving 118-day history used by the
Android Debug full-chain rehearsal:

```powershell
powershell -ExecutionPolicy Bypass -File `
  backend/deploy/rehealth/scripts/seed-admin-rhi-test-data.ps1
```

The RHI seed upserts the MySQL profile, complete confirmed RHI manual inputs,
and a synthetic device binding, then replaces one stable TimescaleDB batch with
1,180 measurements, 118 sleep sessions, and 118 activities. Pass
`-AnchorDate yyyy-MM-dd` to reproduce a specific scoring window. Re-running the
script is idempotent for its fixed batch and binding IDs. All rows are marked
`LOCAL_TEST_SEED` and are for QA only. Because the profile and manual-input rows
are shared per user, run the desired admin scenario seed immediately before its
test. This seed persists RHI inputs but does not fabricate a cloud RHI daily
snapshot. Snapshots are accepted only through the authenticated
`POST /rehealth/mobile/rhi/daily-snapshot` path after the App has persisted and
calculated a real local result.

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

The launcher now verifies each expected loopback port before starting its managed process. If an
older untracked JeecgBoot or Python process still owns `8080`, `8000`, `8010`, or the selected Device
Service port, startup fails with the owning PID instead of writing a misleading fresh PID file while
the new process exits on a bind conflict.

If Docker/WSL reserves the default `8091` host port on Windows, choose an
available loopback port without changing tracked files:

```powershell
$env:REHEALTH_DEVICE_SERVICE_PORT = '8381'
powershell -ExecutionPolicy Bypass -File backend/deploy/rehealth/start-local-apps.ps1
```

The launcher applies the same port to the Device Service listener and the
JeecgBoot internal client base URL.

The local launcher runs JeecgBoot with the `development` profile, so it does not
load Viomi defaults from `application-prod.yml`. For local Viomi binding, place
`REHEALTH_VIOMI_APP_ID`, `REHEALTH_VIOMI_APP_KEY`, and optionally
`REHEALTH_VIOMI_USER_ID` in the ignored `backend/deploy/rehealth/.env` file.
`start-local-apps.ps1` loads those values into the JeecgBoot process while
preserving already supplied process environment variables as fallbacks. Never
commit the local `.env` file or print the AppKey in logs.

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
tables exist. The ReHealth launcher and Compose deployment set Quartz's
`tablePrefix` to lowercase `qrtz_`, matching the lowercase `qrtz_*` tables in
the MySQL schema when `lower_case_table_names=0`.

`start-local-apps.ps1` 默认使用 `JEECG_SMS_DEV_MODE=true`。此模式下
`POST /jeecg-boot/sys/registerSms` 不要求在 APK 中保存 Jeecg 共享签名，但仍执行服务端手机号/IP 频控；开发模式不会调用短信网关，服务端创建 5 分钟开发
会话，`POST /jeecg-boot/sys/user/register` 只接受固定测试验证码 `123456`。本地 `.env` 可显式设置
`JEECG_SMS_DEV_MODE=false` 与 `JEECG_SMS_DYPNS_ENABLED=true` 来测试真实号码认证短信；启动器会把忽略跟踪的
`secrets/aliyun_sms_access_key_id` 和 `secrets/aliyun_sms_access_key_secret` 作为文件配置传给 JeecgBoot。
为兼容既有本地 `.env`，新的 DYPNS 开关或签名未填写时，启动器可回退读取旧的
`JEECG_SMS_ALIYUN_ENABLED` 和 `JEECG_SMS_ALIYUN_SIGN_NAME`；staging/production 不使用该本地兼容逻辑。

staging/production 必须设置 `JEECG_SMS_DEV_MODE=false`、`JEECG_SMS_DYPNS_ENABLED=true`，
填写号码认证服务控制台显示的赠送签名，并使用已确认的赠送登录/注册模板：

```text
TemplateCode: 100001
Template: 您的验证码为${code}。尊敬的客户，以上验证码${min}分钟内有效，请注意保密，切勿告知他人。
TemplateParam: {"code":"##code##","min":"5"}
SchemeName: rehealth-register
CodeLength: 6
ValidTime: 300 seconds
Interval: 60 seconds
DuplicatePolicy: 1 (overwrite)
ReturnVerifyCode: false
```

JeecgBoot 通过 `SendSmsVerifyCode` 让号码认证服务生成并发送验证码；注册时使用相同
`SchemeName` 和发送阶段的 `OutId` 调用 `CheckSmsVerifyCode`，只有 `Code=OK`、`Success=true`
且 `Model.VerifyResult=PASS` 才创建账号。生产验证码不写入 Redis；Redis 只保存哈希键的发送
会话、冷却、手机号/IP 配额和注册锁。

把专用 RAM 用户的 AccessKey ID/Secret 分别写入忽略跟踪的
`secrets/aliyun_sms_access_key_id` 与 `secrets/aliyun_sms_access_key_secret`。RAM 最小权限为
`dypns:SendSmsVerifyCode` 和 `dypns:CheckSmsVerifyCode`。凭据不得复用 OSS AccessKey，也不得
进入 `.env`、Android APK、日志或受版本控制的 YAML；任一生产必填项缺失时发送/校验失败
关闭，不会回退到测试码。号码认证服务的赠送签名/模板不能与标准短信服务 `Dysmsapi` 混用；
旧 Jeecg 短信登录/找回密码配置仍由 `JEECG_SMS_ALIYUN_*` 独立控制，默认关闭。密钥文件轮换后
需要重启 JeecgBoot 以重建 SDK 客户端。

该本地启动脚本还会启用
`REHEALTH_QA_SYNTHETIC_ATTRIBUTION_HISTORY_ENABLED=true`，仅供 Debug APK 的
全链路演练把已完成真实 RDI-16 远程评估的 30 日合成历史转发给 PIAS。后端默认值仍为
`false`，staging/production 不得启用。

The scripts run applications as hidden Windows processes. PID files and
separate stdout/stderr logs are written to the ignored
`backend/deploy/rehealth/.local-runtime/` directory. The model service loads
the reviewed local model, PIAS uses its production entrypoint, and the external
health-agent provider follows the ignored local `.env` configuration.
