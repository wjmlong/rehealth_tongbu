# ReHealth AI — project memory

## Build env (Android, WSL2)
- WSL `Ubuntu-24.04`, JDK17 `/usr/lib/jvm/java-17-openjdk-amd64`, Gradle 8.9. Linux SDK at `/home/wjmlong/Android/Sdk` (Windows SDK `/mnt/d/Android_SDK` CANNOT be used from WSL).
- Recipe: clone Android-apk to `~/rehealthAI-android`, set `local.properties sdk.dir=/home/wjmlong/Android/Sdk`, `chmod +x gradlew`, kill stale GradleDaemon, `./gradlew assembleDebug --no-daemon`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- GOTCHA: never pass build cmds via `wsl -e bash -lc "..."` with inline `$PATH` (Git Bash expands to Windows PATH → breaks). Write a script, run `wsl bash /mnt/d/.../script.sh`.

## Backend deploy topology
- Docker Desktop Win11 = one shared daemon; `docker` from Windows or WSL hits same engine. Compose orchestration: `backend/deploy/rehealth` (Windows path OK). Project `rehealth`.
- Services: edge(nginx:8080)→gateway(:9999)→jeecg-system(:7001)/device-service(:8091)/model-service(:8000)/pias(:8010); infra software-db/hardware-db/redis/nacos/kafka; observability prometheus/grafana. Only `edge` host-exposed (127.0.0.1:8080).
- JAR pre-built (Dockerfile copies it); to include Java source changes: `mvn clean package -DskipTests` in jeecg-boot, then `docker compose up -d --build <svc>`.
- Model-service: Windows reserves 7961-8060 → set `MODEL_PORT=8090` (internal container port stays 8000).

## rehealth stack — critical gotchas
- **Frontend prefix mapping (KEY, fixed 2026-07-24):** JeecgBoot frontend default `VITE_GLOB_API_URL=/jeecgboot` (NO dash) in `.env.{development,docker,production}`. Backend context-path = `/jeecg-boot`. So edge MUST have `location /jeecgboot/ { proxy_pass http://gateway:9999/jeecg-boot/; }`. Without it, `/jeecgboot/...` falls to `location /` → admin-web STATIC nginx → GET 404 / POST 405 (the "登录405/验证码404" bug). Frontend `.env` is correct as-is; do NOT change it — the mapping belongs at edge/gateway (like upstream dev proxy).
- **gateway NPE on boot:** `DynamicRouteLoader.refresh()` line 99 `endsWith(getDataType())` NPE when `jeecg.route.config.data-type` unset. Fix: `JEECG_ROUTE_CONFIG_DATA_TYPE=nacos` + `JEECG_ROUTE_CONFIG_DATA_ID=gateway-router`; seed `gateway-router.json` into Nacos (namespace `springboot3`, group `DEFAULT_GROUP`).
- **Nacos no persistent volume** → config lost on recreate. `nacos-seed` one-shot init container re-pushes jeecg.yaml/jeecg-dev.yaml/gateway-router.json idempotently each `up`; jeecg-boot/gateway/device-service `depends_on` it. Manual fallback `scripts/seed-nacos.sh`.
- **gateway routes:** `rehealth-device-telemetry`→device-service; `rehealth-business` `/jeecg-boot/rehealth/**`→jeecg-system; catch-all `/jeecg-boot/**`→jeecg-system.
- **edge `proxy_pass` for `/jeecg-boot/` = `http://gateway:9999` (NO trailing slash)** — trailing slash strips prefix → gateway 404.
- **device-service startup:** `JdbcOutboxStore` needs `JdbcTemplate`; hardware-db disabled → no DS → crash. Local fix `REHEALTH_KAFKA_PUBLISHER_ENABLED=false`. gateway must NOT `depends_on` device-service.
- **edge port 8080 conflict:** stray local jeecg-boot (from `start-local-apps.ps1`) can hold it — kill by live PID; do NOT run `stop-local-apps.ps1` (stale pid files → PID reuse risk). Tailscale also listens on 8080 but on a different interface (no conflict).

## Android (build + MuMu emulator)
- **Build (Windows, preferred over WSL):** use Android Studio's bundled JBR as JAVA_HOME:
  `export JAVA_HOME="D:/Android_Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"`, then
  `cd D:\rehealthAI\Android-apk && ./gradlew assembleDebug --no-daemon`. SDK from
  `local.properties sdk.dir=D:\Android_SDK` (Windows SDK works fine on Windows host; only
  WSL needs the separate Linux SDK). Output: `app/build/outputs/apk/debug/app-debug.apk`.
- **MuMu emulator (replaces crashed Pixel_10):** installed at `E:\Program Files\Netease\MuMu`.
  Bundled adb: `E:\Program Files\Netease\MuMu\nx_device\15.0\shell\adb.exe` (v36.0.0). Default
  instance adb port **127.0.0.1:16384** (MuMu 12; legacy 7555 also listens). Android 15 (API
  35), x86_64. Device IP 10.0.2.15/24 → **10.0.2.2 is the host loopback**, exactly like the
  official emulator, so the app's default `reHealthApiBaseUrl()=http://10.0.2.2:8080/jeecg-boot/`
  reaches edge→backend with NO change. Verified: app logs `HTTP 401 token为空` from backend on
  launch (chain works; 401 is just not-logged-in).
- **adb connect:** `adb connect 127.0.0.1:16384`. User PATH already includes
  `D:\Android_SDK\platform-tools` and the MuMu adb dir. Android Studio sees it after restart.
- **Start MuMu headlessly (when VM not running):** the Android VM is NOT auto-started by the
  service. Use the CLI: `E:\Program Files\Netease\MuMu\nx_main\mumu-cli.exe control --vmindex 0
  launch` (errcode 0 = issued). After ~30s the instance shows `is_android_started:true` and adb
  ports 16384/7555 come up. `mumu-cli.exe info --vmindex all` lists instances. Do NOT invoke
  mumu-cli / PowerShell from inside a Bash `powershell -Command` (blocked); run them as separate
  tool calls.
- **GOTCHA:** Windows `adb.exe` does NOT understand Git-Bash `/d/...` paths — pass
  `D:/rehealthAI/...` or `D:\rehealthAI\...` (both OK on Windows adb).
- App package `com.rehealth.genie`, launcher `com.rehealth.genie.MainActivity`.
- Physical device: override `rehealth.api.base.url` in `local.properties` to
  `http://<HOST_LAN_IP>:8080/jeecg-boot/` + set `REHEALTH_EDGE_BIND=0.0.0.0` in `.env`.

## Conventions
- Park colliding Kotlin files as `*.kt.disabled`. Android git repo = `Android-apk/`; branch `work/<feature>`.
- AGENTS.md rules: smallest safe change; real over mock; BLE independent of upload; persist before upload; no PII logging; conservative medical advice.
