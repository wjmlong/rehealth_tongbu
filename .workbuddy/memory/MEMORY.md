# ReHealth AI — project memory

## Build environment (critical)
- **Building from WSL2 now WORKS** (user lifted the "System Tools" sandbox policy that previously blocked `wsl`). A real `assembleDebug` was run successfully 2026-07-20 → `BUILD SUCCESSFUL`, APK at WSL `~/rehealthAI-android/app/build/outputs/apk/debug/app-debug.apk`.
- WSL distro `Ubuntu-24.04`; JDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64` (set in `~/.bashrc`); Gradle 8.9 (downloads on first run, cache at `~/.gradle`).
- **The Windows SDK at `/mnt/d/Android_SDK` CANNOT be used from WSL** — it ships Windows `.exe` build-tools, so AGP errors "Build-tool ... corrupted / missing AAPT". A **Linux SDK must be installed in WSL**.
- **Working recipe (verified 2026-07-20):** install Linux SDK via sdkmanager into `/home/wjmlong/Android/Sdk` (`platform-tools`, `platforms;android-36`, `build-tools;34.0.0`, `build-tools;36.0.0`); then `git clone /mnt/d/rehealthAI/Android-apk ~/rehealthAI-android`, write `local.properties` with `sdk.dir=/home/wjmlong/Android/Sdk`, `chmod +x gradlew`, `pkill -f GradleDaemon` (kill stale daemons — a leftover daemon from an earlier `/mnt/d` build can cache the wrong SDK path), then `./gradlew assembleDebug --no-daemon`.
- **CRITICAL gotcha:** do NOT pass build commands via `wsl -e bash -lc "..."` with inline `$PATH`/`$JAVA_HOME` — the OUTER Git Bash expands `$PATH` to the Windows PATH (contains `Program Files (x86)` → `(` breaks bash parsing). **Write the command to a script file and run `wsl bash /mnt/d/.../script.sh`** instead, so `$PATH` only expands inside WSL (clean Linux PATH).
- The user's Windows-side `local.properties` keeps `sdk.dir=D:\\Android_SDK` (Windows build uses the Windows SDK). WSL builds override via the cloned copy's `local.properties`.

## Backend deploy topology (verified 2026-07-21)
- **Docker Desktop on Win11 = ONE shared daemon** (WSL2 backend). `docker` from Windows shells (PowerShell/Git Bash/CMD) AND from any WSL distro hit the SAME engine. No Docker-level need to be in WSL.
- **BUT the compose orchestration lives ONLY in WSL** at `~/rehealthAI/backend/deploy/staging/` (compose.yml, `.env` w/ secrets, `Dockerfile.rehealth-staging-jar`, `mysql/`, backup scripts). Windows `D:\rehealthAI\backend\` has the `jeecg-boot/` source but **NO `deploy/` dir at all**. `docker compose` must run where compose.yml is → run from WSL.
- compose `backend.build.context: ../../jeecg-boot` → `~/rehealthAI/backend/jeecg-boot/` (exists in WSL). The `@JSONField` snake_case fix IS present in the WSL source (6 aliases) as well as Windows (7). Rebuild from WSL picks up the fix.
- Backend publishes `0.0.0.0:8080` on the Windows host; emulator reaches via `10.0.2.2:8080`. Reachability is identical regardless of which side runs compose.
- **Probe `outputs/probe_mobile_fixed.py` is stdlib-only (urllib), targets `http://127.0.0.1:8080` → must run from WINDOWS**, not WSL. WSL2 default NAT networking cannot reach the Windows-host-published port via `127.0.0.1` (WSL's 127.0.0.1 is its own loopback). Correct split: **compose rebuild from WSL, probe from Windows.**
- To run compose fully from Windows instead: copy staging dir over first (`wsl bash -lc 'cp -r ~/rehealthAI/backend/deploy/staging /mnt/d/rehealthAI/backend/deploy/'`), then `cd D:\rehealthAI\backend\deploy\staging && docker compose up -d --build backend`. build.context then resolves to Windows jeecg-boot (also has the fix). Extra step, no benefit.
- **JAR rebuild recipe (verified 2026-07-21):** the Dockerfile copies a PRE-BUILT JAR (not building from source). To include source changes → first `cd ~/rehealthAI/backend/jeecg-boot && mvn clean package -DskipTests` (WSL needs JDK17 + Maven; `sudo apt install maven`); then `cd ~/rehealthAI/backend/deploy/staging && docker compose up -d --build backend`. Without the Maven step, Docker build cache will reuse the old JAR.
- **Model-service port fix:** Windows reserves ports 7961-8060 (via `netsh int ipv4 show excludedportrange`). The default 8000 falls in this range → change `MODEL_PORT=8090` and `MODEL_BIND_ADDRESS=0.0.0.0` in `.env` to avoid port reservation. Backend→model-service internal Docker network uses container port 8000 regardless (unaffected by host mapping).
- **Verified result (2026-07-21):** `isMock: false`, `modelVersion: cvd-core16-catboost-20260710T173543Z`, 0 fields dropped, 16/16 snake_case keys parsed by @JSONField fix.

## Conventions
- To exclude a Kotlin file from compilation without deleting it, rename to `*.kt.disabled` (reversible, no git history lost). Used to park orphaned/parallel refactors that collide at compile time (e.g. duplicate `ApiResult` declarations in the same package).
- Git repo for Android work lives at `Android-apk/` (not repo root `D:/rehealthAI`). Branch naming: `work/<feature>`.

## Current feature branch
- `work/D3_android_auth_typed_feedback` — D3 auth-aware upload + typed intervention feedback. Infrastructure (auth client, queue repo, feedback repo, sync worker, banner) was committed in `67f77df`/`1e8dbac`/`f40f630`; UI integration (login/logout/feedback/banner/worker) done 2026-07-20.

## Project rules (from AGENTS.md)
- Make smallest safe change; prefer real impl over mock; keep BLE independent of network upload; persist locally before upload; never log raw health data/PII; medical advice must be conservative (no diagnosis claims).
