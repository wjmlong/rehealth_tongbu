# PIAS Model-Service — Docker (dev / prod isolation)

The real PIAS causal-attribution engine lives in `rehealth-algorithms/api`
(FastAPI) and imports `healthagent.pias`. To keep the WSL2 host clean and to
separate **dev** from **prod** (the prod container is the intended main runtime),
we run it as a Docker container.

## Prerequisites
Use the **Windows Docker Desktop** (its WSL2 backend is the runtime here) — NOT
a docker.io installed inside WSL2.
1. Install Docker Desktop on Windows and start it (systray → "Docker Desktop").
2. Confirm the daemon is up from a Windows terminal:
   ```powershell
   docker info   # ServerVersion should print; OSType=linux (WSL2 backend)
   ```
3. The repo lives on Windows `D:\rehealthAI` and is auto-mounted into WSL2 as
   `/mnt/d/rehealthAI`; Docker Desktop can build from the Windows path directly.


## Build the image
```bash
cd rehealth-algorithms/docker
docker compose -f docker-compose.yml -f docker-compose.dev.yml build
```

## Run — DEV (live reload, source mounted)
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
# edits to ../api or ../healthagent hot-reload automatically
```

## Run — PROD (immutable, always-on, healthchecked)
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
docker compose ps          # should show "healthy"
docker compose logs -f     # tail logs
```

## Verify
```bash
curl -s http://localhost:8000/health
curl -s -X POST http://localhost:8000/api/pias/v2/attribute/individual \
  -H "Content-Type: application/json" \
  -d '{"risk_history":[{"date":"2026-05-01","Y":0.52,"Z":1}],"forecast_days":30,"language":"zh"}'
```

## Android client
Point `Android-apk/local.properties`:
```
rehealth.model.service.base.url=http://10.0.2.2:8000/api/pias/v2   # emulator
rehealth.model.service.base.url=http://<WSL2_IP>:8000/api/pias/v2 # real device
```
and run `tools/wsl2-android-connect.ps1` (admin PowerShell) for port forwarding.
