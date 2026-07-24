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
D:\rehealthAI\model-service\.venv\Scripts\python.exe backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production --report topology.json
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

D:\rehealthAI\model-service\.venv\Scripts\python.exe `
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
D:\rehealthAI\model-service\.venv\Scripts\python.exe `
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

After the current JARs and Python virtual environment have been built, start
or stop all application processes with:

```powershell
powershell -ExecutionPolicy Bypass -File backend/deploy/rehealth/start-local-apps.ps1
powershell -ExecutionPolicy Bypass -File backend/deploy/rehealth/stop-local-apps.ps1
```

The scripts run applications as hidden Windows processes. PID files and
separate stdout/stderr logs are written to the ignored
`backend/deploy/rehealth/.local-runtime/` directory. The model service loads
the reviewed local model, PIAS uses its production entrypoint, and the external
health-agent provider remains disabled unless explicitly configured.
