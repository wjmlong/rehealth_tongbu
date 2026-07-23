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

Device Service readiness requires TimescaleDB and Jeecg identity resolution.
Kafka is intentionally not a readiness dependency: an outage degrades the
publisher and leaves committed Outbox rows pending while ingestion stays ready.
