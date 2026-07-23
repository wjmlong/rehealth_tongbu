# ReHealth platform contracts

This directory is the versioned compatibility boundary for ReHealth services. `openapi/` freezes the Android-facing Jeecg API, `schemas/events/` freezes privacy-safe Kafka envelopes, `adrs/` records ownership and safety decisions, and `fixtures/` supplies executable examples.

Run from the repository root with the checked project interpreter. On this Windows host, bare `python` resolves to the WindowsApps stub and exits 9009.

```powershell
$python = (Resolve-Path .\model-service\.venv\Scripts\python.exe).Path
& $python backend/contracts/scripts/validate_contracts.py --all --fixtures backend/contracts/fixtures/valid --report contracts.json
& $python backend/contracts/scripts/validate_contracts.py --fixtures backend/contracts/fixtures/forbidden --expect-rejected token,raw_signal,client_owner
```

Public mobile routes are compatibility contracts. A service cutover may change the internal Gateway target only after migration reconciliation; it must not change the Android path or response fields.
